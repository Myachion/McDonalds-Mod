package com.myachi.mcdonaldsmod.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Big Beacons makes the beacon Luck effect strengthen Fortune ore drops.
 * The tool's own Fortune level and the Luck level are added together.
 */
@Mixin(ApplyBonusLootFunction.class)
public abstract class ApplyBonusLootFunctionMixin {
	@Shadow
	@Final
	private RegistryEntry<Enchantment> enchantment;

	@Inject(method = "process", at = @At("HEAD"), cancellable = true)
	private void addLuckToFortune(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
		if (!this.enchantment.matchesKey(Enchantments.FORTUNE)) {
			return;
		}

		Entity entity = context.get(LootContextParameters.THIS_ENTITY);

		if (!(entity instanceof LivingEntity living)) {
			return;
		}

		StatusEffectInstance luck = living.getStatusEffect(StatusEffects.LUCK);

		if (luck == null) {
			return;
		}

		ItemStack tool = context.get(LootContextParameters.TOOL);
		int fortune = tool == null ? 0 : EnchantmentHelper.getLevel(this.enchantment, tool);
		int level = fortune + luck.getAmplifier() + 1;
		int bonus = context.getRandom().nextInt(level + 2) - 1;

		if (bonus < 0) {
			bonus = 0;
		}

		stack.setCount(stack.getCount() * (bonus + 1));
		cir.setReturnValue(stack);
	}
}
