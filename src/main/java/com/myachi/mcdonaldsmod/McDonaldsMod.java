package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.beacon.ModBeaconFeatures;
import com.myachi.mcdonaldsmod.energy.EnergyNetworks;
import com.myachi.mcdonaldsmod.energy.EnergyConfig;
import com.myachi.mcdonaldsmod.material.ModMaterials;
import com.myachi.mcdonaldsmod.worldgen.ModOreGeneration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McDonaldsMod implements ModInitializer {
	public static final String MOD_ID = "mcdonalds-mod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
        // 材料族要在其它注册之前跑：ModItemGroups 会遍历 ModMaterials
        ModMaterials.initialize();
        ModItems.initializeMod();
        ModItemGroups.initializeModItemGroups();
        ModBlocks.initializeModBlocks();
        ModBlockEntities.initializeModBlockEntities();
        ModScreenHandlers.initializeModScreenHandlers();
        ModFuels.initializeModFuels();
        ModBlockTags.initializeModBlockTags();
        ModItemTags.initializeModItemTags();
        ModSounds.initializeModSounds();
        ModBeaconFeatures.initialize();
        ModOreGeneration.initialize();

        // 电网：每 tick 结算一次（网络结构只在方块增删时重算）
        EnergyConfig.load();
        ServerTickEvents.END_WORLD_TICK.register(world -> EnergyNetworks.get(world).tick());

		LOGGER.info("Hello Fabric world!");


	}
}
