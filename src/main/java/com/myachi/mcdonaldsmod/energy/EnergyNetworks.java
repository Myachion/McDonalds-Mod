package com.myachi.mcdonaldsmod.energy;

import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;

/** 每个世界一个 {@link EnergyNetworkManager} 的查找表。 */
public final class EnergyNetworks {
    private static final Map<ServerWorld, EnergyNetworkManager> MANAGERS = new HashMap<>();

    private EnergyNetworks() {
    }

    public static EnergyNetworkManager get(ServerWorld world) {
        return MANAGERS.computeIfAbsent(world, EnergyNetworkManager::new);
    }
}
