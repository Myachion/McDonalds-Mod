package com.myachi.mcdonaldsmod.energy;

import com.myachi.mcdonaldsmod.ModBlockTags;
import com.myachi.mcdonaldsmod.McDonaldsMod;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
    /** 从什么时候开始冒烟预警（1.5 秒）。 */
    private static final int WARNING_TICKS = 30;
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
    /**
     * 本 tick 每台机器的读数累积：[输入电压V, 输入电流mA, 输出电压V, 输出电流mA]。
     *
     * <p>为什么要汇总：一台机器可以同时挂在两张网上（例如电池盒：上/下面那张网给它充电、
     * 侧面那张网让它放电）。逐网络直接往机器里写会把另一张网的读数覆盖掉，所以先汇总、最后统一写回。
     */
    private final Map<BlockPos, long[]> readings = new HashMap<>();

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
        this.readings.clear();
        for (EnergyNetwork network : this.networks) {
            network.tick(this.world, this.readings);
        }
        applyReadings();
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

    /** 把本 tick 汇总出来的读数写回机器；一张网都没碰到的机器清零（免得界面停在旧值）。 */
    private void applyReadings() {
        for (Map.Entry<BlockPos, long[]> entry : this.readings.entrySet()) {
            if (this.world.getBlockEntity(entry.getKey()) instanceof EnergyStorage storage) {
                long[] reading = entry.getValue();
                storage.setMeasuredInput((int) reading[0], (int) Math.min(Integer.MAX_VALUE, reading[1]));
                storage.setMeasuredOutput((int) reading[2], (int) Math.min(Integer.MAX_VALUE, reading[3]));
            }
        }
        for (BlockPos pos : this.machines) {
            if (this.readings.containsKey(pos)) {
                continue;
            }
            if (this.world.getBlockEntity(pos) instanceof EnergyStorage storage) {
                storage.setMeasuredInput(0, 0);
                storage.setMeasuredOutput(0, 0);
            }
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
            } else if (ticks >= WARNING_TICKS && ticks % 4 == 0) {
                // 快撑不住了：冒点烟提醒玩家这根线在发烫
                this.world.spawnParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        2, 0.12, 0.12, 0.12, 0.01);
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
            spawnBurnoutEffects(pos);
            // 直接移除，不掉落任何东西
            this.world.removeBlock(pos, false);
        }
        if (!burnt.isEmpty()) {
            this.dirty = true;
        }
    }

    /** 烧断瞬间：大烟、火苗、岩浆火球、电火花一起上，再配一声滋滋。 */
    private void spawnBurnoutEffects(BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        ServerWorld world = this.world;
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 24, 0.25, 0.25, 0.25, 0.02);
        world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 16, 0.3, 0.3, 0.3, 0.03);
        world.spawnParticles(ParticleTypes.FLAME, x, y, z, 18, 0.2, 0.2, 0.2, 0.06);
        world.spawnParticles(ParticleTypes.LAVA, x, y, z, 6, 0.2, 0.2, 0.2, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 20, 0.3, 0.3, 0.3, 0.15);
        world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.7F, 1.4F);
        world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 1.8F);
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

        /*
         * 网络 = 一段"电缆之间互相连通"的组件 + 贴着这段电缆的机器。
         *
         * 关键点：机器**不导通**，而且一台机器可以同时属于多张网。
         * 比如"发电机 — 电缆 — 电池盒（上面进电、侧面出电）— 电缆 — 电炉"：
         * 电池盒上面那截电缆和侧面那截电缆是两个组件，电池盒同时属于两张网
         * （一张网里当负载充电、另一张网里当电源放电）。
         * 以前用"从机器出发的洪水填充 + 全局 visited"会把机器只分给先遍历到的那张网，
         * 另一张网就凭空少了一台机器（表现为：发电机实际输出 0、电池盒→电炉的线 0 W）。
         */
        // 种子电缆：已登记的电缆 + "贴着已知机器"的电缆。
        // 后者很关键：电缆只在被放下时登记一次，服务器/区块重新加载后不会自己回来；
        // 而机器每 tick 都会登记，所以靠机器把它们身边的电缆重新发现一遍，电网就不会在重载后"消失"。
        Set<BlockPos> seeds = new LinkedHashSet<>(this.cables);
        for (BlockPos machine : this.machines) {
            for (Direction direction : Direction.values()) {
                BlockPos cable = machine.offset(direction);
                if (this.world.getBlockState(cable).isIn(ModBlockTags.CABLE)) {
                    seeds.add(cable);
                }
            }
        }

        Set<BlockPos> visited = new HashSet<>();
        List<EnergyNetwork> built = new ArrayList<>();
        this.cableToNetwork.clear();
        for (BlockPos start : seeds) {
            if (visited.contains(start)) {
                continue;
            }
            Set<BlockPos> component = EnergyNetwork.newCableSet();
            Map<BlockPos, Set<Direction>> componentMachines = EnergyNetwork.newMachineMap();
            Deque<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            visited.add(start);
            while (!queue.isEmpty()) {
                BlockPos cable = queue.poll();
                component.add(cable);
                for (Direction direction : Direction.values()) {
                    BlockPos next = cable.offset(direction);
                    // 跨未加载区块就断开
                    if (!this.isLoaded(next)) {
                        continue;
                    }
                    BlockState state = this.world.getBlockState(next);
                    if (state.isIn(ModBlockTags.CABLE)) {
                        // 顺便补登记：重载后这些电缆原本不在 this.cables 里
                        this.cables.add(next);
                        if (visited.add(next)) {
                            queue.add(next);
                        }
                    } else if (this.world.getBlockEntity(next) instanceof EnergyStorage) {
                        // 记下"机器被接在哪一面"：direction 是电缆指向机器的方向，取反才是机器的面
                        componentMachines.computeIfAbsent(next, key -> new LinkedHashSet<>())
                                .add(direction.getOpposite());
                    }
                }
            }
            EnergyNetwork network = new EnergyNetwork(component, componentMachines);
            built.add(network);
            for (BlockPos cable : component) {
                this.cableToNetwork.put(cable, network);
            }
        }
        this.networks = built;
        this.dirty = false;
    }

    private boolean isLoaded(BlockPos pos) {
        return this.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

}
