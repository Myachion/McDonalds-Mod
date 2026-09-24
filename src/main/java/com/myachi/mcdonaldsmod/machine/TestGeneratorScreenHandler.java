package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModBlocks;
import com.myachi.mcdonaldsmod.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * 测试发电机的界面逻辑。
 *
 * <p>没有任何物品槽，只负责把方块实体的数据读给界面，以及处理按钮点击。
 * 按钮走原版 {@code ButtonClickC2SPacket} 通道，不需要自定义网络包。
 */
public class TestGeneratorScreenHandler extends ScreenHandler {
    public static final int BUTTON_GENERATION_VOLTAGE_DOWN = 0;
    public static final int BUTTON_GENERATION_VOLTAGE_UP = 1;
    public static final int BUTTON_GENERATION_CURRENT_DOWN = 2;
    public static final int BUTTON_GENERATION_CURRENT_UP = 3;
    public static final int BUTTON_OUTPUT_VOLTAGE_DOWN = 4;
    public static final int BUTTON_OUTPUT_VOLTAGE_UP = 5;
    public static final int BUTTON_OUTPUT_CURRENT_DOWN = 6;
    public static final int BUTTON_OUTPUT_CURRENT_UP = 7;

    private final PropertyDelegate properties;
    private final World world;
    /** 客户端侧为 null（数据已经同步过来了，不需要再定位方块）。 */
    private final @Nullable BlockPos pos;

    /** 服务端构造：直接接到方块实体的 delegate 上。 */
    public TestGeneratorScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos, PropertyDelegate properties) {
        super(ModScreenHandlers.TEST_GENERATOR, syncId);
        checkDataCount(properties, TestGeneratorBlockEntity.PROPERTY_COUNT);
        this.properties = properties;
        this.pos = pos;
        this.world = playerInventory.player.getEntityWorld();
        this.addProperties(properties);
    }

    /** 客户端构造：空白 delegate，值由服务端每 tick 同步过来。 */
    public TestGeneratorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, BlockPos.ORIGIN, new ArrayPropertyDelegate(TestGeneratorBlockEntity.PROPERTY_COUNT));
    }

    /** 界面每帧读这个值。 */
    public int get(int index) {
        return this.properties.get(index);
    }

    /** 缓存电量，单位焦耳（同步值是按 100 J 一格的）。 */
    public int getStoredEnergy() {
        return get(TestGeneratorBlockEntity.INDEX_ENERGY) * TestGeneratorBlockEntity.ENERGY_UNIT;
    }

    public long getOutputPower() {
        return (long) get(TestGeneratorBlockEntity.INDEX_OUTPUT_VOLTAGE) * get(TestGeneratorBlockEntity.INDEX_OUTPUT_CURRENT);
    }

    public long getGenerationPower() {
        return (long) get(TestGeneratorBlockEntity.INDEX_GENERATION_VOLTAGE) * get(TestGeneratorBlockEntity.INDEX_GENERATION_CURRENT);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(this.world.getBlockEntity(this.pos) instanceof TestGeneratorBlockEntity generator)) {
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

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (this.pos == null || !(this.world.getBlockEntity(this.pos) instanceof TestGeneratorBlockEntity)) {
            // 客户端侧没有 pos，直接放行
            return true;
        }
        return this.world.getBlockState(this.pos).isOf(ModBlocks.TEST_GENERATOR)
                && player.squaredDistanceTo(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
