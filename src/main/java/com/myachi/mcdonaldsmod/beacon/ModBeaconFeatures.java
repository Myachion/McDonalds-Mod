package com.myachi.mcdonaldsmod.beacon;

import com.myachi.mcdonaldsmod.McDonaldsMod;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleCategory;

/**
 * Big Beacons style beacon progression, ported to 1.21.11.
 *
 * <p>Vanilla beacons stop at level 4. This module raises the cap to 16, unlocks a
 * new effect tier for every beacon level, upgrades the primary effect to level III
 * once the beacon reaches level 10 and grants creative flight at level 16.
 */
public final class ModBeaconFeatures {
	/** Highest pyramid level vanilla knows about. */
	public static final int VANILLA_MAX_LEVEL = 4;
	/** Highest pyramid level this mod accepts, a full 16 layer pyramid. */
	public static final int MAX_LEVEL = 16;
	/** Beacon level at which a matched primary and secondary become level III. */
	public static final int LEVEL_THREE_REQUIREMENT = 10;
	/** Beacon level at which the flight effect is handed out. */
	public static final int FLIGHT_REQUIREMENT = 16;

	/** Whether a full beacon may hand out creative flight. */
	public static final GameRule<Boolean> BEACON_FLIGHT = GameRuleBuilder
			.forBoolean(true)
			.category(GameRuleCategory.PLAYER)
			.buildAndRegister(Identifier.of(McDonaldsMod.MOD_ID, "beacon_flight"));

	public static final StatusEffect FLIGHT_EFFECT = new BeaconFlightEffect();

	private static RegistryEntry<StatusEffect> flightEffectEntry;

	private ModBeaconFeatures() {
	}

	/** The registered entry for {@link #FLIGHT_EFFECT}. */
	public static RegistryEntry<StatusEffect> flightEffectEntry() {
		if (flightEffectEntry == null) {
			flightEffectEntry = Registries.STATUS_EFFECT.getEntry(FLIGHT_EFFECT);
		}

		return flightEffectEntry;
	}

	public static void initialize() {
		Registry.register(Registries.STATUS_EFFECT, Identifier.of(McDonaldsMod.MOD_ID, "flight"), FLIGHT_EFFECT);
		BeaconNetwork.initialize();
		McDonaldsMod.LOGGER.info("Registry ModBeaconFeatures!");
	}
}
