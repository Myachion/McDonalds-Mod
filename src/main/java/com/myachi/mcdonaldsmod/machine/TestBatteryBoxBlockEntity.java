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
 * 测试电池盒：一个大号缓冲区，本身体内就是储能池（没有额外的"缓冲区里的缓冲区"）。
 *
 * <p>储电量没有上限（long 装多少算多少），额定输入/输出电压和电流决定它两个方向的功率上限。
 * 每 tick 由电网结算调用 {@link #insertEnergy(long)} / {@link #extractEnergy(long)}，
 * 同时把实际流动的电压电流回填到界面上那几个"实测"读数里。
 *
 * <p>能量单位毫焦，电流对外用毫安，界面显示时再换算。
 */
public class TestBatteryBoxBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, EnergyStorage {
    public static final int INDEX_ENERGY_LOW = 0;
    public static final int INDEX_ENERGY_MID = 1;
    public static final int INDEX_ENERGY_HIGH = 2;
    /** 下面四个是"当前实际"的读数（V / mA）。 */
    public static final int INDEX_INPUT_VOLTAGE = 3;
    /** 电流可能超过 32767 mA（16 位同步上限），所以拆三个字段。 */
    public static final int INDEX_INPUT_CURRENT_LOW = 4;
    public static final int INDEX_INPUT_CURRENT_MID = 5;
    public static final int INDEX_INPUT_CURRENT_HIGH = 6;
    public static final int INDEX_OUTPUT_VOLTAGE = 7;
    public static final int INDEX_OUTPUT_CURRENT_LOW = 8;
    public static final int INDEX_OUTPUT_CURRENT_MID = 9;
    public static final int INDEX_OUTPUT_CURRENT_HIGH = 10;
    /** 下面四个是界面上可以调的"额定"数值（V / A）。 */
    public static final int INDEX_RATED_INPUT_VOLTAGE = 11;
    public static final int INDEX_RATED_INPUT_CURRENT = 12;
    public static final int INDEX_RATED_OUTPUT_VOLTAGE = 13;
    public static final int INDEX_RATED_OUTPUT_CURRENT = 14;
    public static final int PROPERTY_COUNT = 15;

    /** 储电量，单位毫焦，没有上限。 */
    private long storedEnergy = 0;
    // 实际读数
    private int inputVoltage = 0;
    private int inputCurrentMilliAmps = 0;
    private int outputVoltage = 0;
    private int outputCurrentMilliAmps = 0;
    // 额定设定值
    private int ratedInputVoltage = 128;
    private int ratedInputCurrent = 0;
    private int ratedOutputVoltage = 128;
    private int ratedOutputCurrent = 0;

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case INDEX_ENERGY_LOW -> LongPropertyCodec.field(TestBatteryBoxBlockEntity.this.storedEnergy, 0);
                case INDEX_ENERGY_MID -> LongPropertyCodec.field(TestBatteryBoxBlockEntity.this.storedEnergy, 1);
                case INDEX_ENERGY_HIGH -> LongPropertyCodec.field(TestBatteryBoxBlockEntity.this.storedEnergy, 2);
                case INDEX_INPUT_VOLTAGE -> TestBatteryBoxBlockEntity.this.inputVoltage;
                case INDEX_INPUT_CURRENT_LOW -> LongPropertyCodec.field(TestBatteryBoxBlockEntity.this.inputCurrentMilliAmps, 0);
                case INDEX_INPUT_CURRENT_MID -> LongPropertyCodec.field(TestBatteryBoxBlockEntity.this.inputCurrentMilliAmps, 1);
                case INDEX_INPUT_CURRENT_HIGH -> LongPropertyCodec.field(TestBatteryBoxBlockEntity.this.inputCurrentMilliAmps, 2);
                case INDEX_OUTPUT_VOLTAGE -> TestBatteryBoxBlockEntity.this.outputVoltage;
                case INDEX_OUTPUT_CURRENT_LOW -> LongPropertyCodec.field(TestBatteryBoxBlockEntity.this.outputCurrentMilliAmps, 0);
                case INDEX_OUTPUT_CURRENT_MID -> LongPropertyCodec.field(TestBatteryBoxBlockEntity.this.outputCurrentMilliAmps, 1);
                case INDEX_OUTPUT_CURRENT_HIGH -> LongPropertyCodec.field(TestBatteryBoxBlockEntity.this.outputCurrentMilliAmps, 2);
                case INDEX_RATED_INPUT_VOLTAGE -> TestBatteryBoxBlockEntity.this.ratedInputVoltage;
                case INDEX_RATED_INPUT_CURRENT -> TestBatteryBoxBlockEntity.this.ratedInputCurrent;
                case INDEX_RATED_OUTPUT_VOLTAGE -> TestBatteryBoxBlockEntity.this.ratedOutputVoltage;
                case INDEX_RATED_OUTPUT_CURRENT -> TestBatteryBoxBlockEntity.this.ratedOutputCurrent;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case INDEX_RATED_INPUT_VOLTAGE -> TestBatteryBoxBlockEntity.this.ratedInputVoltage = value;
                case INDEX_RATED_INPUT_CURRENT -> TestBatteryBoxBlockEntity.this.ratedInputCurrent = value;
                case INDEX_RATED_OUTPUT_VOLTAGE -> TestBatteryBoxBlockEntity.this.ratedOutputVoltage = value;
                case INDEX_RATED_OUTPUT_CURRENT -> TestBatteryBoxBlockEntity.this.ratedOutputCurrent = value;
                default -> {
                    // 其余的都是服务端写、客户端读
                }
            }
        }

        @Override
        public int size() {
            return PROPERTY_COUNT;
        }
    };

    public TestBatteryBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEST_BATTERY_BOX, pos, state);
    }

    public PropertyDelegate getProperties() {
        return this.properties;
    }

    public long getStoredEnergy() {
        return this.storedEnergy;
    }

    /** 服务端每 tick 登记一次，让电网知道这里有台机器。 */
    public void tick() {
        if (this.world instanceof ServerWorld serverWorld) {
            EnergyNetworks.get(serverWorld).registerMachine(this.pos);
        }
    }

    /** 往储电池加电（也供测试脚本灌电用）。 */
    public void addEnergy(long millijoules) {
        if (millijoules <= 0) {
            return;
        }
        this.storedEnergy = millijoules > Long.MAX_VALUE - this.storedEnergy
                ? Long.MAX_VALUE
                : this.storedEnergy + millijoules;
        this.markDirty();
    }

    /** 额定电压在等级表里挪一格（循环）。 */
    public void cycleRatedVoltage(boolean output, int delta) {
        int current = output ? this.ratedOutputVoltage : this.ratedInputVoltage;
        int next = EnergyLevels.cycle(EnergyLevels.VOLTAGE, current, delta);
        if (output) {
            this.ratedOutputVoltage = next;
        } else {
            this.ratedInputVoltage = next;
        }
        this.markDirty();
    }

    /** 额定电流在档位表里挪一格（循环）。 */
    public void cycleRatedCurrent(boolean output, int delta) {
        int current = output ? this.ratedOutputCurrent : this.ratedInputCurrent;
        int next = EnergyLevels.cycle(EnergyLevels.CURRENT, current, delta);
        if (output) {
            this.ratedOutputCurrent = next;
        } else {
            this.ratedInputCurrent = next;
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

    @Override
    public long insertEnergy(long millijoules) {
        if (millijoules <= 0) {
            return 0;
        }
        addEnergy(millijoules);
        return millijoules;
    }

    @Override
    public long extractEnergy(long millijoules) {
        if (millijoules <= 0) {
            return 0;
        }
        long taken = Math.min(millijoules, this.storedEnergy);
        if (taken > 0) {
            this.storedEnergy -= taken;
            this.markDirty();
        }
        return taken;
    }

    @Override
    public int getRatedInputVoltage() {
        return this.ratedInputVoltage;
    }

    @Override
    public int getRatedInputCurrent() {
        return this.ratedInputCurrent * 1000;
    }

    @Override
    public int getRatedOutputVoltage() {
        return this.ratedOutputVoltage;
    }

    @Override
    public int getRatedOutputCurrent() {
        return this.ratedOutputCurrent * 1000;
    }

    /**
     * 只有四个侧面能往外输电 —— 也就是材质上画了圆形端口的那四面。
     * 上下两面不是输出口。
     */
    @Override
    public boolean canProvideEnergyFrom(net.minecraft.util.math.Direction side) {
        return side.getAxis().isHorizontal() && canProvideEnergy();
    }

    /**
     * 只有上下两面能充电。侧面虽然也能接电缆，但那是输出口，电只能往外走。
     */
    @Override
    public boolean canReceiveEnergyOn(net.minecraft.util.math.Direction side) {
        return side.getAxis() == net.minecraft.util.math.Direction.Axis.Y && canReceiveEnergy();
    }

    @Override
    public void setMeasuredInput(int voltage, int currentMilliAmps) {
        this.inputVoltage = voltage;
        this.inputCurrentMilliAmps = currentMilliAmps;
    }

    @Override
    public void setMeasuredOutput(int voltage, int currentMilliAmps) {
        this.outputVoltage = voltage;
        this.outputCurrentMilliAmps = currentMilliAmps;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.storedEnergy = Math.max(0L, view.getLong("stored_energy", 0L));
        this.ratedInputVoltage = EnergyLevels.snap(EnergyLevels.VOLTAGE, view.getInt("rated_input_voltage", 128));
        this.ratedInputCurrent = EnergyLevels.snap(EnergyLevels.CURRENT, view.getInt("rated_input_current", 0));
        this.ratedOutputVoltage = EnergyLevels.snap(EnergyLevels.VOLTAGE, view.getInt("rated_output_voltage", 128));
        this.ratedOutputCurrent = EnergyLevels.snap(EnergyLevels.CURRENT, view.getInt("rated_output_current", 0));
    }

    @Override
    protected void writeData(WriteView view) {
        view.putLong("stored_energy", this.storedEnergy);
        view.putInt("rated_input_voltage", this.ratedInputVoltage);
        view.putInt("rated_input_current", this.ratedInputCurrent);
        view.putInt("rated_output_voltage", this.ratedOutputVoltage);
        view.putInt("rated_output_current", this.ratedOutputCurrent);
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new TestBatteryBoxScreenHandler(syncId, playerInventory, this.pos, this.properties);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(ModScreenHandlers.TEST_BATTERY_BOX_TITLE_KEY);
    }
}
