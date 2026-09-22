package com.myachi.mcdonaldsmod.mixin;

import com.myachi.mcdonaldsmod.beacon.ModBeaconFeatures;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/**
 * Revokes creative flight once the beacon flight effect wears off.
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
	@Inject(method = "onStatusEffectsRemoved", at = @At("TAIL"))
	private void removeBeaconFlight(Collection<StatusEffectInstance> effects, CallbackInfo ci) {
		if (effects.stream().noneMatch(effect -> effect.getEffectType().value() == ModBeaconFeatures.FLIGHT_EFFECT)) {
			return;
		}

		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

		if (!player.isSpectator() && !player.isCreative()) {
			player.getAbilities().allowFlying = false;
			player.getAbilities().flying = false;
			player.sendAbilitiesUpdate();
		}
	}
}
