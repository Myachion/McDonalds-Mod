package com.myachi.mcdonaldsmod;

import net.fabricmc.fabric.api.registry.FuelRegistryEvents;

public class ModFuels {
    public static void modFuelRegister() {
        FuelRegistryEvents.BUILD.register(((builder, context) -> {
            builder.add(ModItems.SOYBEAN_OIL,1200);
            builder.add(ModItems.COAL_DUST, 2000);
        }));
    }

    public static void initializeModFuels() {
        modFuelRegister();
        McDonaldsMod.LOGGER.info("Registry ModFuels!");
    }
}
