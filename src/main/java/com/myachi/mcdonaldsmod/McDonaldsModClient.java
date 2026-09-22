package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.beacon.BeaconNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class McDonaldsModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Declaring a receiver on this channel tells the server this client knows the
        // mod, which is what allows it to safely receive the beacon flight effect.
        ClientPlayNetworking.registerGlobalReceiver(BeaconNetwork.BEACON_CAPABILITY_ID, (payload, context) -> {
        });

        McDonaldsMod.LOGGER.info("Registry ModBeaconFeatures client!");
    }
}
