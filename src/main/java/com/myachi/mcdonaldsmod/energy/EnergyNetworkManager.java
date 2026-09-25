package com.myachi.mcdonaldsmod.energy;

import com.myachi.mcdonaldsmod.ModBlockTags;
import com.myachi.mcdonaldsmod.McDonaldsMod;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一个世界里的所有电网。
 *
 * <p>网络结构只在方块增删时重算（{@link #dirty}），数值每 tick 结算一次。
 * 跨未加载区块时网络断开：洪水填充不会走进没加载的区块。
 */
public final class EnergyNetworkManager {
    /** 超过额定电流多久才烧毁：3 秒。 */
    private static final int BURNOUT_TICKS = 60;
    /** 临时诊断用：最多打多少行过载日志。 */
    private static int debugBurnoutLogs = 0;

    private final ServerWorld world;
    /** 已知电缆位置。 */
    private final Set<BlockPos> cables = new LinkedHashSet<>();
    /** 已知机器位置（机器方块实体每 tick 会来登记一次）。 */
    private final Set<BlockPos> machines = new LinkedHashSet<>();
    private boolean dirty = true;
    private List<EnergyNetwork> networks = List.of();
    /** 电缆位置 -> 它属于哪张网，给万用表这类查询用。 */
    private final Map<BlockPos, EnergyNetwork> cableToNetwork = new HashMap<>();
    /** 每根电缆已经连续过载了多少 tick。 */
    private final Map<BlockPos, Integer> overloadTicks = new HashMap<>();

    EnergyNetworkManager(ServerWorld world) {
        this.world = world;
    }

    /** 电缆被放下或移除时调用。 */
    public void markCable(BlockPos pos, boolean present) {
        boolean changed = present ? this.cables.add(pos) : this.cables.remove(pos);
        if (changed) {
            this.dirty = true;
        }
    }

    /** 机器方块实体每 tick 登记自己（幂等，开销只有一个 HashSet.add）。 */
    public void registerMachine(BlockPos pos) {
        if (this.machines.add(pos)) {
            this.dirty = true;
        }
    }

    public void tick() {
        if (this.dirty) {
            rebuild();
        }
        for (EnergyNetwork network : this.networks) {
            network.tick(this.world);
        }
        if (EnergyConfig.isOverloadBurnoutEnabled()) {
            // 必须把所有网络的过载电缆汇总起来一起算：逐网络处理的话，
            // 空网络的"降温"会把别的网络正在计时的电缆清掉。
            Set<BlockPos> allOverloaded = new HashSet<>();
            for (EnergyNetwork network : this.networks) {
                allOverloaded.addAll(network.getOverloadedCables());
            }
            applyOverloadBurnout(allOverloaded);
        }
    }

    /**
     * 过载烧毁：某根电缆的电流超过它的额定值并持续 3 秒就直接烧掉，<b>不留掉落物</b>。
     * 判定是逐根的——一根烧断只影响它自己，后面的电网断掉但机器不会跟着炸。
     */
    private void applyOverloadBurnout(Set<BlockPos> overloaded) {
        if (this.overloadTicks.isEmpty() && overloaded.isEmpty()) {
            return;
        }
        List<BlockPos> burnt = new ArrayList<>();
        for (BlockPos pos : overloaded) {
            int ticks = this.overloadTicks.merge(pos, 1, Integer::sum);
            if (ticks >= BURNOUT_TICKS) {
                burnt.add(pos);
            }
        }
        // 不再过载的电缆缓慢降温，而不是立刻清零。
        // 电源缓存不足时电流是脉冲式的，一断流就清零的话永远烧不断。
        Iterator<Map.Entry<BlockPos, Integer>> iterator = this.overloadTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            if (overloaded.contains(entry.getKey())) {
                continue;
            }
            int cooled = entry.getValue() - 1;
            if (cooled <= 0) {
                iterator.remove();
            } else {
                entry.setValue(cooled);
            }
        }
        for (BlockPos pos : burnt) {
            this.overloadTicks.remove(pos);
            // 直接移除，不掉落任何东西
            this.world.removeBlock(pos, false);
        }
        if (!burnt.isEmpty()) {
            this.dirty = true;
        }
    }

    /** 查某根电缆属于哪张网（没接网就是 null）。 */
    public EnergyNetwork getNetworkAt(BlockPos cablePos) {
        return this.cableToNetwork.get(cablePos);
    }

    private void rebuild() {
        // 清掉已经不存在的电缆和机器。
        // 注意跳过未加载的区块：那里的方块「查不到」不代表被拆了，误删会让整张网失效。
        this.cables.removeIf(pos -> this.isLoaded(pos) && !this.world.getBlockState(pos).isIn(ModBlockTags.CABLE));
        this.machines.removeIf(pos -> this.isLoaded(pos) && !(this.world.getBlockEntity(pos) instanceof EnergyStorage));

        Set<BlockPos> visited = new HashSet<>();
        List<EnergyNetwork> built = new ArrayList<>();
        this.cableToNetwork.clear();
        for (BlockPos machine : this.machines) {
            if (!visited.contains(machine)) {
                EnergyNetwork network = flood(machine, visited);
                if (network != null) {
                    built.add(network);
                }
            }
        }
        for (EnergyNetwork network : built) {
            for (BlockPos cable : network.getCables()) {
                this.cableToNetwork.put(cable, network);
            }
        }
        this.networks = built;
        this.dirty = false;
    }

    private boolean isLoaded(BlockPos pos) {
        return this.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /** 从一台机器出发，沿电缆铺开，收集连通的电缆和其它机器。 */
    private EnergyNetwork flood(BlockPos start, Set<BlockPos> visited) {
        Set<BlockPos> foundCables = EnergyNetwork.newCableSet();
        Map<BlockPos, Set<Direction>> foundMachines = EnergyNetwork.newMachineMap();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        Set<Direction> startSides = new LinkedHashSet<>();
        foundMachines.put(start, startSides);
        // 起点机器接在哪些面：看它哪几面贴着电缆
        for (Direction direction : Direction.values()) {
            BlockPos next = start.offset(direction);
            if (this.world.getBlockState(next).isIn(ModBlockTags.CABLE)) {
                startSides.add(direction);
            }
        }

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.offset(direction);
                if (visited.contains(next)) {
                    continue;
                }
                // 跨未加载区块就断开
                if (!this.isLoaded(next)) {
                    continue;
                }
                BlockState state = this.world.getBlockState(next);
                if (state.isIn(ModBlockTags.CABLE)) {
                    visited.add(next);
                    foundCables.add(next);
                    queue.add(next);
                } else if (this.world.getBlockEntity(next) instanceof EnergyStorage) {
                    visited.add(next);
                    // 记下"机器被接在哪一面"：direction 是电缆指向机器的方向，取反才是机器的面
                    foundMachines.computeIfAbsent(next, key -> new LinkedHashSet<>()).add(direction.getOpposite());
                }
            }
        }

        // 只有一台机器、又没连着电缆的，不算一个网络
        if (foundCables.isEmpty()) {
            return null;
        }
        return new EnergyNetwork(foundCables, foundMachines);
    }
}
