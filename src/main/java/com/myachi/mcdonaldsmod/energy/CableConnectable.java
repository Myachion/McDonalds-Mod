package com.myachi.mcdonaldsmod.energy;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

import java.util.Set;

/**
 * "电缆能不能接到这一面"的规则。
 *
 * <p>{@code side} 是<b>机器方块的面的方向</b>，也就是"电缆在机器的哪一边"，不是电缆自己的朝向。
 *
 * <p>给方块接上这套规则的三种方式（从简单到灵活）：
 * <ol>
 *     <li>数据包：把方块写进 {@code data/mcdonalds-mod/tags/block/cable_connectable.json}，六面可连，零代码；</li>
 *     <li>代码：在 {@code ModBlocks.initializeModBlocks()} 里写一行 {@code CableConnections.always(MY_MACHINE)}，同样是六面可连；</li>
 *     <li>要限制面（比如只有正面出线）：{@code CableConnections.custom(MY_MACHINE, CableConnectable.facing(MY_MACHINE.FACING))}。</li>
 * </ol>
 *
 * <p>没有登记、也不在标签里的方块默认不能接电缆。
 */
public interface CableConnectable {
    boolean canCableConnect(BlockState state, BlockView world, BlockPos pos, Direction side);

    /** 六面都能接。 */
    CableConnectable ALL_SIDES = (state, world, pos, side) -> true;

    /** 只有列出来的这几个面能接。 */
    static CableConnectable only(Direction... sides) {
        Set<Direction> allowed = Set.of(sides);
        return (state, world, pos, side) -> allowed.contains(side);
    }

    /** 只有某个朝向属性指向的那一面能接（例如 HorizontalFacingBlock.FACING），用在"正面出线"的机器上。 */
    static CableConnectable facing(Property<Direction> property) {
        return (state, world, pos, side) -> state.get(property) == side;
    }
}
