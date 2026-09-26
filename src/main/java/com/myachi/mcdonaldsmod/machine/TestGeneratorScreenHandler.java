package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.math.BlockPos;

/**
 * 测试发电机的界面逻辑：显示数据 + 按钮调参数。
 *
 * <p>没有物品槽，定位方块/权限校验/标准数值 getter 都在 {@link AbstractMachineScreenHandler} 里，
 * 这里只管两件事：发电参数的两个额外字段怎么读、八个按钮点了做什么。
 * 按钮走原版 {@code ButtonClickC2SPacket} 通道，不需要自定义网络包。
 */
public class TestGeneratorScreenHandler extends AbstractMachineScreenHandler {
    public static final int BUTTON_GENERATION_VOLTAGE_DOWN = 0;
    public static final int BUTTON_GENERATION_VOLTAGE_UP = 1;
    public static final int BUTTON_GENERATION_CURRENT_DOWN = 2;
    public static final int BUTTON_GENERATION_CURRENT_UP = 3;
    public static final int BUTTON_OUTPUT_VOLTAGE_DOWN = 4;
    public static final int BUTTON_OUTPUT_VOLTAGE_UP = 5;
    public static final int BUTTON_OUTPUT_CURRENT_DOWN = 6;
    public static final int BUTTON_OUTPUT_CURRENT_UP = 7;

    /** 服务端构造：直接接到方块实体的属性表上。 */
    public TestGeneratorScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos, PropertyDelegate properties) {
        super(ModScreenHandlers.TEST_GENERATOR, syncId, playerInventory, pos, properties, TestGeneratorBlockEntity.class);
    }

    /** 客户端构造：数值由服务端每 tick 同步过来。 */
    public TestGeneratorScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.TEST_GENERATOR, syncId, playerInventory,
                TestGeneratorBlockEntity.PROPERTY_COUNT, TestGeneratorBlockEntity.class);
    }

    /** 发电电压（V），界面读这个。 */
    public int generationVoltage() {
        return get(TestGeneratorBlockEntity.INDEX_GENERATION_VOLTAGE);
    }

    /** 发电电流（A），界面读这个。 */
    public int generationCurrent() {
        return get(TestGeneratorBlockEntity.INDEX_GENERATION_CURRENT);
    }

    /** 设定的输出功率（W），界面读这个。 */
    public long ratedOutputPowerWatts() {
        return (long) ratedOutputVoltage() * ratedOutputCurrentAmps();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        TestGeneratorBlockEntity generator = machine(TestGeneratorBlockEntity.class);
        if (generator == null) {
            return false;
        }
        switch (id) {
            case BUTTON_GENERATION_VOLTAGE_DOWN -> generator.cycleVoltage(false, -1);
            case BUTTON_GENERATION_VOLTAGE_UP -> generator.cycleVoltage(false, 1);
            case BUTTON_GENERATION_CURRENT_DOWN -> generator.cycleCurrent(false, -1);
            case BUTTON_GENERATION_CURRENT_UP -> generator.cycleCurrent(false, 1);
            case BUTTON_OUTPUT_VOLTAGE_DOWN -> generator.cycleVoltage(true, -1);
            case BUTTON_OUTPUT_VOLTAGE_UP -> generator.cycleVoltage(true, 1);
            case BUTTON_OUTPUT_CURRENT_DOWN -> generator.cycleCurrent(true, -1);
            case BUTTON_OUTPUT_CURRENT_UP -> generator.cycleCurrent(true, 1);
            default -> {
                return false;
            }
        }
        return true;
    }
}
