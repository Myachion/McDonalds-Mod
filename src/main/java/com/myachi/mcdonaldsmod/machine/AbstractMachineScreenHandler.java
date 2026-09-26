package com.myachi.mcdonaldsmod.machine;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * "只有按钮、没有物品槽"的机器界面容器基类（有物品槽的机器请参考电炉那套，直接继承原版容器）。
 *
 * <p>提供的东西：
 * <ul>
 *     <li>按 {@link MachineProperties} 标准索引读数据的成套 getter（电量、实测输入输出、额定值）；</li>
 *     <li>{@link #machine(Class)}：服务端把按钮点击翻译成方块实体上的方法调用时，用它拿机器；</li>
 *     <li>{@link #canUse(PlayerEntity)} / {@link #quickMove} 的默认实现。</li>
 * </ul>
 *
 * <p>子类要做的：构造器里把 {@code ScreenHandlerType}、方块实体类型、属性字段个数传给 super，
 * 然后实现 {@link #onButtonClick}。按钮 id 的约定是"偶数减、奇数加"（见 {@code MachineScreen}）。
 *
 * <pre>{@code
 * public class MyMachineScreenHandler extends AbstractMachineScreenHandler {
 *     public MyMachineScreenHandler(int syncId, PlayerInventory inv, BlockPos pos, PropertyDelegate props) {
 *         super(ModScreenHandlers.MY_MACHINE, syncId, inv, pos, props, MyMachineBlockEntity.class);
 *     }
 *     public MyMachineScreenHandler(int syncId, PlayerInventory inv) {
 *         super(ModScreenHandlers.MY_MACHINE, syncId, inv, MyMachineBlockEntity.PROPERTY_COUNT, MyMachineBlockEntity.class);
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractMachineScreenHandler extends ScreenHandler {
    private final PropertyDelegate properties;
    private final World world;
    /** 客户端构造时是 {@link BlockPos#ORIGIN}（数值已经同步过来，不需要定位方块）。 */
    private final BlockPos pos;
    private final Class<? extends AbstractMachineBlockEntity> machineType;

    /** 服务端构造：直接接在方块实体的属性表上。 */
    protected AbstractMachineScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory,
                                           BlockPos pos, PropertyDelegate properties,
                                           Class<? extends AbstractMachineBlockEntity> machineType) {
        super(type, syncId);
        checkDataCount(properties, properties.size());
        this.properties = properties;
        this.pos = pos;
        this.machineType = machineType;
        this.world = playerInventory.player.getEntityWorld();
        this.addProperties(properties);
    }

    /** 客户端构造：数值由服务端每 tick 同步进一个空白数组。 */
    protected AbstractMachineScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory,
                                           int propertyCount,
                                           Class<? extends AbstractMachineBlockEntity> machineType) {
        this(type, syncId, playerInventory, BlockPos.ORIGIN, new ArrayPropertyDelegate(propertyCount), machineType);
    }

    // ------------------------------------------------------------------
    // 读数值（布局见 MachineProperties）
    // ------------------------------------------------------------------

    public int get(int index) {
        return this.properties.get(index);
    }

    /** 缓冲区电量（mJ）。 */
    public long storedEnergyMilliJoules() {
        return LongPropertyCodec.combine(
                get(MachineProperties.ENERGY_LOW),
                get(MachineProperties.ENERGY_MID),
                get(MachineProperties.ENERGY_HIGH));
    }

    /** 实测输入电压（V）。 */
    public int inputVoltage() {
        return get(MachineProperties.INPUT_VOLTAGE);
    }

    /** 实测输入电流（mA）。 */
    public int inputCurrentMilliAmps() {
        return (int) LongPropertyCodec.combine(
                get(MachineProperties.INPUT_CURRENT_LOW),
                get(MachineProperties.INPUT_CURRENT_MID),
                get(MachineProperties.INPUT_CURRENT_HIGH));
    }

    /** 实测输入功率（mW）= 电压 × 电流。 */
    public long inputPowerMilliWatts() {
        return (long) inputVoltage() * inputCurrentMilliAmps();
    }

    /** 实测输出电压（V）。 */
    public int outputVoltage() {
        return get(MachineProperties.OUTPUT_VOLTAGE);
    }

    /** 实测输出电流（mA）。 */
    public int outputCurrentMilliAmps() {
        return (int) LongPropertyCodec.combine(
                get(MachineProperties.OUTPUT_CURRENT_LOW),
                get(MachineProperties.OUTPUT_CURRENT_MID),
                get(MachineProperties.OUTPUT_CURRENT_HIGH));
    }

    /** 实测输出功率（mW）。 */
    public long outputPowerMilliWatts() {
        return (long) outputVoltage() * outputCurrentMilliAmps();
    }

    /** 额定输入电压（V）。 */
    public int ratedInputVoltage() {
        return get(MachineProperties.RATED_INPUT_VOLTAGE);
    }

    /** 额定输入电流（A，整数档位）。 */
    public int ratedInputCurrentAmps() {
        return get(MachineProperties.RATED_INPUT_CURRENT);
    }

    /** 额定输出电压（V）。 */
    public int ratedOutputVoltage() {
        return get(MachineProperties.RATED_OUTPUT_VOLTAGE);
    }

    /** 额定输出电流（A，整数档位）。 */
    public int ratedOutputCurrentAmps() {
        return get(MachineProperties.RATED_OUTPUT_CURRENT);
    }

    // ------------------------------------------------------------------
    // 机器定位与默认实现
    // ------------------------------------------------------------------

    /** 取这台界面对应的机器方块实体；客户端或不匹配时返回 null。 */
    protected @Nullable <T extends AbstractMachineBlockEntity> T machine(Class<T> type) {
        BlockEntity blockEntity = this.world.getBlockEntity(this.pos);
        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }

    /** 没有物品槽，所以 Shift 点击没有可搬运的东西。 */
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        BlockEntity blockEntity = this.world.getBlockEntity(this.pos);
        if (!this.machineType.isInstance(blockEntity)) {
            // 客户端侧方块实体还没同步过来，直接放行（真伪由服务端这次校验决定）
            return true;
        }
        return player.squaredDistanceTo(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
