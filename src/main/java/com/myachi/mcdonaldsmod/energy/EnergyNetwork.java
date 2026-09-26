package com.myachi.mcdonaldsmod.energy;

import com.myachi.mcdonaldsmod.ModBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    /** 临时诊断开关：启动参数加 {@code -Dmcdonalds.debug.energy=true} 就会每 20 tick 打一行结算明细。 */
    private static final boolean DEBUG = Boolean.getBoolean("mcdonalds.debug.energy");
    private static final Logger LOGGER = LoggerFactory.getLogger("mcdonalds-mod/energy");

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
    /** 诊断日志用的计数（每张网各自计数，避免多张网共享静态计数器时漏打）。 */
    private int debugTicks;

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

    /**
     * 结算这张网。
     *
     * @param readings 由 {@code EnergyNetworkManager} 传进来的汇总表：
     *                 机器位置 -> [输入电压V, 输入电流mA, 输出电压V, 输出电流mA]。
     *                 一台机器可能挂在多张网上（例如电池盒），所以这里只累加，最后由管理器统一写回。
     */
    public void tick(ServerWorld world, Map<BlockPos, long[]> readings) {
        this.debugTicks++;
        List<Provider> providers = new ArrayList<>();
        List<Consumer> consumers = new ArrayList<>();
        this.cableCurrents.clear();

        // 1. 网络电压 = 所有"能供电、且输出面接在网里"的机器里最高的额定输出电压
        int networkVoltage = 0;
        for (Map.Entry<BlockPos, Set<Direction>> entry : this.machines.entrySet()) {
            if (!(world.getBlockEntity(entry.getKey()) instanceof EnergyStorage storage)) {
                continue;
            }
            if (storage.canProvideEnergy() && canOutput(storage, entry.getValue())) {
                networkVoltage = Math.max(networkVoltage, storage.getRatedOutputVoltage());
            }
        }
        if (networkVoltage <= 0) {
            this.cableCurrents.clear();
            this.measureLossAndOverload(world, networkVoltage);
            debugSummary(0, 0, 0, 0);
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

        // 3. 负载：需求 = min(额定功率, 它这一 tick 真正需要补的电)
        //    "真正需要补的电" = 缓冲区空出来的空间（见 EnergyStorage#getRequestedInputMilliWatts）。
        //    结算在所有方块实体 tick 之后跑，所以工作的机器已经消耗掉一部分缓冲区，
        //    空出来的空间正好等于它的耗电（例如 96 W）；缓冲区满又不工作的机器空间是 0。
        long totalDemand = 0;
        for (Map.Entry<BlockPos, Set<Direction>> entry : this.machines.entrySet()) {
            if (!(world.getBlockEntity(entry.getKey()) instanceof EnergyStorage storage)
                    || !storage.canReceiveEnergy()
                    || !canInput(storage, entry.getValue())) {
                continue;
            }
            int voltage = Math.min(networkVoltage, storage.getRatedInputVoltage());
            long rated = (long) voltage * storage.getRatedInputCurrent();
            long requested = Math.max(0L, storage.getRequestedInputMilliWatts());
            long demand = Math.min(rated, requested);
            if (demand > 0) {
                consumers.add(new Consumer(entry.getKey(), storage, voltage, demand));
                totalDemand += demand;
            }
        }

        long delivered = Math.min(totalSupply, totalDemand);
        if (delivered <= 0) {
            this.cableCurrents.clear();
            this.measureLossAndOverload(world, networkVoltage);
            debugSummary(networkVoltage, totalSupply, totalDemand, 0);
            record(networkVoltage, totalSupply, totalDemand, 0);
            return;
        }

        // 4. 按需求比例分给负载，记下每台机器实际吃进的电流
        List<LoadFlow> flows = new ArrayList<>();
        long acceptedTotal = 0;
        for (Consumer consumer : consumers) {
            long share = consumer.demandMilliWatts() * delivered / totalDemand;
            long energy = share / 20L;
            if (energy <= 0) {
                continue;
            }
            long accepted = consumer.storage().insertEnergy(energy);
            if (DEBUG && this.debugTicks % 20 == 0) {
                LOGGER.info("[Energy] 负载 {} 分到 {} mJ / 吃进 {} mJ（需求 {} mW，{} V）",
                        consumer.pos().toShortString(), energy, accepted, consumer.demandMilliWatts(), consumer.voltage());
            }
            acceptedTotal += accepted;
            // 机器侧读数：机器自己的额定输入电压 × 电流（用户定的规则：真实输入按机器额定电压走）
            int machineCurrent = consumer.voltage() > 0
                    ? (int) Math.min(Integer.MAX_VALUE, accepted * 20L / consumer.voltage())
                    : 0;
            addInput(readings, consumer.pos(), consumer.voltage(), machineCurrent);
            // 线路侧电流：同一份功率在网络电压下的电流（升/降压不凭空造能量，万用表的功率才对得上）
            int cableCurrent = networkVoltage > 0
                    ? (int) Math.min(Integer.MAX_VALUE, accepted * 20L / networkVoltage)
                    : 0;
            if (cableCurrent > 0) {
                Set<BlockPos> cables = connectedCables(consumer.pos(), this.machines.get(consumer.pos()),
                        consumer.storage(), true);
                flows.add(new LoadFlow(new ArrayList<>(cables), cableCurrent));
            }
        }

        // 谁都没吃进去就一点电都不该扣（避免"分出去了但没人接收"的能量凭空消失）
        if (acceptedTotal <= 0) {
            this.cableCurrents.clear();
            this.measureLossAndOverload(world, networkVoltage);
            debugSummary(networkVoltage, totalSupply, totalDemand, 0);
            record(networkVoltage, totalSupply, totalDemand, 0);
            return;
        }

        // 5. 电源按"负载实际吃进去的能量 + 线路损耗"扣缓冲区（按可用功率比例分摊），
        //    并把实际输出记到界面上。只扣实际送出的部分，多出来的留在电源缓冲区里。
        long lossEnergy = this.lastLossMilliWatts / 20L;
        long toExtract = acceptedTotal + lossEnergy;
        for (Provider provider : providers) {
            long share = provider.availableMilliWatts() * delivered / totalSupply;
            long energy = delivered > 0 ? toExtract * share / delivered : 0;
            long taken = energy > 0 ? provider.storage().extractEnergy(energy) : 0;
            int current = networkVoltage > 0
                    ? (int) Math.min(Integer.MAX_VALUE, taken * 20L / networkVoltage)
                    : 0;
            addOutput(readings, provider.pos(), networkVoltage, current);
        }

        traceCableCurrents(world, providers, flows);
        debugSummary(networkVoltage, totalSupply, totalDemand, acceptedTotal);
        measureLossAndOverload(world, networkVoltage);
        record(networkVoltage, totalSupply, totalDemand, acceptedTotal * 20L);
    }

    /** 诊断日志：每 20 tick 打一行这张网的结算明细（`-Dmcdonalds.debug.energy=true`）。 */
    private void debugSummary(int voltage, long supply, long demand, long acceptedEnergy) {
        if (!DEBUG || this.debugTicks % 20 != 0) {
            return;
        }
        StringBuilder cables = new StringBuilder();
        this.cableCurrents.forEach((pos, current) ->
                cables.append(pos.toShortString()).append('=').append(current).append("mA "));
        LOGGER.info("[Energy] 供电={} mW 需求={} mW 实收={} mJ/tick 损耗={} mW 电压={}V 电缆: {}",
                supply, demand, acceptedEnergy, this.lastLossMilliWatts, voltage, cables);
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
     * 逐电源统计每根电缆上的电流。
     *
     * <p>做法：对<b>每一台电源</b>各跑一次 BFS 生成树，然后按可用功率比例，
     * 把每台负载吃到的电流拆成"这台电源承担的那一份"，沿它自己的父链累加。
     *
     * <p>为什么不能像以前那样"从负载回推到最近的电源"：那样多台电源并联时，
     * 只有离负载最近的那台电源的路径会被经过，其它电源的支线永远是 0 A
     * （表现为"新加的电池盒前面那根线没电"）。逐电源分摊才能让每台电源的支线
     * 显示它真正送出的那部分，主干也自然是所有电源之和。
     */
    private void traceCableCurrents(ServerWorld world, List<Provider> providers, List<LoadFlow> flows) {
        if (flows.isEmpty() || providers.isEmpty()) {
            return;
        }
        long totalAvailable = 0;
        for (Provider provider : providers) {
            totalAvailable += provider.availableMilliWatts();
        }
        if (totalAvailable <= 0) {
            return;
        }

        for (Provider provider : providers) {
            // 单源 BFS：从这台电源紧挨着的电缆出发，得到它到全网电缆的最短路径树
            Map<BlockPos, BlockPos> parent = new HashMap<>();
            Map<BlockPos, Integer> distance = new HashMap<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            for (Direction direction : Direction.values()) {
                BlockPos cable = provider.pos().offset(direction);
                if (this.cables.contains(cable) && distance.putIfAbsent(cable, 0) == null) {
                    queue.add(cable);
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

            // 这台电源按可用功率比例，承担每台负载的一部分电流，并沿自己的路径累加
            for (LoadFlow flow : flows) {
                long share = flow.currentMilliAmps() * provider.availableMilliWatts() / totalAvailable;
                if (share <= 0) {
                    continue;
                }
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
                    this.cableCurrents.merge(best, share, Long::sum);
                    best = parent.get(best);
                }
            }
        }
    }

    /** 往汇总表里累加"这台机器吃到的输入"。电压取多张网里最大的那个，电流直接相加。 */
    private static void addInput(Map<BlockPos, long[]> readings, BlockPos pos, int voltage, long currentMilliAmps) {
        long[] reading = readings.computeIfAbsent(pos, key -> new long[4]);
        if (voltage > reading[0]) {
            reading[0] = voltage;
        }
        reading[1] += currentMilliAmps;
    }

    /** 往汇总表里累加"这台机器给出的输出"。 */
    private static void addOutput(Map<BlockPos, long[]> readings, BlockPos pos, int voltage, long currentMilliAmps) {
        long[] reading = readings.computeIfAbsent(pos, key -> new long[4]);
        if (voltage > reading[2]) {
            reading[2] = voltage;
        }
        reading[3] += currentMilliAmps;
    }

    static Set<BlockPos> newCableSet() {
        return new LinkedHashSet<>();
    }

    static Map<BlockPos, Set<Direction>> newMachineMap() {
        return new LinkedHashMap<>();
    }
}
