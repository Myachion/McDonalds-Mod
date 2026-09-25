package com.myachi.mcdonaldsmod;

import net.fabricmc.fabric.api.registry.FuelRegistryEvents;

public class ModFuels {
    public static void modFuelRegister() {
        FuelRegistryEvents.BUILD.register(((builder, context) -> {
            builder.add(ModItems.VEGETABLE_OIL,1200);
            builder.add(ModItems.COAL_DUST, 2000);
            // 和原版煤炭块一样：16000 tick（可烧炼 80 个物品）
            builder.add(ModBlocks.CHARCOAL_BLOCK, 16000);
        }));
    }

    public static void initializeModFuels() {
        modFuelRegister();
        McDonaldsMod.LOGGER.info("Registry ModFuels!");
    }
}
