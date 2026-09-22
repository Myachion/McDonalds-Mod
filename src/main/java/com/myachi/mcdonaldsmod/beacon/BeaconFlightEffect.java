package com.myachi.mcdonaldsmod.beacon;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Marker effect that keeps {@code allowFlying} enabled while it is active.
 * Revoking the ability happens in {@code ServerPlayerEntityMixin} as soon as
 * the effect wears off.
 */
public class BeaconFlightEffect extends StatusEffect {
	public BeaconFlightEffect() {
		super(StatusEffectCategory.BENEFICIAL, 0x30F8FF);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return true;
	}

	@Override
	public void onApplied(LivingEntity entity, int amplifier) {
		allowFlight(entity);
	}

	@Override
	public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
		allowFlight(entity);
		return true;
	}

	private static void allowFlight(LivingEntity entity) {
		if (entity instanceof PlayerEntity player) {
			player.getAbilities().allowFlying = true;
			player.sendAbilitiesUpdate();
		}
	}
}
