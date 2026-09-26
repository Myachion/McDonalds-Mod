package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModBlockEntities;
import com.myachi.mcdonaldsmod.ModScreenHandlers;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * 测试电池盒：一个大号缓冲区，本身体内就是储能池（没有额外的"缓冲区里的缓冲区"）。
 *
 * <p>迁移到 {@link AbstractMachineBlockEntity} 之后，储能量、额定输入/输出、实测读数、
 * 存档、界面属性表全部由基类提供；这里只剩"哪些面能充/能放"和按钮调档两件事。
 *
 * <p>储电量没有上限（{@link #UNLIMITED}，long 装多少算多少）。
 *
 * <p>界面字段就是标准 15 个（{@link #PROPERTY_COUNT} = {@link MachineProperties#STANDARD_COUNT}），
 * 没有额外字段。
 */
public class TestBatteryBoxBlockEntity extends AbstractMachineBlockEntity {
    /** 属性字段个数：只有标准字段。 */
    public static final int PROPERTY_COUNT = MachineProperties.STANDARD_COUNT;
    /** 初始额定档位 128 V / 0 A（和以前一致）。 */
    private static final int DEFAULT_RATED_VOLTAGE = 128;

    private final PropertyDelegate properties = standardProperties();

    public TestBatteryBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEST_BATTERY_BOX, pos, state, UNLIMITED,
                DEFAULT_RATED_VOLTAGE, 0, DEFAULT_RATED_VOLTAGE, 0);
    }

    public PropertyDelegate getProperties() {
        return this.properties;
    }

    /** 储电量，单位毫焦（界面显示用）。 */
    public long getStoredEnergy() {
        return this.getStoredEnergyMilliJoules();
    }

    /** 额定电压在等级表里挪一格（循环）。 */
    public void cycleRatedVoltage(boolean output, int delta) {
        if (output) {
            setRatedOutput(EnergyLevels.cycle(EnergyLevels.VOLTAGE, getRatedOutputVoltage(), delta),
                    getRatedOutputCurrent());
        } else {
            setRatedInput(EnergyLevels.cycle(EnergyLevels.VOLTAGE, getRatedInputVoltage(), delta),
                    getRatedInputCurrent());
        }
    }

    /** 额定电流在档位表里挪一格（循环）。 */
    public void cycleRatedCurrent(boolean output, int delta) {
        if (output) {
            setRatedOutput(getRatedOutputVoltage(),
                    EnergyLevels.cycle(EnergyLevels.CURRENT, getRatedOutputCurrent() / 1000, delta) * 1000);
        } else {
            setRatedInput(getRatedInputVoltage(),
                    EnergyLevels.cycle(EnergyLevels.CURRENT, getRatedInputCurrent() / 1000, delta) * 1000);
        }
    }

    /**
     * 只有四个侧面能往外输电 —— 也就是材质上画了圆形端口的那四面。
     * 上下两面不是输出口。
     */
    @Override
    public boolean canProvideEnergyFrom(Direction side) {
        return side.getAxis().isHorizontal() && canProvideEnergy();
    }

    /**
     * 只有上下两面能充电。侧面虽然也能接电缆，但那是输出口，电只能往外走。
     */
    @Override
    public boolean canReceiveEnergyOn(Direction side) {
        return side.getAxis() == Direction.Axis.Y && canReceiveEnergy();
    }

    /**
     * 读档。基类读的是"额定电流（mA）"，但迁移前的测试电池盒把电流按 A 存进同一个键，
     * 所以这里做一次单位兼容：非零且小于 1000 的值按旧的 A 单位处理。
     */
    @Override
    protected void readData(ReadView view) {
        int rawInputCurrent = view.getInt("rated_input_current", 0);
        int rawOutputCurrent = view.getInt("rated_output_current", 0);
        super.readData(view);
        setRatedInput(EnergyLevels.snap(EnergyLevels.VOLTAGE, getRatedInputVoltage()),
                snapCurrentToMilliAmps(rawInputCurrent, getRatedInputCurrent()));
        setRatedOutput(EnergyLevels.snap(EnergyLevels.VOLTAGE, getRatedOutputVoltage()),
                snapCurrentToMilliAmps(rawOutputCurrent, getRatedOutputCurrent()));
    }

    /** 旧存档是 A、新存档是 mA；把两种写法都还原成档位表里的 mA 值。 */
    private static int snapCurrentToMilliAmps(int rawStoredValue, int parsedMilliAmps) {
        int amps = rawStoredValue > 0 && rawStoredValue < 1000 ? rawStoredValue : parsedMilliAmps / 1000;
        return EnergyLevels.snap(EnergyLevels.CURRENT, amps) * 1000;
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
