package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModBlockEntities;
import com.myachi.mcdonaldsmod.McDonaldsMod;
import com.myachi.mcdonaldsmod.ModScreenHandlers;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * 测试发电机的数据。
 *
 * <p>现在这一步只做界面：四个可调参数（发电电压/电流、输出电压/电流）加一个 100 kJ 的缓存池，
 * 数值不参与任何实际的能量传输——电网逻辑以后再说。
 *
 * <p>界面数据通过 {@link PropertyDelegate} 同步：服务端每 tick 把变化过的值推给客户端，
 * 和原版熔炉显示燃烧进度是同一套机制。
 */
public class TestGeneratorBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    /** 缓存容量：100 kJ。 */
    public static final int CAPACITY = 100_000;
    /**
     * 界面同步用的能量刻度（焦耳/单位）。
     *
     * <p>原版 {@link PropertyDelegate} 的同步走的是 16 位字段（熔炉的 0~200 那种量级），
     * 直接把 100000 焦塞进去会被截断成负数。所以按 100 J 一格同步，
     * 精度 0.1 kJ，对 100 kJ 的容量来说足够。
     */
    public static final int ENERGY_UNIT = 100;
    public static final int INDEX_GENERATION_VOLTAGE = 0;
    public static final int INDEX_GENERATION_CURRENT = 1;
    public static final int INDEX_OUTPUT_VOLTAGE = 2;
    public static final int INDEX_OUTPUT_CURRENT = 3;
    public static final int INDEX_ENERGY = 4;
    public static final int PROPERTY_COUNT = 5;

    private int generationVoltage = 128;
    private int generationCurrent = 0;
    private int outputVoltage = 128;
    private int outputCurrent = 0;
    private int storedEnergy = 0;

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case INDEX_GENERATION_VOLTAGE -> TestGeneratorBlockEntity.this.generationVoltage;
                case INDEX_GENERATION_CURRENT -> TestGeneratorBlockEntity.this.generationCurrent;
                case INDEX_OUTPUT_VOLTAGE -> TestGeneratorBlockEntity.this.outputVoltage;
                case INDEX_OUTPUT_CURRENT -> TestGeneratorBlockEntity.this.outputCurrent;
                case INDEX_ENERGY -> TestGeneratorBlockEntity.this.storedEnergy / ENERGY_UNIT;
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
                case INDEX_ENERGY ->
                        TestGeneratorBlockEntity.this.storedEnergy = Math.clamp(value * ENERGY_UNIT, 0, CAPACITY);
                default -> {
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

    /**
     * 服务端每 tick 一次：按"发电功率"往自己的缓存池里充电（功率 W = J/s，所以每 tick 充 1/20）。
     *
     * <p>这只是发电机自己的本地行为，用来让界面上的缓存条动起来；
     * 电缆网络之间怎么传电还没有做。
     */
    public void tick() {
        if (this.world == null || this.world.isClient()) {
            return;
        }
        long power = this.getGenerationPower();
        if (power <= 0 || this.storedEnergy >= CAPACITY) {
            return;
        }
        long added = Math.max(1L, power / 20L);
        long next = (long) this.storedEnergy + added;
        this.storedEnergy = (int) Math.clamp(next, 0L, (long) CAPACITY);
        if (this.storedEnergy < 0 || this.storedEnergy > CAPACITY) {
            McDonaldsMod.LOGGER.warn("[generator] energy out of range: power={} added={} next={} stored={}",
                    power, added, next, this.storedEnergy);
        }
        this.markDirty();
    }

    public PropertyDelegate getProperties() {
        return this.properties;
    }

    public int getStoredEnergy() {
        return this.storedEnergy;
    }

    public long getOutputPower() {
        return (long) this.outputVoltage * this.outputCurrent;
    }

    public long getGenerationPower() {
        return (long) this.generationVoltage * this.generationCurrent;
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

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.generationVoltage = EnergyLevels.snap(EnergyLevels.VOLTAGE, view.getInt("generation_voltage", 128));
        this.generationCurrent = EnergyLevels.snap(EnergyLevels.CURRENT, view.getInt("generation_current", 0));
        this.outputVoltage = EnergyLevels.snap(EnergyLevels.VOLTAGE, view.getInt("output_voltage", 128));
        this.outputCurrent = EnergyLevels.snap(EnergyLevels.CURRENT, view.getInt("output_current", 0));
        this.storedEnergy = Math.clamp(view.getInt("stored_energy", 0), 0, CAPACITY);
    }

    @Override
    protected void writeData(WriteView view) {
        view.putInt("generation_voltage", this.generationVoltage);
        view.putInt("generation_current", this.generationCurrent);
        view.putInt("output_voltage", this.outputVoltage);
        view.putInt("output_current", this.outputCurrent);
        view.putInt("stored_energy", this.storedEnergy);
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
