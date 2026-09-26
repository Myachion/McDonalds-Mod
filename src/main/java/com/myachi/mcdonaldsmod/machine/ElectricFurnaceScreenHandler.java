package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

/**
 * 电炉界面：直接复用原版熔炉的布局与贴图（三个格子 + 火苗 + 进度箭头），
 * 只把输出格换成自己的版本 —— 原版输出格只在炉子方块实体上发经验，
 * 这里要在取出产物时把电炉攒的熔炼经验发给玩家。
 */
public class ElectricFurnaceScreenHandler extends FurnaceScreenHandler {
    /** 服务端用：绑定到电炉方块实体。 */
    public ElectricFurnaceScreenHandler(int syncId, PlayerInventory playerInventory, ElectricFurnaceBlockEntity entity) {
        super(syncId, playerInventory, entity, entity.getProperties());
        this.slots.set(ElectricFurnaceBlockEntity.SLOT_OUTPUT,
                new ElectricFurnaceOutputSlot(playerInventory.player, entity, ElectricFurnaceBlockEntity.SLOT_OUTPUT, 116, 35));
    }

    /** 客户端用：内容与属性由服务端同步过来。 */
    public ElectricFurnaceScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(syncId, playerInventory);
    }

    /**
     * 原版 {@link FurnaceScreenHandler} 的构造器会把类型写死成 {@code minecraft:furnace}，
     * 那样客户端会照熔炉的界面去开。这里改回自己的类型，客户端才会用
     * {@link com.myachi.mcdonaldsmod.client.ElectricFurnaceScreen}。
     */
    @Override
    public ScreenHandlerType<?> getType() {
        return ModScreenHandlers.ELECTRIC_FURNACE;
    }

    /** 输出格：不能放入，取出时结算经验。 */
    private static class ElectricFurnaceOutputSlot extends Slot {
        private final PlayerEntity player;
        private final ElectricFurnaceBlockEntity furnace;

        private ElectricFurnaceOutputSlot(PlayerEntity player, ElectricFurnaceBlockEntity inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.player = player;
            this.furnace = inventory;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            this.furnace.grantPendingExperience(this.player);
            super.onTakeItem(player, stack);
        }
    }
}
