package com.myachi.mcdonaldsmod;

import net.minecraft.item.Item;


import net.minecraft.item.Items;
import net.minecraft.item.equipment.trim.ArmorTrimMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.resource.*;

import java.util.function.Function;

public class TestItems {


    public static final Item STEEL_INGOT = register("steel_ingot",Item::new,new Item.Settings());

    private TestItems() {}

    public static Item register(String path, Function<Item.Settings,Item> factory,Item.Settings settings) {
        final RegistryKey<Item> registerKey = RegistryKey.of(RegistryKeys.ITEM,Identifier.of(McDonaldsMod.MOD_ID,path));
        return Items.register(registerKey,factory,settings);
    }

    public static void initializeMod() {
        McDonaldsMod.LOGGER.info("Register TestItems!");
    }


}
