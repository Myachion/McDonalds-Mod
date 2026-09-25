package com.myachi.mcdonaldsmod;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;

public class ModItemGroups {
    public static final ItemGroup MCDONALD_GROUP = FabricItemGroup.builder()
            .icon(()->new ItemStack(ModItems.CHEESE_HAMBURGER))
            .displayName(Text.translatable("itemGroup.mcdonalds-mod.mcdonald_group"))
            .entries((context,entries)-> {
                entries.add(ModBlocks.SALT_ORE);
                entries.add(ModBlocks.DEEPSLATE_SALT_ORE);
                entries.add(ModBlocks.IRON_REFINED_BLOCK);
                entries.add(ModBlocks.ALUMINUM_BLOCK);
                entries.add(ModBlocks.BRONZE_BLOCK);
                entries.add(ModBlocks.CHARCOAL_BLOCK);
                entries.add(ModBlocks.LEAD_BLOCK);
                entries.add(ModBlocks.SILVER_BLOCK);
                entries.add(ModBlocks.STEEL_BLOCK);
                entries.add(ModBlocks.TIN_BLOCK);
                entries.add(ModBlocks.URANIUM_BLOCK);
                entries.add(ModBlocks.MACHINE_SHELL);
                entries.add(ModBlocks.TEST_GENERATOR);
                entries.add(ModBlocks.TEST_BATTERY_BOX);
                entries.add(ModBlocks.TIN_ORE);
                entries.add(ModBlocks.LEAD_ORE);
                entries.add(ModBlocks.DEEPSLATE_LEAD_ORE);
                entries.add(ModBlocks.ALUMINUM_ORE);
                entries.add(ModBlocks.URANIUM_ORE);
                entries.add(ModBlocks.DEEPSLATE_URANIUM_ORE);

                entries.add(ModItems.RAW_TIN);
                entries.add(ModItems.RAW_LEAD);
                entries.add(ModItems.RAW_ALUMINUM);
                entries.add(ModItems.RAW_URANIUM);
                entries.add(ModItems.TIN_INGOT);
                entries.add(ModItems.LEAD_INGOT);
                entries.add(ModItems.ALUMINUM_INGOT);
                entries.add(ModItems.URANIUM_INGOT);
                entries.add(ModItems.BRONZE_INGOT);
                entries.add(ModItems.SILVER_INGOT);
                entries.add(ModItems.TIN_PLATE);
                entries.add(ModItems.LEAD_PLATE);
                entries.add(ModItems.ALUMINUM_PLATE);
                entries.add(ModItems.BRONZE_PLATE);
                entries.add(ModItems.SILVER_PLATE);
                entries.add(ModItems.GOLD_PLATE);
                entries.add(ModItems.LAPIS_PLATE);
                entries.add(ModItems.OBSIDIAN_PLATE);
                entries.add(ModItems.REDSTONE_PLATE);
                entries.add(ModItems.DIAMOND_PLATE);
                entries.add(ModItems.ALUMINUM_DUST);
                entries.add(ModItems.COPPER_DUST);
                entries.add(ModItems.TIN_DUST);
                entries.add(ModItems.LEAD_DUST);
                entries.add(ModItems.SILVER_DUST);
                entries.add(ModItems.GOLD_DUST);
                entries.add(ModItems.IRON_DUST);
                entries.add(ModItems.BRONZE_DUST);
                entries.add(ModItems.CARBON_DUST);
                entries.add(ModItems.WET_CARBON_DUST);
                entries.add(ModItems.ASH_DUST);
                entries.add(ModItems.CLAY_DUST);
                entries.add(ModItems.STONE_DUST);
                entries.add(ModItems.END_STONE_DUST);
                entries.add(ModItems.NETHERRACK_DUST);
                entries.add(ModItems.OBSIDIAN_DUST);
                entries.add(ModItems.QUARTZ_DUST);
                entries.add(ModItems.DIAMOND_DUST);
                entries.add(ModItems.EMERALD_DUST);
                entries.add(ModItems.LAPIS_DUST);
                entries.add(ModItems.LITHIUM_DUST);
                entries.add(ModItems.PHOSPHORUS_DUST);
                entries.add(ModItems.RED_ALLOY_DUST);
                entries.add(ModItems.SILICON_DIOXIDE_DUST);

                entries.add(ModItems.COAL_DUST);
                entries.add(ModItems.RAW_SALT);
                entries.add(ModItems.IRON_REFINED_INGOT);
                entries.add(ModItems.IRON_REFINED_PLATE);
                entries.add(ModItems.IRON_PLATE);
                entries.add(ModItems.COPPER_PLATE);
                entries.add(ModItems.COPPER_STRIPS);
                entries.add(ModItems.TIN_CABLE);
                entries.add(ModItems.COPPER_CABLE);
                entries.add(ModItems.IRON_REFINED_CABLE);
                entries.add(ModItems.GOLD_CABLE);
                entries.add(ModItems.IRON_CABLE);
                entries.add(ModItems.FIBERGLASS_CABLE);
                entries.add(ModItems.METER);

                entries.add(ModItems.IRIDIUM);
                entries.add(ModItems.IRIDIUM_SHARD);
                entries.add(ModItems.IRIDIUM_PLATE);
                entries.add(ModItems.CIRCUIT_BOARD);

                entries.add(ModItems.SALT);
                entries.add(ModItems.VEGETABLE_OIL);
                entries.add(ModItems.TOMATO);
                entries.add(ModItems.ONION);
                entries.add(ModItems.CORN);
                entries.add(ModItems.BLUEBERRY);
                entries.add(ModItems.TOMATO_SEEDS);
                entries.add(ModItems.ONION_SEEDS);
                entries.add(ModItems.CORN_SEEDS);
                entries.add(ModItems.BLUEBERRY_BUSH);

                entries.add(ModItems.CHEESE);
                entries.add(ModItems.CHEESE_HAMBURGER);
                entries.add(ModItems.CHICKEN_BURGER);


                entries.add(ModBlocks.CHEESE_CAKE);
                entries.add(ModBlocks.CHOCOLATE_CAKE);
                entries.add(ModItems.APPLE_PIE);

                entries.add(ModItems.COCOA_POWDER);
                entries.add(ModItems.CHOCOLATE);
                entries.add(ModItems.MILK_CHOCOLATE);
                entries.add(ModItems.CREAM);
                entries.add(ModItems.BUTTER);
                entries.add(ModItems.APPLE_SANDWICH_COOKIE);

                entries.add(ModItems.FRIED_FISH);
                entries.add(ModItems.FRENCH_FRIES);
                entries.add(ModItems.SAUSAGE);
                entries.add(ModItems.COOKED_SAUSAGE);
                entries.add(ModItems.COOKED_EGG);
                entries.add(ModItems.COOKED_CORN);

                entries.add(ModItems.SALAD);
                entries.add(ModItems.CREAM_OF_MUSHROOM_SOUP);
                entries.add(ModItems.BEEF_STEW);
                entries.add(ModItems.PORRIDGE);

                entries.add(ModItems.CEREAL);
                entries.add(ModItems.VEGETABLE_SOUP);
                entries.add(ModItems.CREAM_OF_VEGETABLE_SOUP);
                entries.add(ModItems.PUMPKIN_SOUP);


                entries.add(ModItems.HOT_DOG);
                entries.add(ModItems.DOUGH);
                entries.add(ModItems.SUGARY_DOUGH);


                entries.add(ModItems.SOYBEANS);
                entries.add(ModItems.BAKED_BEANS);
                entries.add(ModItems.TOFU);
                entries.add(ModItems.SOYBEAN_MILK);
                entries.add(ModItems.SOYBEAN_MEAL);
                entries.add(ModItems.POTATO_STRIPS);
                entries.add(ModItems.TEA_LEAVES);
                entries.add(ModItems.FLOUR);
                entries.add(ModItems.CHUM);
                entries.add(ModItems.CHUM_ON_STICK);
                entries.add(ModItems.STONE_MORTAR);
                entries.add(ModItems.IRON_REFINED_MORTAR);
                entries.add(ModItems.NETHERITE_MORTAR);
                entries.add(ModItems.IRON_REFINED_HAMMER);
                entries.add(ModItems.IRON_REFINED_CUTTER);
                entries.add(ModItems.FIJI_CUP);
                entries.add(ModItems.FULL_FIJI_CUP);




            })
            .build();

    public static void initializeModItemGroups () {
        Registry.register(Registries.ITEM_GROUP, Identifier.of(McDonaldsMod.MOD_ID,"mcdonald_group"),MCDONALD_GROUP);
        McDonaldsMod.LOGGER.info("Registry ItemGroups");
    }
}
