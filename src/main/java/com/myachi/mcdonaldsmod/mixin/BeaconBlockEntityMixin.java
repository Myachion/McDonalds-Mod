package com.myachi.mcdonaldsmod.mixin;

import com.myachi.mcdonaldsmod.beacon.BeaconNetwork;
import com.myachi.mcdonaldsmod.beacon.ModBeaconFeatures;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

/**
 * Big Beacons core: raises the pyramid cap from 4 to 16 levels, unlocks eight
 * effect tiers and adds the level III and flight rewards.
 */
@Mixin(BeaconBlockEntity.class)
public class BeaconBlockEntityMixin {

	/** Swaps the vanilla four tier table for the eight tier one. */
	@Redirect(
			method = "<clinit>",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;"
			)
	)
	private static List<List<RegistryEntry<StatusEffect>>> bigBeaconEffects(Object first, Object second, Object third, Object fourth) {
		// Built inline rather than stored in a @Unique field: a field initialiser would be
		// merged into this class's static initialiser without any ordering guarantee.
		return List.of(
				List.of(StatusEffects.SPEED, StatusEffects.HASTE),
				List.of(StatusEffects.RESISTANCE, StatusEffects.JUMP_BOOST),
				List.of(StatusEffects.STRENGTH),
				List.of(StatusEffects.REGENERATION),
				List.of(StatusEffects.FIRE_RESISTANCE),
				List.of(StatusEffects.SATURATION),
				List.of(StatusEffects.ABSORPTION),
				List.of(StatusEffects.LUCK)
		);
	}

	/** Allows pyramids up to {@link ModBeaconFeatures#MAX_LEVEL} levels. */
	@ModifyConstant(method = "updateLevel", constant = @Constant(intValue = 4, ordinal = 0))
	private static int raiseBeaconLevelCap(int original) {
		return ModBeaconFeatures.MAX_LEVEL;
	}

	/** Adds the level III upgrade and the level 16 flight buff. */
	@Inject(method = "applyPlayerEffects", at = @At("TAIL"))
	private static void applyHighLevelBeaconEffects(
			World world,
			BlockPos pos,
			int beaconLevel,
			RegistryEntry<StatusEffect> primaryEffect,
			RegistryEntry<StatusEffect> secondaryEffect,
			CallbackInfo ci
	) {
		if (!(world instanceof ServerWorld serverWorld) || primaryEffect == null) {
			return;
		}

		boolean triplePower = beaconLevel >= ModBeaconFeatures.LEVEL_THREE_REQUIREMENT
				&& Objects.equals(primaryEffect, secondaryEffect);
		boolean grantFlight = beaconLevel >= ModBeaconFeatures.FLIGHT_REQUIREMENT
				&& serverWorld.getGameRules().getValue(ModBeaconFeatures.BEACON_FLIGHT);

		if (!triplePower && !grantFlight) {
			return;
		}

		double range = beaconLevel * 10 + 10;
		int duration = (9 + beaconLevel * 2) * 20;
		Box area = new Box(pos).expand(range).stretch(0.0, world.getHeight(), 0.0);

		for (PlayerEntity player : serverWorld.getNonSpectatingEntities(PlayerEntity.class, area)) {
			if (triplePower) {
				player.addStatusEffect(new StatusEffectInstance(primaryEffect, duration, 2, true, true));
			}

			if (grantFlight && knowsFlightEffect(player)) {
				player.addStatusEffect(new StatusEffectInstance(
						ModBeaconFeatures.flightEffectEntry(), duration, 0, true, false));
			}
		}
	}

	/**
	 * Clients that do not know the flight effect are disconnected when they receive
	 * it, so it only goes to players whose client announced the mod.
	 */
	@Unique
	private static boolean knowsFlightEffect(PlayerEntity player) {
		return !(player instanceof ServerPlayerEntity serverPlayer)
				|| ServerPlayNetworking.canSend(serverPlayer, BeaconNetwork.BEACON_CAPABILITY_ID);
	}
}
