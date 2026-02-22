package com.myachi.mcdonaldsmod;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.FoodComponents;
import net.minecraft.item.Item;


import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {

    public static final Item STEEL_INGOT = register("steel_ingot",Item::new,new Item.Settings());
    public static final Item IRIDIUM = register("iridium",Item::new,new Item.Settings());
    public static final Item IRIDIUM_SHARD = register("iridium_shard",Item::new,new Item.Settings());
    public static final Item SALT = register("salt",Item::new,new Item.Settings());
    public static final Item RAW_SALT = register("raw_salt",Item::new,new Item.Settings());
    public static final Item SOYBEAN_OIL = register("soybean_oil",Item::new,new Item.Settings());

    public static final Item CHEESE = register("cheese",Item::new, new Item.Settings().food(ModFoodComponent.CHEESE));
    public static final Item TOMATO = register("tomato",Item::new, new Item.Settings().food(ModFoodComponent.TOMATO));
    public static final Item ONION = register("onion",Item::new, new Item.Settings().food(ModFoodComponent.ONION));
    public static final Item CHEESE_HAMBURGER = register("cheese_hamburger",Item::new, new Item.Settings().food(ModFoodComponent.CHEESE_HAMBURGER));

    private ModItems() {
    }

    public static Item register(String path, Function<Item.Settings,Item> factory,Item.Settings settings) {
        final RegistryKey<Item> registerKey = RegistryKey.of(RegistryKeys.ITEM,Identifier.of(McDonaldsMod.MOD_ID,path));
        return Items.register(registerKey,factory,settings);
    }

/*    public static void registerToVanillaItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content->{
            content.addAfter(Items.IRON_INGOT,STEEL_INGOT);
        });
    }*/


    public static void itemTooltipInitializer(){
        ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipType, list) -> {
            if (!itemStack.isOf(STEEL_INGOT)) {
                return;
            }
            list.add(Text.translatable("item.mcdonalds-mod.steel_ingot.tooltip"));
        });
    }

    public static void initializeMod() {
        //ModItems.registerToVanillaItemGroups();
        itemTooltipInitializer();
        McDonaldsMod.LOGGER.info("Register ModItems!");
    }


}
