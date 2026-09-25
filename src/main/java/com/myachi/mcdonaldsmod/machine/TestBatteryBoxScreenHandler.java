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

/** 测试电池盒的界面逻辑：显示数据 + 调节额定输入/输出的电压、电流。 */
public class TestBatteryBoxScreenHandler extends ScreenHandler {
    public static final int BUTTON_RATED_INPUT_VOLTAGE_DOWN = 0;
    public static final int BUTTON_RATED_INPUT_VOLTAGE_UP = 1;
    public static final int BUTTON_RATED_INPUT_CURRENT_DOWN = 2;
    public static final int BUTTON_RATED_INPUT_CURRENT_UP = 3;
    public static final int BUTTON_RATED_OUTPUT_VOLTAGE_DOWN = 4;
    public static final int BUTTON_RATED_OUTPUT_VOLTAGE_UP = 5;
    public static final int BUTTON_RATED_OUTPUT_CURRENT_DOWN = 6;
    public static final int BUTTON_RATED_OUTPUT_CURRENT_UP = 7;

    private final PropertyDelegate properties;
    private final World world;
    private final @Nullable BlockPos pos;

    public TestBatteryBoxScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos, PropertyDelegate properties) {
        super(ModScreenHandlers.TEST_BATTERY_BOX, syncId);
        checkDataCount(properties, TestBatteryBoxBlockEntity.PROPERTY_COUNT);
        this.properties = properties;
        this.pos = pos;
        this.world = playerInventory.player.getEntityWorld();
        this.addProperties(properties);
    }

    public TestBatteryBoxScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, BlockPos.ORIGIN, new ArrayPropertyDelegate(TestBatteryBoxBlockEntity.PROPERTY_COUNT));
    }

    public int get(int index) {
        return this.properties.get(index);
    }

    /** 储电量（焦耳）。三个 15 位字段在服务端拆开、客户端拼回来。 */
    public long getStoredEnergy() {
        return LongPropertyCodec.combine(
                get(TestBatteryBoxBlockEntity.INDEX_ENERGY_LOW),
                get(TestBatteryBoxBlockEntity.INDEX_ENERGY_MID),
                get(TestBatteryBoxBlockEntity.INDEX_ENERGY_HIGH));
    }

    public long getInputPower() {
        return getInputPowerMilliWatts() / 1000L;
    }

    public long getOutputPower() {
        return getOutputPowerMilliWatts() / 1000L;
    }

    /** 实测输入功率（mW）= 电压（V）× 电流（mA）。 */
    public long getInputPowerMilliWatts() {
        return (long) get(TestBatteryBoxBlockEntity.INDEX_INPUT_VOLTAGE)
                * getInputCurrentMilliAmps();
    }

    /** 实测输入电流（mA）。 */
    public int getInputCurrentMilliAmps() {
        return (int) LongPropertyCodec.combine(
                get(TestBatteryBoxBlockEntity.INDEX_INPUT_CURRENT_LOW),
                get(TestBatteryBoxBlockEntity.INDEX_INPUT_CURRENT_MID),
                get(TestBatteryBoxBlockEntity.INDEX_INPUT_CURRENT_HIGH));
    }

    /** 实测输出电流（mA）。 */
    public int getOutputCurrentMilliAmps() {
        return (int) LongPropertyCodec.combine(
                get(TestBatteryBoxBlockEntity.INDEX_OUTPUT_CURRENT_LOW),
                get(TestBatteryBoxBlockEntity.INDEX_OUTPUT_CURRENT_MID),
                get(TestBatteryBoxBlockEntity.INDEX_OUTPUT_CURRENT_HIGH));
    }
    /** 实测输出功率（mW）。 */
    public long getOutputPowerMilliWatts() {
        return (long) get(TestBatteryBoxBlockEntity.INDEX_OUTPUT_VOLTAGE)
                * getOutputCurrentMilliAmps();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(this.world.getBlockEntity(this.pos) instanceof TestBatteryBoxBlockEntity battery)) {
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

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (this.pos == null || !(this.world.getBlockEntity(this.pos) instanceof TestBatteryBoxBlockEntity)) {
            return true;
        }
        return this.world.getBlockState(this.pos).isOf(ModBlocks.TEST_BATTERY_BOX)
                && player.squaredDistanceTo(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
