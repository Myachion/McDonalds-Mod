package com.myachi.mcdonaldsmod.mixin;

import com.myachi.mcdonaldsmod.beacon.ModBeaconFeatures;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Hides the marker flight effect from the effect list, matching Big Beacons.
 */
@Mixin(StatusEffectsDisplay.class)
public class StatusEffectsDisplayMixin {
	@ModifyVariable(method = "drawStatusEffects", at = @At("HEAD"), argsOnly = true, index = 2)
	private Collection<StatusEffectInstance> hideBeaconFlight(Collection<StatusEffectInstance> effects) {
		List<StatusEffectInstance> visible = new ArrayList<>(effects.size());

		for (StatusEffectInstance effect : effects) {
			if (effect.getEffectType().value() != ModBeaconFeatures.FLIGHT_EFFECT) {
				visible.add(effect);
			}
		}

		return visible;
	}
}
