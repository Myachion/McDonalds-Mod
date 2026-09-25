package com.myachi.mcdonaldsmod.energy;

import com.myachi.mcdonaldsmod.ModBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一张电网：一组连通的电缆 + 挂在它们上面的机器（含"接在哪个面"）。
 *
 * <p>结算规则（和用户确认过的设计一致）：
 * <ol>
 *     <li>网络电压取网内所有电源里最高的那个额定输出电压；</li>
 *     <li>额定电压低于网络电压的电源不并网；而且只有<b>输出面</b>接在网里的机器才能供电；</li>
 *     <li>只有<b>输入面</b>接在网里的机器才能被充电；</li>
 *     <li>负载的实际输入电压 = min(网络电压, 它的额定输入电压)；</li>
 *     <li>可用总功率不够时，按各负载的额定功率比例分配；</li>
 *     <li>电源侧也按各自的可用功率比例扣缓冲区。</li>
 * </ol>
 *
 * <h2>逐段电流</h2>
 * 电缆本身只是导线，网络结算先算出"每个负载拿到多少电流"，再把这些电流按
 * <b>多源 BFS 生成树</b>回推到电源：每个负载归给离它最近的电源，沿途每根电缆
 * 累加这股电流。于是每根电缆上的电流 = 它下游所有负载电流之和。真实电路要用
 * 基尔霍夫定律解，这里用生成树做近似——有环的网会按 BFS 的形状分配，够游戏用了。
 *
 * <p>电缆载流量、线路电损、过压烧毁都还没做。
 */
public final class EnergyNetwork {
    /** 每个提供能量的机器。 */
    private record Provider(BlockPos pos, EnergyStorage storage, long availableMilliWatts) {
    }

    /** 每个吸收能量的机器。 */
    private record Consumer(BlockPos pos, EnergyStorage storage, int voltage, long demandMilliWatts) {
    }

    /** 结算完成后，一股实际流进负载的电流（mA）+ 它可能接的电缆。 */
    private record LoadFlow(List<BlockPos> cables, long currentMilliAmps) {
    }

    private final Set<BlockPos> cables;
    /** 机器位置 -> 它接在网络上的那些面（可能不止一个）。 */
    private final Map<BlockPos, Set<Direction>> machines;
    // 上次结算的结果，供万用表查询（功率单位 mW）
    private int lastVoltage;
    private long lastSupply;
    private long lastDemand;
    private long lastDelivered;
    /** 每根电缆上承载的电流（mA），来自上次结算。 */
    private final Map<BlockPos, Long> cableCurrents = new HashMap<>();
    /** 上次算出的线路总损耗（mW）。因为损耗依赖电流、电流依赖分配，用上一 tick 的值错开一轮。 */
    private long lastLossMilliWatts;
    /** 上次结算里超过额定电流的电缆。 */
    private final Set<BlockPos> overloaded = new LinkedHashSet<>();

    EnergyNetwork(Set<BlockPos> cables, Map<BlockPos, Set<Direction>> machines) {
        this.cables = cables;
        this.machines = machines;
    }

    public int getCableCount() {
        return this.cables.size();
    }

    public Set<BlockPos> getCables() {
        return this.cables;
    }

    public int getMachineCount() {
        return this.machines.size();
    }

    public int getVoltage() {
        return this.lastVoltage;
    }

    public long getSupplyMilliWatts() {
        return this.lastSupply;
    }

    public long getDemandMilliWatts() {
        return this.lastDemand;
    }

    public long getDeliveredMilliWatts() {
        return this.lastDelivered;
    }

    /** 这根电缆上流过的电流（mA），上次结算的结果。 */
    public long getCableCurrentMilliAmps(BlockPos cablePos) {
        return this.cableCurrents.getOrDefault(cablePos, 0L);
    }

    /** 上次结算的线路总损耗（mW）。 */
    public long getLossMilliWatts() {
        return this.lastLossMilliWatts;
    }

    /** 上次结算里超过额定电流的电缆（每 tick 重新算）。 */
    public Set<BlockPos> getOverloadedCables() {
        return this.overloaded;
    }

    private void record(int voltage, long supply, long demand, long delivered) {
        this.lastVoltage = voltage;
        this.lastSupply = supply;
        this.lastDemand = demand;
        this.lastDelivered = delivered;
    }

    /** 这台机器接在网上的面里，有没有任意一个能当输出口。 */
    private static boolean canOutput(EnergyStorage storage, Set<Direction> sides) {
        for (Direction side : sides) {
            if (storage.canProvideEnergyFrom(side)) {
                return true;
            }
        }
        return false;
    }

