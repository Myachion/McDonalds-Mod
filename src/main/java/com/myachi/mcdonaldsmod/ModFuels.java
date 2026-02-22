package com.myachi.mcdonaldsmod;

import net.fabricmc.fabric.api.registry.FuelRegistryEvents;

public class ModFuels {
    public static void modFuelRegister() {
        FuelRegistryEvents.BUILD.register(((builder, context) -> {
            builder.add(ModItems.SOYBEAN_OIL,1200);
        }));
    }

    public static void initializeModFuels() {
        modFuelRegister();
        McDonaldsMod.LOGGER.info("Registry ModFuels!");
    }
}
