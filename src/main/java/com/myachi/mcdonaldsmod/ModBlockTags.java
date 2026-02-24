package com.myachi.mcdonaldsmod;

import net.fabricmc.fabric.api.tag.FabricTagKey;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModBlockTags {
    public static final TagKey<Block> MACHINE = of("machine");


    private static TagKey<Block> of(String id) {
        return TagKey.of(RegistryKeys.BLOCK,Identifier.of(McDonaldsMod.MOD_ID,id));
    }

    public static void initializeModBlockTags() {
        McDonaldsMod.LOGGER.info("Registry ModBlocksTags!");
    }


}