    /** 这台机器接在网上的面里，有没有任意一个能当输入口。 */
    private static boolean canInput(EnergyStorage storage, Set<Direction> sides) {
        for (Direction side : sides) {
            if (storage.canReceiveEnergyOn(side)) {
                return true;
            }
        }
        return false;
    }

    /** 机器通过哪些"允许收发"的面连着电缆。 */
    private Set<BlockPos> connectedCables(BlockPos machine, Set<Direction> sides, EnergyStorage storage, boolean forInput) {
        Set<BlockPos> found = new LinkedHashSet<>();
        for (Direction side : sides) {
            boolean allowed = forInput ? storage.canReceiveEnergyOn(side) : storage.canProvideEnergyFrom(side);
            if (allowed) {
                BlockPos cable = machine.offset(side);
                if (this.cables.contains(cable)) {
                    found.add(cable);
                }
            }
        }
        return found;
    }

    public void tick(ServerWorld world) {
        List<Provider> providers = new ArrayList<>();
        List<Consumer> consumers = new ArrayList<>();
        List<EnergyStorage> storages = new ArrayList<>();
        this.cableCurrents.clear();

        // 1. 网络电压 = 所有"能供电、且输出面接在网里"的机器里最高的额定输出电压
        int networkVoltage = 0;
        for (Map.Entry<BlockPos, Set<Direction>> entry : this.machines.entrySet()) {
            if (!(world.getBlockEntity(entry.getKey()) instanceof EnergyStorage storage)) {
                continue;
            }
            storages.add(storage);
            if (storage.canProvideEnergy() && canOutput(storage, entry.getValue())) {
                networkVoltage = Math.max(networkVoltage, storage.getRatedOutputVoltage());
            }
        }
        if (networkVoltage <= 0) {
            this.clearReadings(storages);
            record(0, 0, 0, 0);
            return;
        }

        // 2. 电源
        long totalSupply = 0;
        for (Map.Entry<BlockPos, Set<Direction>> entry : this.machines.entrySet()) {
            if (!(world.getBlockEntity(entry.getKey()) instanceof EnergyStorage storage)
                    || !storage.canProvideEnergy()
                    || !canOutput(storage, entry.getValue())
                    || storage.getRatedOutputVoltage() != networkVoltage) {
                continue;
            }
            long rated = (long) networkVoltage * storage.getRatedOutputCurrent();
            long fromBuffer = storage.getStoredEnergyMilliJoules() * 20L;
            long available = Math.min(rated, fromBuffer);
            if (available > 0) {
                providers.add(new Provider(entry.getKey(), storage, available));
                totalSupply += available;
            }
        }

        // 3. 负载
        long totalDemand = 0;
        for (Map.Entry<BlockPos, Set<Direction>> entry : this.machines.entrySet()) {
            if (!(world.getBlockEntity(entry.getKey()) instanceof EnergyStorage storage)
                    || !storage.canReceiveEnergy()
                    || !canInput(storage, entry.getValue())) {
                continue;
            }
            int voltage = Math.min(networkVoltage, storage.getRatedInputVoltage());
            long demand = (long) voltage * storage.getRatedInputCurrent();
            if (demand > 0) {
                consumers.add(new Consumer(entry.getKey(), storage, voltage, demand));
                totalDemand += demand;
            }
        }

        long delivered = Math.min(totalSupply, totalDemand);
        if (delivered <= 0) {
            this.clearReadings(storages);
            this.cableCurrents.clear();
            this.measureLossAndOverload(world, networkVoltage);
            record(networkVoltage, totalSupply, totalDemand, 0);
            return;
        }

        // 4. 按额定功率比例分给负载，记下每台机器实际吃进的电流
        List<LoadFlow> flows = new ArrayList<>();
        for (Consumer consumer : consumers) {
            long share = consumer.demandMilliWatts() * delivered / totalDemand;
            long energy = share / 20L;
            if (energy <= 0) {
                consumer.storage().setMeasuredInput(0, 0);
                continue;
            }
            long accepted = consumer.storage().insertEnergy(energy);
            int current = consumer.voltage() > 0
                    ? (int) Math.min(Integer.MAX_VALUE, accepted * 20L / consumer.voltage())
                    : 0;
            consumer.storage().setMeasuredInput(consumer.voltage(), current);
            if (current > 0) {
                Set<BlockPos> cables = connectedCables(consumer.pos(), this.machines.get(consumer.pos()),
                        consumer.storage(), true);
                flows.add(new LoadFlow(new ArrayList<>(cables), current));
            }
        }

        // 5. 电源按可用功率比例扣缓冲区（额外多扣一份线路损耗），并把实际输出记到界面上
        for (Provider provider : providers) {
            long share = provider.availableMilliWatts() * delivered / totalSupply;
            long lossShare = delivered > 0 ? this.lastLossMilliWatts * share / delivered : 0;
            long energy = (share + lossShare) / 20L;
            long taken = energy > 0 ? provider.storage().extractEnergy(energy) : 0;
            int current = networkVoltage > 0
                    ? (int) Math.min(Integer.MAX_VALUE, taken * 20L / networkVoltage)
                    : 0;
            provider.storage().setMeasuredOutput(networkVoltage, current);
        }

        traceCableCurrents(world, providers, flows);
        measureLossAndOverload(world, networkVoltage);
        record(networkVoltage, totalSupply, totalDemand, delivered);
    }

