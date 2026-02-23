package com.myachi.mcdonaldsmod.specialItem;

import com.myachi.mcdonaldsmod.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class FIJI_CUP extends Item {
    public FIJI_CUP(Settings settings) {
        super(settings);
    }
    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!user.getEntityWorld().isClient()) {
            if (user.isSneaking()  && entity instanceof VillagerEntity) {
                stack.decrement(1);
                ItemStack itemB = new ItemStack(ModItems.FULL_FIJI_CUP);
                if (stack.isEmpty()) {
                    user.setStackInHand(hand, itemB);
                } else {
                    user.getInventory().insertStack(itemB);
                }
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

}
