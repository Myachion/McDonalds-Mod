package com.myachi.mcdonaldsmod;

import net.minecraft.item.Item;
import net.minecraft.item.equipment.trim.ArmorTrimMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class TestItems {
    public static final Item STEEL_INGOT = registerItems("steel_ingot",new Item(new Item.Settings()));

    private static Item registerItems(String id, Item item) {
        //return Registry.register(Registries.ITEM, RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(McDonaldsMod.MOD_ID,id)),item);
        return Registry.register(Registries.ITEM,Identifier.of(McDonaldsMod.MOD_ID,id),item);
    }
    public static void registerTestItems(){
        McDonaldsMod.LOGGER.info("Register TestItems!");
    }

}
