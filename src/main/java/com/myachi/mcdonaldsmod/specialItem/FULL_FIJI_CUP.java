package com.myachi.mcdonaldsmod.specialItem;

import com.myachi.mcdonaldsmod.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class FULL_FIJI_CUP extends Item {

    public FULL_FIJI_CUP(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {

        super.finishUsing(stack, world, user);

        if (!world.isClient() && user instanceof PlayerEntity player) {

            // 吃完后给玩家一个物品A
            ItemStack newItem = new ItemStack(ModItems.FIJI_CUP);

            if (!player.getInventory().insertStack(newItem)) {
                player.dropItem(newItem, false);
            }
        }

        return stack;
    }
}