package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.math.BlockPos;

/**
 * 测试电池盒的界面逻辑：显示数据 + 调节额定输入/输出的电压、电流。
 * 数值 getter 全部来自 {@link AbstractMachineScreenHandler} 的标准字段，这里只管按钮。
 */
public class TestBatteryBoxScreenHandler extends AbstractMachineScreenHandler {
    public static final int BUTTON_RATED_INPUT_VOLTAGE_DOWN = 0;
    public static final int BUTTON_RATED_INPUT_VOLTAGE_UP = 1;
    public static final int BUTTON_RATED_INPUT_CURRENT_DOWN = 2;
    public static final int BUTTON_RATED_INPUT_CURRENT_UP = 3;
    public static final int BUTTON_RATED_OUTPUT_VOLTAGE_DOWN = 4;
    public static final int BUTTON_RATED_OUTPUT_VOLTAGE_UP = 5;
    public static final int BUTTON_RATED_OUTPUT_CURRENT_DOWN = 6;
    public static final int BUTTON_RATED_OUTPUT_CURRENT_UP = 7;

    /** 服务端构造：直接接到方块实体的属性表上。 */
    public TestBatteryBoxScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos, PropertyDelegate properties) {
        super(ModScreenHandlers.TEST_BATTERY_BOX, syncId, playerInventory, pos, properties, TestBatteryBoxBlockEntity.class);
    }

    /** 客户端构造：数值由服务端每 tick 同步过来。 */
    public TestBatteryBoxScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.TEST_BATTERY_BOX, syncId, playerInventory,
                TestBatteryBoxBlockEntity.PROPERTY_COUNT, TestBatteryBoxBlockEntity.class);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        TestBatteryBoxBlockEntity battery = machine(TestBatteryBoxBlockEntity.class);
        if (battery == null) {
            return false;
        }
        switch (id) {
            case BUTTON_RATED_INPUT_VOLTAGE_DOWN -> battery.cycleRatedVoltage(false, -1);
            case BUTTON_RATED_INPUT_VOLTAGE_UP -> battery.cycleRatedVoltage(false, 1);
            case BUTTON_RATED_INPUT_CURRENT_DOWN -> battery.cycleRatedCurrent(false, -1);
            case BUTTON_RATED_INPUT_CURRENT_UP -> battery.cycleRatedCurrent(false, 1);
            case BUTTON_RATED_OUTPUT_VOLTAGE_DOWN -> battery.cycleRatedVoltage(true, -1);
            case BUTTON_RATED_OUTPUT_VOLTAGE_UP -> battery.cycleRatedVoltage(true, 1);
            case BUTTON_RATED_OUTPUT_CURRENT_DOWN -> battery.cycleRatedCurrent(true, -1);
            case BUTTON_RATED_OUTPUT_CURRENT_UP -> battery.cycleRatedCurrent(true, 1);
            default -> {
                return false;
            }
        }
        return true;
    }
}
