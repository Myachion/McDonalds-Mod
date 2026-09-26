package com.myachi.mcdonaldsmod.machine;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * 机器的通用物品栏：固定格数 + 自动与方块实体联动（改动即 {@code markDirty()}）。
 *
 * <p>用法：
 * <pre>{@code
 * private final MachineInventory inventory = new MachineInventory(this, 3);
 *
 * // 存档
 * Inventories.readData(view, inventory.getStacks());
 * Inventories.writeData(view, inventory.getStacks());
 * }</pre>
 *
 * <p>需要限制某个格子能不能放东西时，在自己的方块实体里判断（例如输出格
 * {@code canInsert} 用界面里的 Slot 子类拦），这个类只负责存。
 */
public class MachineInventory implements Inventory {
    private final BlockEntity owner;
    private final DefaultedList<ItemStack> stacks;

    public MachineInventory(BlockEntity owner, int size) {
        this.owner = owner;
        this.stacks = DefaultedList.ofSize(size, ItemStack.EMPTY);
    }

    /** 存档/读档用的原始列表。 */
    public DefaultedList<ItemStack> getStacks() {
        return this.stacks;
    }

    @Override
    public int size() {
        return this.stacks.size();
    }

    @Override
    public boolean isEmpty() {
        return this.stacks.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.stacks.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack removed = Inventories.splitStack(this.stacks, slot, amount);
        if (!removed.isEmpty()) {
            this.markDirty();
        }
        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.stacks, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.stacks.set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        this.markDirty();
    }

    @Override
    public void markDirty() {
        this.owner.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this.owner, player);
    }

    @Override
    public void clear() {
        this.stacks.clear();
    }
}
