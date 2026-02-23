package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.specialItem.FIJI_CUP;
import com.myachi.mcdonaldsmod.specialItem.FULL_FIJI_CUP;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.Item;


import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.function.Function;

public class ModItems {

    public static final Item STEEL_INGOT = register("steel_ingot",Item::new,new Item.Settings());
    public static final Item IRIDIUM = register("iridium",Item::new,new Item.Settings());
    public static final Item IRIDIUM_SHARD = register("iridium_shard",Item::new,new Item.Settings());
    public static final Item SALT = register("salt",Item::new,new Item.Settings());
    public static final Item RAW_SALT = register("raw_salt",Item::new,new Item.Settings());
    public static final Item STEEL_PLATE = register("steel_plate",Item::new,new Item.Settings());
    public static final Item IRON_PLATE = register("iron_plate",Item::new,new Item.Settings());
    public static final Item COPPER_PLATE = register("copper_plate",Item::new,new Item.Settings());
    public static final Item COPPER_STRIPS = register("copper_strips",Item::new,new Item.Settings());
    public static final Item COAL_DUST = register("coal_dust",Item::new,new Item.Settings());
    public static final Item IRIDIUM_PLATE = register("iridium_plate",Item::new,new Item.Settings());
    public static final Item CIRCUIT_BOARD = register("circuit_board",Item::new,new Item.Settings());

    public static final Item CHEESE_CAKE = register("cheese_cake",Item::new,new Item.Settings());
    public static final Item CHOCOLATE_CAKE = register("chocolate_cake",Item::new,new Item.Settings());
    public static final Item COCOA_POWDER = register("cocoa_powder",Item::new,new Item.Settings());
    public static final Item BUTTER = register("butter",Item::new,new Item.Settings());
    public static final Item CHOCOLATE = register("chocolate",Item::new,new Item.Settings());
    public static final Item FRIED_FISH = register("fried_fish",Item::new,new Item.Settings());
    public static final Item MILK_CHOCOLATE = register("milk_chocolate",Item::new,new Item.Settings());
    public static final Item SAUSAGE = register("sausage",Item::new,new Item.Settings());
    public static final Item COOKED_SAUSAGE = register("cooked_sausage",Item::new,new Item.Settings());
    public static final Item FRENCH_FRIES = register("french_fries",Item::new,new Item.Settings());

    public static final Item COOKED_EGG = register("cooked_egg",Item::new,new Item.Settings());
    public static final Item SALAD = register("salad",Item::new,new Item.Settings());
    public static final Item CREAM_OF_MUSHROOM_SOUP = register("cream_of_mushroom_soup",Item::new,new Item.Settings());
    public static final Item BEEF_STEW = register("beef_stew",Item::new,new Item.Settings());
    public static final Item PORRIDGE = register("porridge",Item::new,new Item.Settings());

    public static final Item TOFU = register("tofu",Item::new,new Item.Settings());
    public static final Item BAKED_BEANS = register("baked_beans",Item::new,new Item.Settings());
    public static final Item HOT_DOG = register("hot_dog",Item::new,new Item.Settings());

    public static final Item CHEESE = register("cheese",Item::new, new Item.Settings().food(ModFoodComponent.CHEESE));
    public static final Item TOMATO = register("tomato",Item::new, new Item.Settings().food(ModFoodComponent.TOMATO));
    public static final Item ONION = register("onion",Item::new, new Item.Settings().food(ModFoodComponent.ONION));
    public static final Item CHEESE_HAMBURGER = register("cheese_hamburger",Item::new, new Item.Settings().food(ModFoodComponent.CHEESE_HAMBURGER));
    public static final Item CHICKEN_BURGER = register("chicken_burger",Item::new, new Item.Settings());

    public static final Item FLOUR = register("flour",Item::new, new Item.Settings());
    public static final Item TOMATO_SEEDS = register("tomato_seeds",Item::new, new Item.Settings());
    public static final Item ONION_SEEDS = register("onion_seeds",Item::new, new Item.Settings());

    public static final Item TEA_LEAVES = register("tea_leaves",Item::new, new Item.Settings());
    public static final Item CREAM = register("cream",Item::new, new Item.Settings());
    public static final Item POTATO_STRIPS = register("potato_strips",Item::new, new Item.Settings());
    public static final Item SOYBEANS = register("soybeans",Item::new, new Item.Settings());
    public static final Item SOYBEAN_OIL = register("soybean_oil",Item::new,new Item.Settings());
    public static final Item SOYBEAN_MILK = register("soybean_milk",Item::new, new Item.Settings());
    public static final Item SOYBEAN_MEAL = register("soybean_meal",Item::new, new Item.Settings());

    public static final Item APPLE_PIE = register("apple_pie",Item::new, new Item.Settings());
    public static final Item APPLE_SANDWICH_COOKIE = register("apple_sandwich_cookie",Item::new, new Item.Settings());
    public static final Item BLUEBERRY = register("blueberry",Item::new, new Item.Settings());
    public static final Item BLUEBERRY_BUSH = register("blueberry_bush",Item::new, new Item.Settings());

    public static final Item CORN = register("corn",Item::new, new Item.Settings());
    public static final Item CORN_SEEDS = register("corn_seeds",Item::new, new Item.Settings());
    public static final Item COOKED_CORN = register("cooked_corn",Item::new, new Item.Settings());
    public static final Item DOUGH = register("dough",Item::new, new Item.Settings());
    public static final Item SUGARY_DOUGH = register("sugary_dough",Item::new, new Item.Settings());
    public static final Item CEREAL = register("cereal",Item::new, new Item.Settings());
    public static final Item VEGETABLE_SOUP = register("vegetable_soup",Item::new, new Item.Settings());
    public static final Item CREAM_OF_VEGETABLE_SOUP = register("cream_of_vegetable_soup",Item::new, new Item.Settings());
    public static final Item PUMPKIN_SOUP = register("pumpkin_soup",Item::new, new Item.Settings());



    public static final Item FIJI_CUP = register("fiji_cup",FIJI_CUP::new,new FIJI_CUP.Settings().rarity(Rarity.EPIC));
    public static final Item FULL_FIJI_CUP = register("full_fiji_cup",FULL_FIJI_CUP::new,new FULL_FIJI_CUP.Settings()
            .rarity(Rarity.EPIC)
            .recipeRemainder(FIJI_CUP)
            .food(ModFoodComponent.FULL_FIJI_CUP,ModConsumableComponents.FULL_FIJI_CUP));


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
