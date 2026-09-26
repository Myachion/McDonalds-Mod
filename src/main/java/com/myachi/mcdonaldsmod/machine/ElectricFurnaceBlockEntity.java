package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * 电炉方块实体：{@link AbstractMachineBlockEntity} 的第一个正式用例。
 *
 * <p>参数：额定输入 32 V / 4 A，缓冲区 2.5 kJ；烧制时按 96 W（4800 mJ/tick）从缓冲区取电，
 * 速度和高炉一致（100 tick 一个物品）。能烧的东西和熔炉完全一样（{@link RecipeType#SMELTING}）。
 *
 * <p>缓冲区、额定值、电网登记、存档都在基类里；这里只写"怎么烧"和界面属性。
 * 缓冲区不够一个 tick 的电就暂停烧制，进度不倒退。
 */
public class ElectricFurnaceBlockEntity extends AbstractMachineBlockEntity {
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
    /** 烧制功率 96 W = 4800 mJ/tick。 */
    public static final long DRAW_PER_TICK_MILLI_JOULES = milliJoulesPerTick(96_000L);
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

    private final MachineInventory inventory = createInventory(INVENTORY_SIZE);
    /** 当前物品已经烧了多少 tick。 */
    private int cookTicks;
    /** 攒着还没发的熔炼经验（毫经验，取出产物时一起给玩家）。 */
    private int pendingExperienceMilli;

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_FLAME -> storedJoules();
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
        super(ModBlockEntities.ELECTRIC_FURNACE, pos, state,
                CAPACITY_MILLI_JOULES, RATED_INPUT_VOLTAGE, RATED_INPUT_CURRENT_MILLI_AMPS);
    }

    public PropertyDelegate getProperties() {
        return this.properties;
    }

    /** 缓冲区电量，单位 J（取整）；界面闪电比例和悬停提示都用它。 */
    public int storedJoules() {
        return (int) Math.clamp(this.getStoredEnergyMilliJoules() / 1000L, 0L, (long) CAPACITY_JOULES);
    }

    // ------------------------------------------------------------------
    // 每 tick：烧制
    // ------------------------------------------------------------------

    @Override
    protected void tickServer(ServerWorld world) {
        ItemStack input = this.inventory.getStack(SLOT_INPUT);
        Optional<RecipeEntry<SmeltingRecipe>> match = input.isEmpty()
                ? Optional.empty()
                : world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(input), world);
        // 1.21 起配方结果只能通过 craft 拿，这里每 tick 取一次（有内部缓存，开销很小）
        ItemStack result = match
                .map(entry -> entry.value().craft(new SingleStackRecipeInput(input), world.getRegistryManager()))
                .orElse(ItemStack.EMPTY);

        boolean active = false;
        if (!result.isEmpty() && canAcceptOutput(result)) {
            // 从缓冲区扣这一 tick 的电；不够就原地暂停，进度不倒退
            if (consumeEnergy(DRAW_PER_TICK_MILLI_JOULES)) {
                this.cookTicks++;
                active = true;
                if (this.cookTicks >= COOK_TICKS) {
                    this.cookTicks = 0;
                    craft(match.orElseThrow(), input, result);
                }
            }
        } else if (this.cookTicks != 0) {
            this.cookTicks = 0;
        }
        setActive(active);
    }

    private boolean canAcceptOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = this.inventory.getStack(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.areItemsAndComponentsEqual(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxCount();
    }

    private void craft(RecipeEntry<SmeltingRecipe> recipe, ItemStack input, ItemStack result) {
        ItemStack output = this.inventory.getStack(SLOT_OUTPUT);
        if (output.isEmpty()) {
            this.inventory.setStack(SLOT_OUTPUT, result.copy());
        } else {
            output.increment(result.getCount());
        }
        input.decrement(1);
        this.pendingExperienceMilli += Math.round(recipe.value().getExperience() * 1000.0F);
        this.markDirty();
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
    // 存档：库存 + 烧制进度（缓冲区与额定值在基类里）
    // ------------------------------------------------------------------

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, this.inventory.getStacks());
        this.cookTicks = Math.clamp(view.getInt("cook_ticks", 0), 0, COOK_TICKS);
        this.pendingExperienceMilli = Math.max(0, view.getInt("pending_experience_milli", 0));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory.getStacks());
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
