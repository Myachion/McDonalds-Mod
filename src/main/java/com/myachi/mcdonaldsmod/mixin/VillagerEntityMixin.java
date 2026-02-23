package com.myachi.mcdonaldsmod.mixin;

import com.myachi.mcdonaldsmod.ModItems;
import com.myachi.mcdonaldsmod.ModSounds;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {
    @Inject(
            method = "interactMob",
            at = @At("HEAD"),
            cancellable = true
    )
    private void interceptInteraction(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = player.getStackInHand(hand);

        if (player.isSneaking() && stack.getItem() == ModItems.FIJI_CUP) {

            stack.decrement(1);
            ItemStack newStack = new ItemStack(ModItems.FULL_FIJI_CUP);
            if (!player.getInventory().insertStack(newStack)) {
                player.dropItem(newStack, false);
            }
            player.playSound(ModSounds.VILLAGER_MOAN, 1.0f, 1.0f);

            // 返回 SUCCESS，阻止村民打开交易界面
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }


}
