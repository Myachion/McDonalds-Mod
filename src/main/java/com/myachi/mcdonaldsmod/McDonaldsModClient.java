package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.beacon.BeaconNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.render.BlockRenderLayer;

public class McDonaldsModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // ItemTooltipCallback is client-only; registering it from the common
        // initializer crashes dedicated servers.
        ModTooltips.itemTooltipInitializer();

        // 作物必须走镂空渲染层，否则贴图里的透明像素会被当成实心填充，整格变成一块面板。
        // 原版小麦/胡萝卜/马铃薯/甜菜/瓶子草同样都是 CUTOUT。
        BlockRenderLayerMap.putBlocks(BlockRenderLayer.CUTOUT,
                ModBlocks.TOMATO_CROP,
                ModBlocks.ONION_CROP,
                ModBlocks.CORN_CROP,
                ModBlocks.BLUEBERRY_BUSH);

        // Declaring a receiver on this channel tells the server this client knows the
        // mod, which is what allows it to safely receive the beacon flight effect.
        ClientPlayNetworking.registerGlobalReceiver(BeaconNetwork.BEACON_CAPABILITY_ID, (payload, context) -> {
        });

        McDonaldsMod.LOGGER.info("Registry ModBeaconFeatures client!");
    }
}
