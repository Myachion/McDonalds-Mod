package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.energy.EnergyNetworks;
import com.myachi.mcdonaldsmod.energy.EnergyStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

/**
 * 用电机器方块实体的公共底座，实现的是用户定下的"缓冲区模式"。
 *
 * <h2>单位（全整数，避免浮点误差）</h2>
 * <ul>
 *     <li>电压 V；电流 mA；能量 mJ；功率 mW（= V × mA）</li>
 *     <li>一个 tick 是 1/20 秒，所以每 tick 的能量 = 功率 ÷ 20（用 {@link #milliJoulesPerTick}）</li>
 * </ul>
 *
 * <h2>缓冲区模式怎么工作</h2>
 * <ol>
 *     <li>电网每 tick 往缓冲区里灌电，灌多少受"额定输入电压/电流"限制（见 {@link EnergyStorage}）；</li>
 *     <li>机器自己干活时用 {@link #consumeEnergy(long)} 从缓冲区扣电，不够就干不了（由子类决定暂停还是重来）；</li>
 *     <li>额定值一般比实际耗电略大一点，这样能一边充电一边工作。</li>
 * </ol>
 *
 * <h2>子类要做的三件事</h2>
 * <ol>
 *     <li>构造器里把容量和额定值传给 super；</li>
 *     <li>实现 {@link #tickServer(ServerWorld)}（电网登记已经由基类做掉了）；</li>
 *     <li>需要界面就实现 {@code createMenu}（{@link NamedScreenHandlerFactory} 的方法）。</li>
 * </ol>
 *
 * <p>配套的方块请继承 {@link MachineBlock}，这样 tick 会自动转进来、正面贴图也会自动切换。
 */
