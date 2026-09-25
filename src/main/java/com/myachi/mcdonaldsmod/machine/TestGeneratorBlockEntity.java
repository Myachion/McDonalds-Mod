package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModBlockEntities;
import com.myachi.mcdonaldsmod.ModScreenHandlers;
import com.myachi.mcdonaldsmod.energy.EnergyNetworks;
import com.myachi.mcdonaldsmod.energy.EnergyStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * 测试发电机的数据。
 *
 * <p>四个可调参数（发电电压/电流、输出电压/电流）加一个 100 kJ 的缓存池。
 * 发电机按"发电功率"往缓存里充电，对外输出时从缓存取电（见 {@link #tick()} 和
 * {@link #extractEnergy(long)}），这符合"缓冲区模式"：发出来的电先存进缓冲区，输出再从缓冲区扣。
 *
 * <p>单位：能量用毫焦（mJ），电流对外暴露用毫安（mA），功率由 V × mA = mW 直接得出。
 * 界面数据通过 {@link PropertyDelegate} 同步，能量是 long，拆成三个 15 位字段发过去。
 */
public class TestGeneratorBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, EnergyStorage {
    /** 缓存容量：100 kJ，换算成毫焦。 */
    public static final long CAPACITY = 100_000_000L;

    public static final int INDEX_GENERATION_VOLTAGE = 0;
    public static final int INDEX_GENERATION_CURRENT = 1;
    public static final int INDEX_OUTPUT_VOLTAGE = 2;
    public static final int INDEX_OUTPUT_CURRENT = 3;
    public static final int INDEX_ENERGY_LOW = 4;
    public static final int INDEX_ENERGY_MID = 5;
    public static final int INDEX_ENERGY_HIGH = 6;
    /** 下面两个是电网回填的"实际输出"，和上面那四个"设定值"区分开。 */
    public static final int INDEX_MEASURED_OUTPUT_VOLTAGE = 7;
    /** 实测电流可能超过 32767 mA（16 位同步上限），所以一样拆三个字段。 */
    public static final int INDEX_MEASURED_CURRENT_LOW = 8;
    public static final int INDEX_MEASURED_CURRENT_MID = 9;
    public static final int INDEX_MEASURED_CURRENT_HIGH = 10;
    public static final int PROPERTY_COUNT = 11;

    private int generationVoltage = 128;
    private int generationCurrent = 0;
    private int outputVoltage = 128;
    private int outputCurrent = 0;
    /** 缓冲区能量，单位毫焦。 */
    private long storedEnergy = 0;
    /** 实际输出（由电网结算回填）：电压 V，电流 mA。 */
    private int measuredOutputVoltage = 0;
    private int measuredOutputCurrentMilliAmps = 0;

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case INDEX_GENERATION_VOLTAGE -> TestGeneratorBlockEntity.this.generationVoltage;
                case INDEX_GENERATION_CURRENT -> TestGeneratorBlockEntity.this.generationCurrent;
                case INDEX_OUTPUT_VOLTAGE -> TestGeneratorBlockEntity.this.outputVoltage;
                case INDEX_OUTPUT_CURRENT -> TestGeneratorBlockEntity.this.outputCurrent;
                case INDEX_ENERGY_LOW -> LongPropertyCodec.field(TestGeneratorBlockEntity.this.storedEnergy, 0);
                case INDEX_ENERGY_MID -> LongPropertyCodec.field(TestGeneratorBlockEntity.this.storedEnergy, 1);
                case INDEX_ENERGY_HIGH -> LongPropertyCodec.field(TestGeneratorBlockEntity.this.storedEnergy, 2);
                case INDEX_MEASURED_OUTPUT_VOLTAGE -> TestGeneratorBlockEntity.this.measuredOutputVoltage;
                case INDEX_MEASURED_CURRENT_LOW ->
                        LongPropertyCodec.field(TestGeneratorBlockEntity.this.measuredOutputCurrentMilliAmps, 0);
                case INDEX_MEASURED_CURRENT_MID ->
                        LongPropertyCodec.field(TestGeneratorBlockEntity.this.measuredOutputCurrentMilliAmps, 1);
                case INDEX_MEASURED_CURRENT_HIGH ->
                        LongPropertyCodec.field(TestGeneratorBlockEntity.this.measuredOutputCurrentMilliAmps, 2);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case INDEX_GENERATION_VOLTAGE -> TestGeneratorBlockEntity.this.generationVoltage = value;
                case INDEX_GENERATION_CURRENT -> TestGeneratorBlockEntity.this.generationCurrent = value;
                case INDEX_OUTPUT_VOLTAGE -> TestGeneratorBlockEntity.this.outputVoltage = value;
                case INDEX_OUTPUT_CURRENT -> TestGeneratorBlockEntity.this.outputCurrent = value;
                default -> {
                    // 能量那几段只由服务端写，客户端只会往数组里存
                }
            }
        }

        @Override
        public int size() {
            return PROPERTY_COUNT;
        }
    };

    public TestGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEST_GENERATOR, pos, state);
    }

    public PropertyDelegate getProperties() {
        return this.properties;
    }

    public long getOutputPower() {
        return (long) this.outputVoltage * this.outputCurrent;
    }

    public long getGenerationPower() {
        return (long) this.generationVoltage * this.generationCurrent;
    }

    /**
     * 服务端每 tick 一次：先把缓存充满（按发电功率），再把自己登记到电网上。
     * 对外输出发生在 {@link #extractEnergy(long)}，由电网结算调用。
     */
    public void tick() {
        if (this.world == null || this.world.isClient()) {
            return;
        }
        if (this.world instanceof ServerWorld serverWorld) {
            EnergyNetworks.get(serverWorld).registerMachine(this.pos);
        }
        long power = this.getGenerationPower();
        if (power <= 0 || this.storedEnergy >= CAPACITY) {
            return;
        }
        // 1 W 持续一个 tick 相当于 50 mJ（1/20 秒）
        this.storedEnergy = Math.min(CAPACITY, this.storedEnergy + Math.max(1L, power * 50L));
        this.markDirty();
    }

    /** 把电压在等级表里往前/往后挪一格（超出两端就绕回去）。 */
    public void cycleVoltage(boolean output, int delta) {
        int current = output ? this.outputVoltage : this.generationVoltage;
        int next = EnergyLevels.cycle(EnergyLevels.VOLTAGE, current, delta);
        if (output) {
            this.outputVoltage = next;
        } else {
            this.generationVoltage = next;
        }
        this.markDirty();
    }

    /**
     * 电流在 {@link EnergyLevels#CURRENT} 档位表里上/下挪一格，和电压一样是循环的：
     * 50 A 再往上回到 0 A，0 A 再往下绕到 50 A。
     */
    public void cycleCurrent(boolean output, int delta) {
        int current = output ? this.outputCurrent : this.generationCurrent;
        int next = EnergyLevels.cycle(EnergyLevels.CURRENT, current, delta);
        if (output) {
            this.outputCurrent = next;
        } else {
            this.generationCurrent = next;
        }
        this.markDirty();
    }

    // ------------------------------------------------------------------
    // 电网接口
    // ------------------------------------------------------------------

    @Override
    public long getStoredEnergyMilliJoules() {
        return this.storedEnergy;
    }

    /** 发电机不接受外部充电，缓存只由自己的发电填。 */
    @Override
    public long insertEnergy(long millijoules) {
        return 0;
    }

    @Override
    public long extractEnergy(long millijoules) {
        long taken = Math.min(millijoules, this.storedEnergy);
        if (taken > 0) {
            this.storedEnergy -= taken;
            this.markDirty();
        }
        return taken;
    }

    @Override
    public int getRatedInputVoltage() {
        return 0;
    }

    @Override
    public int getRatedInputCurrent() {
        return 0;
    }

    @Override
    public int getRatedOutputVoltage() {
        return this.outputVoltage;
    }

    @Override
    public int getRatedOutputCurrent() {
        return this.outputCurrent * 1000;
    }

    @Override
    public void setMeasuredOutput(int voltage, int currentMilliAmps) {
        this.measuredOutputVoltage = voltage;
        this.measuredOutputCurrentMilliAmps = currentMilliAmps;
    }

    /** 发电机一律不能输入电流：六个面都只出不进。 */
    @Override
    public boolean canReceiveEnergyOn(net.minecraft.util.math.Direction side) {
        return false;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.generationVoltage = EnergyLevels.snap(EnergyLevels.VOLTAGE, view.getInt("generation_voltage", 128));
        this.generationCurrent = EnergyLevels.snap(EnergyLevels.CURRENT, view.getInt("generation_current", 0));
        this.outputVoltage = EnergyLevels.snap(EnergyLevels.VOLTAGE, view.getInt("output_voltage", 128));
        this.outputCurrent = EnergyLevels.snap(EnergyLevels.CURRENT, view.getInt("output_current", 0));
        this.storedEnergy = Math.clamp(view.getLong("stored_energy", 0L), 0L, CAPACITY);
    }

    @Override
    protected void writeData(WriteView view) {
        view.putInt("generation_voltage", this.generationVoltage);
        view.putInt("generation_current", this.generationCurrent);
        view.putInt("output_voltage", this.outputVoltage);
        view.putInt("output_current", this.outputCurrent);
        view.putLong("stored_energy", this.storedEnergy);
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new TestGeneratorScreenHandler(syncId, playerInventory, this.pos, this.properties);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(ModScreenHandlers.TEST_GENERATOR_TITLE_KEY);
    }
}
