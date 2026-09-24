package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModBlockEntities;
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
 * 测试电池盒的数据：储电量 + 输入/输出 的电压/电流。
 *
 * <p>储电量**没有上限**（就是 long 能装多少，约 9.2e18 焦），所以同步时把它拆成
 * 三个 15 位字段（见 {@link LongPropertyCodec}）。实际读数暂时不参与能量传输。
 */
public class TestBatteryBoxBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    public static final int INDEX_ENERGY_LOW = 0;
    public static final int INDEX_ENERGY_MID = 1;
    public static final int INDEX_ENERGY_HIGH = 2;
    /** 下面四个是"当前实际"的读数，等电网接进来才会动。 */
    public static final int INDEX_INPUT_VOLTAGE = 3;
    public static final int INDEX_INPUT_CURRENT = 4;
    public static final int INDEX_OUTPUT_VOLTAGE = 5;
    public static final int INDEX_OUTPUT_CURRENT = 6;
    /** 下面四个是界面上可以调的"额定"数值。 */
    public static final int INDEX_RATED_INPUT_VOLTAGE = 7;
    public static final int INDEX_RATED_INPUT_CURRENT = 8;
    public static final int INDEX_RATED_OUTPUT_VOLTAGE = 9;
    public static final int INDEX_RATED_OUTPUT_CURRENT = 10;
    public static final int PROPERTY_COUNT = 11;

    /** 储电量（焦耳），没有上限。 */
    private long storedEnergy = 0;
    // 实际读数（暂时恒为 0）
    private int inputVoltage = 0;
    private int inputCurrent = 0;
    private int outputVoltage = 0;
    private int outputCurrent = 0;
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
                case INDEX_INPUT_CURRENT -> TestBatteryBoxBlockEntity.this.inputCurrent;
                case INDEX_OUTPUT_VOLTAGE -> TestBatteryBoxBlockEntity.this.outputVoltage;
                case INDEX_OUTPUT_CURRENT -> TestBatteryBoxBlockEntity.this.outputCurrent;
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
                case INDEX_ENERGY_LOW, INDEX_ENERGY_MID, INDEX_ENERGY_HIGH -> {
                    // 客户端只会往数组里写，这里不用管
                }
                case INDEX_INPUT_VOLTAGE -> TestBatteryBoxBlockEntity.this.inputVoltage = value;
                case INDEX_INPUT_CURRENT -> TestBatteryBoxBlockEntity.this.inputCurrent = value;
                case INDEX_OUTPUT_VOLTAGE -> TestBatteryBoxBlockEntity.this.outputVoltage = value;
                case INDEX_OUTPUT_CURRENT -> TestBatteryBoxBlockEntity.this.outputCurrent = value;
                case INDEX_RATED_INPUT_VOLTAGE -> TestBatteryBoxBlockEntity.this.ratedInputVoltage = value;
                case INDEX_RATED_INPUT_CURRENT -> TestBatteryBoxBlockEntity.this.ratedInputCurrent = value;
                case INDEX_RATED_OUTPUT_VOLTAGE -> TestBatteryBoxBlockEntity.this.ratedOutputVoltage = value;
                case INDEX_RATED_OUTPUT_CURRENT -> TestBatteryBoxBlockEntity.this.ratedOutputCurrent = value;
                default -> {
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

    /** 往储电池里加电（电网接入后会用到）。 */
    public void addEnergy(long joules) {
        if (joules <= 0) {
            return;
        }
        this.storedEnergy = joules > Long.MAX_VALUE - this.storedEnergy ? Long.MAX_VALUE : this.storedEnergy + joules;
        this.markDirty();
    }

    public long getInputPower() {
        return (long) this.inputVoltage * this.inputCurrent;
    }

    public long getOutputPower() {
        return (long) this.outputVoltage * this.outputCurrent;
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

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.storedEnergy = Math.max(0, view.getLong("stored_energy", 0));
        this.inputVoltage = view.getInt("input_voltage", 0);
        this.inputCurrent = view.getInt("input_current", 0);
        this.outputVoltage = view.getInt("output_voltage", 0);
        this.outputCurrent = view.getInt("output_current", 0);
        this.ratedInputVoltage = EnergyLevels.snap(EnergyLevels.VOLTAGE, view.getInt("rated_input_voltage", 128));
        this.ratedInputCurrent = EnergyLevels.snap(EnergyLevels.CURRENT, view.getInt("rated_input_current", 0));
        this.ratedOutputVoltage = EnergyLevels.snap(EnergyLevels.VOLTAGE, view.getInt("rated_output_voltage", 128));
        this.ratedOutputCurrent = EnergyLevels.snap(EnergyLevels.CURRENT, view.getInt("rated_output_current", 0));
    }

    @Override
    protected void writeData(WriteView view) {
        view.putLong("stored_energy", this.storedEnergy);
        view.putInt("input_voltage", this.inputVoltage);
        view.putInt("input_current", this.inputCurrent);
        view.putInt("output_voltage", this.outputVoltage);
        view.putInt("output_current", this.outputCurrent);
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
