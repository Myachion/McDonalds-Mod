package com.myachi.mcdonaldsmod;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModItemTags {

    public static final TagKey<Item> MORTAR = of("mortar");

    private static TagKey<Item> of(String id) {
        return TagKey.of(RegistryKeys.ITEM, Identifier.of(McDonaldsMod.MOD_ID,id));
    }
    public static void initializeModItemTags() {
        McDonaldsMod.LOGGER.info("Registry ModItemTags!");
    }

}
