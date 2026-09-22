package com.myachi.mcdonaldsmod.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Big Beacons makes the beacon Luck effect improve Looting, mirroring how
 * vanilla Luck already improves other loot rolls.
 */
@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {
	@Inject(method = "getEquipmentLevel", at = @At("RETURN"), cancellable = true)
	private static void addLuckToLooting(
			RegistryEntry<Enchantment> enchantment,
			LivingEntity entity,
			CallbackInfoReturnable<Integer> cir
	) {
		if (!enchantment.matchesKey(Enchantments.LOOTING)) {
			return;
		}

		StatusEffectInstance luck = entity.getStatusEffect(StatusEffects.LUCK);

		if (luck == null) {
			return;
		}

		cir.setReturnValue(cir.getReturnValueI() + luck.getAmplifier() + 1);
	}
}