public abstract class AbstractMachineBlockEntity extends BlockEntity
        implements EnergyStorage, NamedScreenHandlerFactory, Inventory {
    /** 缓冲区无上限（测试电池盒那种）。 */
    public static final long UNLIMITED = Long.MAX_VALUE;

    private final long capacityMilliJoules;
    private long storedEnergy;

    private int ratedInputVoltage;
    private int ratedInputCurrentMilliAmps;
    private int ratedOutputVoltage;
    private int ratedOutputCurrentMilliAmps;

    private int measuredInputVoltage;
    private int measuredInputCurrentMilliAmps;
    private int measuredOutputVoltage;
    private int measuredOutputCurrentMilliAmps;

    /** 机器自己的物品栏；没有物品栏的机器（纯机器类）就是 null。 */
    private MachineInventory inventory;

    /** 纯用电器：只有输入，没有输出。 */
    protected AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                         long capacityMilliJoules,
                                         int ratedInputVoltage, int ratedInputCurrentMilliAmps) {
        this(type, pos, state, capacityMilliJoules,
                ratedInputVoltage, ratedInputCurrentMilliAmps, 0, 0);
    }

    protected AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                         long capacityMilliJoules,
                                         int ratedInputVoltage, int ratedInputCurrentMilliAmps,
                                         int ratedOutputVoltage, int ratedOutputCurrentMilliAmps) {
        super(type, pos, state);
        this.capacityMilliJoules = capacityMilliJoules;
        this.ratedInputVoltage = ratedInputVoltage;
        this.ratedInputCurrentMilliAmps = ratedInputCurrentMilliAmps;
        this.ratedOutputVoltage = ratedOutputVoltage;
        this.ratedOutputCurrentMilliAmps = ratedOutputCurrentMilliAmps;
    }

    /** 功率(mW) 换算成每 tick 的能量(mJ)：96 W -> 4800 mJ/tick。 */
    public static long milliJoulesPerTick(long milliWatts) {
        return milliWatts / 20L;
    }

    // ------------------------------------------------------------------
    // 物品栏（子类在构造器里 createInventory(格数) 就有）
    // ------------------------------------------------------------------

    /**
     * 建一个固定格数的物品栏，并把 {@link Inventory} 接口转发给它。
     *
     * <p>转发很关键：漏斗、{@code /item replace block} 这类原版机制都靠"方块实体是不是 Inventory"来判断，
     * 只把 {@link MachineInventory} 交给界面用的话，这些机制会失效。
     */
    protected MachineInventory createInventory(int size) {
        this.inventory = new MachineInventory(this, size);
        return this.inventory;
    }

    /** 机器物品栏（没有建就是 null）。 */
    public MachineInventory getInventory() {
        return this.inventory;
    }

    private MachineInventory requireInventory() {
        if (this.inventory == null) {
            throw new UnsupportedOperationException("这台机器没有物品栏，先在构造器里调用 createInventory(size)");
        }
        return this.inventory;
    }

    @Override
    public int size() {
        return this.inventory == null ? 0 : this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory == null || this.inventory.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return requireInventory().getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return requireInventory().removeStack(slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return requireInventory().removeStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        requireInventory().setStack(slot, stack);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        if (this.inventory != null) {
            this.inventory.clear();
        }
    }

    // ------------------------------------------------------------------
    // 缓冲区
    // ------------------------------------------------------------------

    public long getCapacityMilliJoules() {
        return this.capacityMilliJoules;
    }

    public boolean isFull() {
        return this.storedEnergy >= this.capacityMilliJoules;
    }

    @Override
    public long getStoredEnergyMilliJoules() {
        return this.storedEnergy;
    }

    @Override
    public long insertEnergy(long millijoules) {
        if (millijoules <= 0) {
            return 0;
        }
        long room = this.capacityMilliJoules == UNLIMITED
                ? Long.MAX_VALUE - this.storedEnergy
                : Math.max(0L, this.capacityMilliJoules - this.storedEnergy);
        long accepted = Math.min(millijoules, room);
        if (accepted > 0) {
            this.storedEnergy += accepted;
            this.markDirty();
        }
        return accepted;
    }

    @Override
    public long extractEnergy(long millijoules) {
        if (millijoules <= 0) {
            return 0;
        }
        long taken = Math.min(millijoules, this.storedEnergy);
        if (taken > 0) {
            this.storedEnergy -= taken;
            this.markDirty();
        }
        return taken;
    }

    /** 直接加电，不看上限也不看额定值（给发电机/测试脚本用）。 */
    public void addEnergy(long millijoules) {
        if (millijoules <= 0) {
            return;
        }
        this.storedEnergy = millijoules > Long.MAX_VALUE - this.storedEnergy
                ? Long.MAX_VALUE
                : this.storedEnergy + millijoules;
        this.markDirty();
    }

    /** 机器自己耗电：缓冲区够这次用电就扣掉并返回 true，不够就返回 false（不扣）。 */
    protected boolean consumeEnergy(long millijoules) {
        if (millijoules <= 0) {
            return true;
        }
        if (this.storedEnergy < millijoules) {
            return false;
        }
        this.storedEnergy -= millijoules;
        this.markDirty();
        return true;
    }

    // ------------------------------------------------------------------
    // 额定值与实测读数
    // ------------------------------------------------------------------

    @Override
    public int getRatedInputVoltage() {
        return this.ratedInputVoltage;
    }

    @Override
    public int getRatedInputCurrent() {
        return this.ratedInputCurrentMilliAmps;
    }

    @Override
    public int getRatedOutputVoltage() {
        return this.ratedOutputVoltage;
    }

    @Override
    public int getRatedOutputCurrent() {
        return this.ratedOutputCurrentMilliAmps;
    }

    /** 额定值可调的机器（电池盒那种）在改动后调用。 */
    protected void setRatedInput(int voltage, int currentMilliAmps) {
        this.ratedInputVoltage = voltage;
        this.ratedInputCurrentMilliAmps = currentMilliAmps;
        this.markDirty();
    }

    protected void setRatedOutput(int voltage, int currentMilliAmps) {
        this.ratedOutputVoltage = voltage;
        this.ratedOutputCurrentMilliAmps = currentMilliAmps;
        this.markDirty();
    }

    @Override
    public void setMeasuredInput(int voltage, int currentMilliAmps) {
        this.measuredInputVoltage = voltage;
        this.measuredInputCurrentMilliAmps = currentMilliAmps;
    }

    @Override
    public void setMeasuredOutput(int voltage, int currentMilliAmps) {
        this.measuredOutputVoltage = voltage;
        this.measuredOutputCurrentMilliAmps = currentMilliAmps;
    }

    /** 界面显示用：电网结算出来的实际输入电压。 */
    public int getMeasuredInputVoltage() {
        return this.measuredInputVoltage;
    }

    /** 界面显示用：电网结算出来的实际输入电流（mA）。 */
    public int getMeasuredInputCurrent() {
        return this.measuredInputCurrentMilliAmps;
    }

    /** 界面显示用：实际输出电压。 */
    public int getMeasuredOutputVoltage() {
        return this.measuredOutputVoltage;
    }

    /** 界面显示用：实际输出电流（mA）。 */
    public int getMeasuredOutputCurrent() {
        return this.measuredOutputCurrentMilliAmps;
    }

    // ------------------------------------------------------------------
    // 每 tick
    // ------------------------------------------------------------------

    /**
     * 由 {@link MachineBlock} 每 tick 调用一次（客户端也会调，但只在服务端干活）。
     * 先把自己登记到电网（和电池盒一样的做法），再交给子类的 {@link #tickServer}。
     */
    public final void tickMachine() {
        if (this.world instanceof ServerWorld serverWorld) {
            EnergyNetworks.get(serverWorld).registerMachine(this.pos);
            this.tickServer(serverWorld);
        }
    }

    /** 子类的服务端每 tick 逻辑（电网登记已经在基类做完了）。 */
    protected abstract void tickServer(ServerWorld world);

    /** 正在工作/没工作：切换方块的 lit 状态（正面激活贴图）。 */
    protected void setActive(boolean active) {
        MachineBlock.setActive(this.world, this.pos, active);
    }

    // ------------------------------------------------------------------
    // 存档：缓冲区 + 额定值
    // ------------------------------------------------------------------

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.storedEnergy = Math.clamp(view.getLong("stored_energy", 0L), 0L, this.capacityMilliJoules);
        this.ratedInputVoltage = view.getInt("rated_input_voltage", this.ratedInputVoltage);
        this.ratedInputCurrentMilliAmps = view.getInt("rated_input_current", this.ratedInputCurrentMilliAmps);
        this.ratedOutputVoltage = view.getInt("rated_output_voltage", this.ratedOutputVoltage);
        this.ratedOutputCurrentMilliAmps = view.getInt("rated_output_current", this.ratedOutputCurrentMilliAmps);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putLong("stored_energy", this.storedEnergy);
        view.putInt("rated_input_voltage", this.ratedInputVoltage);
        view.putInt("rated_input_current", this.ratedInputCurrentMilliAmps);
        view.putInt("rated_output_voltage", this.ratedOutputVoltage);
        view.putInt("rated_output_current", this.ratedOutputCurrentMilliAmps);
    }
}