    /**
     * 逐段统计：这根电缆有没有超过额定电流、以及它按 I²R 产生多少损耗。
     * 没有电流的电缆既不损耗也不超载——所以"拉了一大片但只用一小段"时，只有真正带电的那段在算。
     */
    private void measureLossAndOverload(ServerWorld world, int networkVoltage) {
        long loss = 0;
        this.overloaded.clear();
        for (Map.Entry<BlockPos, Long> entry : this.cableCurrents.entrySet()) {
            BlockPos pos = entry.getKey();
            long current = entry.getValue();
            if (current <= 0 || !(world.getBlockState(pos).getBlock() instanceof CableBlock cable)) {
                continue;
            }
            // 过流或者过压（网络电压高于电缆的绝缘等级）都算过载
            if (current > cable.getRatedCurrentMilliAmps() || networkVoltage > cable.getVoltage()) {
                this.overloaded.add(pos);
            }
            if (EnergyConfig.isEnergyLossEnabled()) {
                long resistance = cable.getResistanceMilliOhms();
                if (resistance > 0) {
                    // 损耗(mW) = I(mA)² × R(mΩ) / 1e6
                    loss += current * current * resistance / 1_000_000L;
                }
            }
        }
        this.lastLossMilliWatts = loss;
    }

    /**
     * 把每台负载吃到的电流按 BFS 生成树回推到电源，于是每根电缆得到"它下游的总电流"。
     * 离负载最近的电源负责供它，路径就是生成树上的父链。
     */
    private void traceCableCurrents(ServerWorld world, List<Provider> providers, List<LoadFlow> flows) {
        if (flows.isEmpty()) {
            return;
        }
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        Map<BlockPos, Integer> distance = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        // 多源 BFS：所有电源紧挨着的电缆作为起点
        for (Provider provider : providers) {
            for (Direction direction : Direction.values()) {
                BlockPos cable = provider.pos().offset(direction);
                if (this.cables.contains(cable) && distance.putIfAbsent(cable, 0) == null) {
                    queue.add(cable);
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            int nextDistance = distance.get(current) + 1;
            for (Direction direction : Direction.values()) {
                BlockPos next = current.offset(direction);
                if (!this.cables.contains(next) || distance.containsKey(next)) {
                    continue;
                }
                distance.put(next, nextDistance);
                parent.put(next, current);
                queue.add(next);
            }
        }

        // 每个负载沿父链回传自己的电流
        for (LoadFlow flow : flows) {
            BlockPos best = null;
            int bestDistance = Integer.MAX_VALUE;
            for (BlockPos cable : flow.cables()) {
                Integer dist = distance.get(cable);
                if (dist != null && dist < bestDistance) {
                    bestDistance = dist;
                    best = cable;
                }
            }
            while (best != null) {
                this.cableCurrents.merge(best, flow.currentMilliAmps(), Long::sum);
                best = parent.get(best);
            }
        }
    }

    private void clearReadings(List<EnergyStorage> storages) {
        for (EnergyStorage storage : storages) {
            storage.setMeasuredInput(0, 0);
            storage.setMeasuredOutput(0, 0);
        }
        this.cableCurrents.clear();
        this.overloaded.clear();
        this.lastLossMilliWatts = 0;
    }

    static Set<BlockPos> newCableSet() {
        return new LinkedHashSet<>();
    }

    static Map<BlockPos, Set<Direction>> newMachineMap() {
        return new LinkedHashMap<>();
    }
}
