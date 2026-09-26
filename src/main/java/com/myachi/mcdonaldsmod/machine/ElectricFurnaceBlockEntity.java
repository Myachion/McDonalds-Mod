package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModBlockEntities;
import com.myachi.mcdonaldsmod.energy.EnergyNetworks;
import com.myachi.mcdonaldsmod.energy.EnergyStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * 电炉方块实体。
 *
 * <p>参数（用户定的）：额定输入 32 V / 4 A，缓冲区 2.5 kJ；烧制时按 96 W 从缓冲区取电，
 * 速度和高炉一致（100 tick 一个物品）。能烧的东西和熔炉完全一样（{@link RecipeType#SMELTING}）。
 *
 * <p>取电方式是"缓冲区模式"：电网往缓冲区里灌电（受额定电压/电流限制），
 * 电炉自己在烧制时按 96 W 从缓冲区扣。缓冲区不够一个 tick 的电就暂停烧制，进度不倒退。
 */
public class ElectricFurnaceBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, EnergyStorage, Inventory {
    public static final int SLOT_INPUT = 0;
    /** 和熔炉界面保持一致的中间格，暂时没用（界面以后会换成专属的）。 */
    public static final int SLOT_SPARE = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int INVENTORY_SIZE = 3;

    /** 缓冲区 2.5 kJ。 */
    public static final long CAPACITY_MILLI_JOULES = 2_500_000L;
    /** 额定输入 32 V / 4 A —— 额定功率 128 W，比耗电 96 W 略大，够一边充电一边烧。 */
    public static final int RATED_INPUT_VOLTAGE = 32;
    public static final int RATED_INPUT_CURRENT_MILLI_AMPS = 4_000;
    /** 烧制功率 96 W：一个 tick 是 1/20 秒，所以 96 W ÷ 20 = 4800 mJ。 */
    public static final long DRAW_PER_TICK_MILLI_JOULES = 4_800L;
    /** 和高炉同速：100 tick = 5 秒一个物品。 */
    public static final int COOK_TICKS = 100;

    /*
     * 界面属性（沿用原版熔炉的四个字段：火苗当前/满值、进度当前/满值）。
     * "火苗"那两个字段直接放缓冲区电量，单位 J —— 满值就是容量 2500 J，
     * 这样界面上的闪电比例和鼠标悬停提示的数字用的是同一份数据。
     */
    public static final int PROPERTY_FLAME = 0;
    public static final int PROPERTY_FLAME_TOTAL = 1;
    public static final int PROPERTY_COOK = 2;
    public static final int PROPERTY_COOK_TOTAL = 3;
    public static final int PROPERTY_COUNT = 4;
    /** 缓冲区容量（J），界面显示用。 */
    public static final int CAPACITY_JOULES = (int) (CAPACITY_MILLI_JOULES / 1000L);

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    /** 缓冲区里的能量（mJ）。 */
    private long storedEnergy;
    /** 当前物品已经烧了多少 tick。 */
    private int cookTicks;
    /** 攒着还没发的熔炼经验（毫经验，取出产物时一起给玩家）。 */
    private int pendingExperienceMilli;

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_FLAME -> flameTicks();
                case PROPERTY_FLAME_TOTAL -> CAPACITY_JOULES;
                case PROPERTY_COOK -> ElectricFurnaceBlockEntity.this.cookTicks;
                case PROPERTY_COOK_TOTAL -> COOK_TICKS;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 都是服务端算、客户端显示，没有需要客户端回写的字段
        }

        @Override
        public int size() {
            return PROPERTY_COUNT;
        }
    };

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE, pos, state);
    }

    public PropertyDelegate getProperties() {
        return this.properties;
    }

    /** 缓冲区电量，单位 J（取整；界面闪电的比例和悬停提示都用它）。 */
    private int flameTicks() {
        return (int) Math.clamp(this.storedEnergy / 1000L, 0L, (long) CAPACITY_JOULES);
    }

    // ------------------------------------------------------------------
    // 每 tick：登记到电网 + 烧制
    // ------------------------------------------------------------------

    public void tick() {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }
        // 让电网知道这里有个负载（和电池盒一样，每 tick 登记一次）
        EnergyNetworks.get(serverWorld).registerMachine(this.pos);
        tickSmelting(serverWorld);
    }

    private void tickSmelting(ServerWorld world) {
        ItemStack input = this.inventory.get(SLOT_INPUT);
        Optional<RecipeEntry<SmeltingRecipe>> match = input.isEmpty()
                ? Optional.empty()
                : world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(input), world);
        // 1.21 起配方结果只能通过 craft 拿，这里每 tick 取一次（有内部缓存，开销很小）
        ItemStack result = match
                .map(entry -> entry.value().craft(new SingleStackRecipeInput(input), world.getRegistryManager()))
                .orElse(ItemStack.EMPTY);

        boolean active = false;
        if (!result.isEmpty() && canAcceptOutput(result)) {
            if (this.storedEnergy >= DRAW_PER_TICK_MILLI_JOULES) {
                extractEnergy(DRAW_PER_TICK_MILLI_JOULES);
                this.cookTicks++;
                active = true;
                if (this.cookTicks >= COOK_TICKS) {
                    this.cookTicks = 0;
                    craft(match.orElseThrow(), input, result);
                }
            }
            // 缓冲区电量不够：原地暂停，进度不倒退，等充上电继续
        } else if (this.cookTicks != 0) {
            this.cookTicks = 0;
        }
        setActive(active);
    }

    private boolean canAcceptOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = this.inventory.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.areItemsAndComponentsEqual(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxCount();
    }

    private void craft(RecipeEntry<SmeltingRecipe> recipe, ItemStack input, ItemStack result) {
        ItemStack output = this.inventory.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            this.inventory.set(SLOT_OUTPUT, result.copy());
        } else {
            output.increment(result.getCount());
        }
        input.decrement(1);
        this.pendingExperienceMilli += Math.round(recipe.value().getExperience() * 1000.0F);
        this.markDirty();
    }

    /** 正在烧制时点亮 {@link ElectricFurnaceBlock#LIT}，正面换激活材质。 */
    private void setActive(boolean active) {
        if (this.world == null) {
            return;
        }
        BlockState state = this.world.getBlockState(this.pos);
        if (state.getBlock() instanceof ElectricFurnaceBlock && state.get(ElectricFurnaceBlock.LIT) != active) {
            this.world.setBlockState(this.pos, state.with(ElectricFurnaceBlock.LIT, active), Block.NOTIFY_LISTENERS);
        }
    }

    /** 取出产物时把攒下的熔炼经验给玩家（和原版炉子一致）。 */
    public void grantPendingExperience(PlayerEntity player) {
        int whole = this.pendingExperienceMilli / 1000;
        if (whole <= 0) {
            return;
        }
        this.pendingExperienceMilli -= whole * 1000;
        player.addExperience(whole);
        this.markDirty();
    }

    // ------------------------------------------------------------------
    // 电网接口
    // ------------------------------------------------------------------

    @Override
    public long getStoredEnergyMilliJoules() {
        return this.storedEnergy;
    }

    @Override
    public long insertEnergy(long millijoules) {
        if (millijoules <= 0) {
            return 0;
        }
        long room = CAPACITY_MILLI_JOULES - this.storedEnergy;
        long accepted = Math.min(millijoules, Math.max(0L, room));
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

    @Override
    public int getRatedInputVoltage() {
        return RATED_INPUT_VOLTAGE;
    }

    @Override
    public int getRatedInputCurrent() {
        return RATED_INPUT_CURRENT_MILLI_AMPS;
    }

    /** 纯用电器：没有输出能力。 */
    @Override
    public int getRatedOutputVoltage() {
        return 0;
    }

    @Override
    public int getRatedOutputCurrent() {
        return 0;
    }

    // ------------------------------------------------------------------
    // 容器
    // ------------------------------------------------------------------

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = Inventories.splitStack(this.inventory, slot, amount);
        if (!stack.isEmpty()) {
            this.markDirty();
        }
        return stack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        this.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    // ------------------------------------------------------------------
    // 存档
    // ------------------------------------------------------------------

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, this.inventory);
        this.storedEnergy = Math.clamp(view.getLong("stored_energy", 0L), 0L, CAPACITY_MILLI_JOULES);
        this.cookTicks = Math.clamp(view.getInt("cook_ticks", 0), 0, COOK_TICKS);
        this.pendingExperienceMilli = Math.max(0, view.getInt("pending_experience_milli", 0));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        view.putLong("stored_energy", this.storedEnergy);
        view.putInt("cook_ticks", this.cookTicks);
        view.putInt("pending_experience_milli", this.pendingExperienceMilli);
    }

    // ------------------------------------------------------------------
    // 界面
    // ------------------------------------------------------------------

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ElectricFurnaceScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.mcdonalds-mod.electric_furnace");
    }
}
