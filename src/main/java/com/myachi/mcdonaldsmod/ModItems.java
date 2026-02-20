package com.myachi.mcdonaldsmod;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
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

    private ModItems() {
    }

    public static Item register(String path, Function<Item.Settings,Item> factory,Item.Settings settings) {
        final RegistryKey<Item> registerKey = RegistryKey.of(RegistryKeys.ITEM,Identifier.of(McDonaldsMod.MOD_ID,path));
        return Items.register(registerKey,factory,settings);
    }

/*    public static void registerToVanillaItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content->{
            content.addAfter(Items.IRON_INGOT,STEEL_INGOT);
            content.addAfter(Items.RAW_GOLD,IRIDIUM);
            content.addAfter(Items.GOLD_NUGGET,IRIDIUM_SHARD);
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
        McDonaldsMod.LOGGER.info("Register TestItems!");
    }


}
