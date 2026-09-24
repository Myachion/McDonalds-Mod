package com.myachi.mcdonaldsmod.energy;

import net.minecraft.block.Block;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * "哪些方块能接电缆"的登记表。
 *
 * <p>加新机器时只要在 {@code ModBlocks.initializeModBlocks()} 里登记一行，不用给方块写专门的类：
 * <ul>
 *     <li>{@code CableConnections.always(MY_MACHINE);} —— 六面都能接；</li>
 *     <li>{@code CableConnections.only(MY_MACHINE, Direction.UP);} —— 只有顶面能接；</li>
 *     <li>{@code CableConnections.custom(MY_MACHINE, CableConnectable.facing(MY_MACHINE.FACING));}
 *         —— 只有正面能接。</li>
 * </ul>
 *
 * <p>完全不想改代码的话，把方块写进数据包标签 {@code mcdonalds-mod:cable_connectable}
 * 也能达到"六面可连"的效果（见 {@code CableBlock#canConnectTo} 的判定顺序）。
 * 没登记的方块一律不能接电缆。
 */
public final class CableConnections {
    private static final Map<Block, CableConnectable> PROVIDERS = new HashMap<>();

    private CableConnections() {
    }

    /** 登记成"六面都能接电缆"。 */
    public static void always(Block... blocks) {
        for (Block block : blocks) {
            PROVIDERS.put(block, CableConnectable.ALL_SIDES);
        }
    }

    /** 登记成"只有这几个面能接电缆"。 */
    public static void only(Block block, Direction... sides) {
        PROVIDERS.put(block, CableConnectable.only(sides));
    }

    /** 登记成自定义规则。 */
    public static void custom(Block block, CableConnectable provider) {
        PROVIDERS.put(block, provider);
    }

    @Nullable
    public static CableConnectable get(Block block) {
        return PROVIDERS.get(block);
    }
}
