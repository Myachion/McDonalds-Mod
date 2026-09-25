package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.specialItem.FIJI_CUP;
import com.myachi.mcdonaldsmod.specialItem.DurableCraftingTool;
import com.myachi.mcdonaldsmod.specialItem.Mortar;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;


import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.function.Function;

public class ModItems {

    public static final Item IRON_REFINED_INGOT = register("iron_refined_ingot",Item::new,new Item.Settings());
    public static final Item IRIDIUM = register("iridium",Item::new,new Item.Settings());
    public static final Item IRIDIUM_SHARD = register("iridium_shard",Item::new,new Item.Settings());
    public static final Item SALT = register("salt",Item::new,new Item.Settings());
    public static final Item RAW_SALT = register("raw_salt",Item::new,new Item.Settings());
    public static final Item IRON_REFINED_PLATE = register("iron_refined_plate",Item::new,new Item.Settings());
    public static final Item IRON_PLATE = register("iron_plate",Item::new,new Item.Settings());
    public static final Item COPPER_PLATE = register("copper_plate",Item::new,new Item.Settings());

    // 矿石产物：粗矿 + 锭。锡/铝没有深层矿石，铅/铀有。
    public static final Item RAW_TIN = register("raw_tin",Item::new,new Item.Settings());
    public static final Item RAW_LEAD = register("raw_lead",Item::new,new Item.Settings());
    public static final Item RAW_ALUMINUM = register("raw_aluminum",Item::new,new Item.Settings());
    public static final Item RAW_URANIUM = register("raw_uranium",Item::new,new Item.Settings());

    public static final Item TIN_INGOT = register("tin_ingot",Item::new,new Item.Settings());
    public static final Item LEAD_INGOT = register("lead_ingot",Item::new,new Item.Settings());
    public static final Item ALUMINUM_INGOT = register("aluminum_ingot",Item::new,new Item.Settings());
    public static final Item URANIUM_INGOT = register("uranium_ingot",Item::new,new Item.Settings());
    public static final Item BRONZE_INGOT = register("bronze_ingot",Item::new,new Item.Settings());
    public static final Item SILVER_INGOT = register("silver_ingot",Item::new,new Item.Settings());

    // 板材。青金石/黑曜石/红石/钻石这四种做不出来（暂时没有配方），只能靠其他途径获得。
    public static final Item BRONZE_PLATE = register("bronze_plate",Item::new,new Item.Settings());
    public static final Item GOLD_PLATE = register("gold_plate",Item::new,new Item.Settings());
    public static final Item LAPIS_PLATE = register("lapis_plate",Item::new,new Item.Settings());
    public static final Item LEAD_PLATE = register("lead_plate",Item::new,new Item.Settings());
    public static final Item OBSIDIAN_PLATE = register("obsidian_plate",Item::new,new Item.Settings());
    public static final Item REDSTONE_PLATE = register("redstone_plate",Item::new,new Item.Settings());
    public static final Item SILVER_PLATE = register("silver_plate",Item::new,new Item.Settings());
    public static final Item TIN_PLATE = register("tin_plate",Item::new,new Item.Settings());
    public static final Item ALUMINUM_PLATE = register("aluminum_plate",Item::new,new Item.Settings());
    public static final Item DIAMOND_PLATE = register("diamond_plate",Item::new,new Item.Settings());

    /*
     * 粉末。带配方的那批（铝/铜/绿宝石/末地石/金/铁/青金石/铅/地狱岩/下界石英/银/锡）
     * 用研钵研磨对应材料得到，其余暂时只能从创造模式或以后的其他途径获取。
     */
    public static final Item ALUMINUM_DUST = register("aluminum_dust",Item::new,new Item.Settings());
    public static final Item ASH_DUST = register("ash_dust",Item::new,new Item.Settings());
    public static final Item BRONZE_DUST = register("bronze_dust",Item::new,new Item.Settings());
    public static final Item CARBON_DUST = register("carbon_dust",Item::new,new Item.Settings());
    public static final Item CLAY_DUST = register("clay_dust",Item::new,new Item.Settings());
    public static final Item WET_CARBON_DUST = register("wet_carbon_dust",Item::new,new Item.Settings());
    public static final Item COPPER_DUST = register("copper_dust",Item::new,new Item.Settings());
    public static final Item DIAMOND_DUST = register("diamond_dust",Item::new,new Item.Settings());
    public static final Item EMERALD_DUST = register("emerald_dust",Item::new,new Item.Settings());
    public static final Item END_STONE_DUST = register("end_stone_dust",Item::new,new Item.Settings());
    public static final Item GOLD_DUST = register("gold_dust",Item::new,new Item.Settings());
    public static final Item IRON_DUST = register("iron_dust",Item::new,new Item.Settings());
    public static final Item LAPIS_DUST = register("lapis_dust",Item::new,new Item.Settings());
    public static final Item LEAD_DUST = register("lead_dust",Item::new,new Item.Settings());
    public static final Item LITHIUM_DUST = register("lithium_dust",Item::new,new Item.Settings());
    public static final Item NETHERRACK_DUST = register("netherrack_dust",Item::new,new Item.Settings());
    public static final Item OBSIDIAN_DUST = register("obsidian_dust",Item::new,new Item.Settings());
    public static final Item PHOSPHORUS_DUST = register("phosphorus_dust",Item::new,new Item.Settings());
    public static final Item QUARTZ_DUST = register("quartz_dust",Item::new,new Item.Settings());
    public static final Item RED_ALLOY_DUST = register("red_alloy_dust",Item::new,new Item.Settings());
    public static final Item SILICON_DIOXIDE_DUST = register("silicon_dioxide_dust",Item::new,new Item.Settings());
    public static final Item SILVER_DUST = register("silver_dust",Item::new,new Item.Settings());
    public static final Item STONE_DUST = register("stone_dust",Item::new,new Item.Settings());
    public static final Item TIN_DUST = register("tin_dust",Item::new,new Item.Settings());

    // 线缆按电压等级排列：锡 32V / 铜·钢 128V / 金 512V / 铁 2048V / 玻璃纤维 8192V
    // 电缆是可放置的方块，物品就是对应方块的 BlockItem（注册名不变）。
    public static final Item TIN_CABLE = ModBlocks.TIN_CABLE.asItem();
    public static final Item COPPER_CABLE = ModBlocks.COPPER_CABLE.asItem();
    public static final Item IRON_REFINED_CABLE = ModBlocks.IRON_REFINED_CABLE.asItem();
    public static final Item GOLD_CABLE = ModBlocks.GOLD_CABLE.asItem();
    public static final Item IRON_CABLE = ModBlocks.IRON_CABLE.asItem();
    public static final Item FIBERGLASS_CABLE = ModBlocks.FIBERGLASS_CABLE.asItem();

    public static final Item COPPER_STRIPS = register("copper_strips",Item::new,new Item.Settings());
    public static final Item COAL_DUST = register("coal_dust",Item::new,new Item.Settings());
    public static final Item IRIDIUM_PLATE = register("iridium_plate",Item::new,new Item.Settings());
    public static final Item CIRCUIT_BOARD = register("circuit_board",Item::new,new Item.Settings());

    public static final Item COCOA_POWDER = register("cocoa_powder",Item::new,new Item.Settings());
    public static final Item BUTTER = register("butter",Item::new,new Item.Settings().food(ModFoodComponent.BUTTER));
    public static final Item CHOCOLATE = register("chocolate",Item::new,new Item.Settings().food(ModFoodComponent.CHOCOLATE));
    public static final Item FRIED_FISH = register("fried_fish",Item::new,new Item.Settings().food(ModFoodComponent.FRIED_FISH));
    public static final Item MILK_CHOCOLATE = register("milk_chocolate",Item::new,new Item.Settings().food(ModFoodComponent.MILK_CHOCOLATE));
    public static final Item SAUSAGE = register("sausage",Item::new,new Item.Settings().food(ModFoodComponent.SAUSAGE));
    public static final Item COOKED_SAUSAGE = register("cooked_sausage",Item::new,new Item.Settings().food(ModFoodComponent.COOKED_SAUSAGE));
    public static final Item FRENCH_FRIES = register("french_fries",Item::new,new Item.Settings().food(ModFoodComponent.FRENCH_FRIES));

    public static final Item COOKED_EGG = register("cooked_egg",Item::new,new Item.Settings().food(ModFoodComponent.COOKED_EGG));
    public static final Item SALAD = register("salad",Item::new,new Item.Settings().food(ModFoodComponent.SALAD).useRemainder(Items.BOWL).maxCount(8));
    public static final Item CREAM_OF_MUSHROOM_SOUP = register("cream_of_mushroom_soup",Item::new,new Item.Settings().food(ModFoodComponent.CREAM_OF_MUSHROOM_SOUP).useRemainder(Items.BOWL).maxCount(8));
    public static final Item BEEF_STEW = register("beef_stew",Item::new,new Item.Settings().food(ModFoodComponent.BEEF_STEW).useRemainder(Items.BOWL).maxCount(8));
    public static final Item PORRIDGE = register("porridge",Item::new,new Item.Settings().food(ModFoodComponent.PORRIDGE).useRemainder(Items.BOWL).maxCount(8));

    public static final Item TOFU = register("tofu",Item::new,new Item.Settings().food(ModFoodComponent.TOFU));
    public static final Item BAKED_BEANS = register("baked_beans",Item::new,new Item.Settings().food(ModFoodComponent.BAKED_BEANS));
    public static final Item HOT_DOG = register("hot_dog",Item::new,new Item.Settings().food(ModFoodComponent.HOT_DOG));

    public static final Item CHEESE = register("cheese",Item::new, new Item.Settings().food(ModFoodComponent.CHEESE));
    public static final Item TOMATO = register("tomato",Item::new, new Item.Settings().food(ModFoodComponent.TOMATO));
    public static final Item ONION = register("onion",Item::new, new Item.Settings().food(ModFoodComponent.ONION));
    public static final Item CHEESE_HAMBURGER = register("cheese_hamburger",Item::new, new Item.Settings().food(ModFoodComponent.CHEESE_HAMBURGER));
    public static final Item CHICKEN_BURGER = register("chicken_burger",Item::new, new Item.Settings().food(ModFoodComponent.CHICKEN_BURGER));

    public static final Item FLOUR = register("flour",Item::new, new Item.Settings());
    // 种子是方块物品：种在耕地上会长出对应作物（useItemPrefixedTranslationKey 保证名称仍是 item.* 而不是 block.*）。
    public static final Item TOMATO_SEEDS = register("tomato_seeds",
            settings -> new BlockItem(ModBlocks.TOMATO_CROP, settings.useItemPrefixedTranslationKey()), new Item.Settings());
    public static final Item ONION_SEEDS = register("onion_seeds",
            settings -> new BlockItem(ModBlocks.ONION_CROP, settings.useItemPrefixedTranslationKey()), new Item.Settings());

    public static final Item TEA_LEAVES = register("tea_leaves",Item::new, new Item.Settings());
    public static final Item CREAM = register("cream",Item::new, new Item.Settings().food(ModFoodComponent.CREAM));
    public static final Item POTATO_STRIPS = register("potato_strips",Item::new, new Item.Settings().food(ModFoodComponent.POTATO_STRIPS));
    public static final Item SOYBEANS = register("soybeans",Item::new, new Item.Settings());
    public static final Item VEGETABLE_OIL = register("vegetable_oil",Item::new,new Item.Settings());
    public static final Item SOYBEAN_MILK = register("soybean_milk",Item::new, new Item.Settings().food(ModFoodComponent.SOYBEANS_MILK));
    public static final Item SOYBEAN_MEAL = register("soybean_meal",Item::new, new Item.Settings().food(ModFoodComponent.SOYBEAN_MEAL));

    public static final Item APPLE_PIE = register("apple_pie",Item::new, new Item.Settings().food(ModFoodComponent.APPLE_PIE));
    public static final Item APPLE_SANDWICH_COOKIE = register("apple_sandwich_cookie",Item::new, new Item.Settings().food(ModFoodComponent.APPLE_SANDWICH_COOKIE));
    // 蓝莓既是食物，也是"种子"：右键泥土/草方块就能种出蓝莓丛（与原版甜浆果一致）。
    public static final Item BLUEBERRY = register("blueberry",
            settings -> new BlockItem(ModBlocks.BLUEBERRY_BUSH, settings.useItemPrefixedTranslationKey()),
            new Item.Settings().food(ModFoodComponent.BLUEBERRY));
    // 蓝莓丛物品：精准采集才掉，放下来和用蓝莓种出来一样，都是 age 0。
    public static final Item BLUEBERRY_BUSH = register("blueberry_bush",
            settings -> new BlockItem(ModBlocks.BLUEBERRY_BUSH, settings.useItemPrefixedTranslationKey()), new Item.Settings());

    public static final Item CORN = register("corn",Item::new, new Item.Settings().food(ModFoodComponent.CORN));
    public static final Item CORN_SEEDS = register("corn_seeds",
            settings -> new BlockItem(ModBlocks.CORN_CROP, settings.useItemPrefixedTranslationKey()), new Item.Settings());
    public static final Item COOKED_CORN = register("cooked_corn",Item::new, new Item.Settings().food(ModFoodComponent.COOKED_CORN));
    public static final Item DOUGH = register("dough",Item::new, new Item.Settings());
    public static final Item SUGARY_DOUGH = register("sugary_dough",Item::new, new Item.Settings());
    public static final Item CEREAL = register("cereal",Item::new, new Item.Settings().food(ModFoodComponent.CEREAL).useRemainder(Items.BOWL).maxCount(8));
    public static final Item VEGETABLE_SOUP = register("vegetable_soup",Item::new, new Item.Settings().food(ModFoodComponent.VEGETABLE_SOUP).useRemainder(Items.BOWL).maxCount(8));
    public static final Item CREAM_OF_VEGETABLE_SOUP = register("cream_of_vegetable_soup",Item::new, new Item.Settings().food(ModFoodComponent.CREAM_OF_VEGETABLE_SOUP).useRemainder(Items.BOWL).maxCount(8));
    public static final Item PUMPKIN_SOUP = register("pumpkin_soup",Item::new, new Item.Settings().food(ModFoodComponent.PUMPKIN_SOUP).useRemainder(Items.BOWL).maxCount(8));

    public static final Item CHUM = register("chum",Item::new, new Item.Settings().food(ModFoodComponent.CHUM,ModConsumableComponents.CHUM));
    public static final Item CHUM_ON_STICK = register("chum_on_stick",Item::new, new Item.Settings().food(ModFoodComponent.CHUM_ON_STICK,ModConsumableComponents.CHUM).useRemainder(Items.STICK));

    public static final Item STONE_MORTAR = register("stone_mortar",Mortar::new, new Item.Settings().maxCount(1).maxDamage(197));
    public static final Item IRON_REFINED_MORTAR = register("iron_refined_mortar",Mortar::new, new Item.Settings().maxCount(1).maxDamage(563));
    public static final Item NETHERITE_MORTAR = register("netherite_mortar",Mortar::new, new Item.Settings().maxCount(1).maxDamage(2267));

    public static final Item IRON_REFINED_HAMMER = register("iron_refined_hammer",DurableCraftingTool::new, new Item.Settings().maxCount(1).maxDamage(120));
    public static final Item IRON_REFINED_CUTTER = register("iron_refined_cutter",DurableCraftingTool::new, new Item.Settings().maxCount(1).maxDamage(120));

    /** 万用表：右键电缆查看电网状态。 */
    public static final Item METER = register("meter", com.myachi.mcdonaldsmod.machine.MeterItem::new, new Item.Settings().maxCount(1));





    public static final Item FIJI_CUP = register("fiji_cup",FIJI_CUP::new,new FIJI_CUP.Settings().rarity(Rarity.EPIC));
    public static final Item FULL_FIJI_CUP = register("full_fiji_cup",Item::new,new Item.Settings()
            .rarity(Rarity.EPIC)
            .recipeRemainder(FIJI_CUP)
            .useRemainder(FIJI_CUP)
            .food(ModFoodComponent.FULL_FIJI_CUP,ModConsumableComponents.FULL_FIJI_CUP));

    private ModItems() {
    }

    public static Item register(String path, Function<Item.Settings,Item> factory,Item.Settings settings) {
        final RegistryKey<Item> registerKey = RegistryKey.of(RegistryKeys.ITEM,Identifier.of(McDonaldsMod.MOD_ID,path));
        return Items.register(registerKey,factory,settings);
    }

/*    public static void registerToVanillaItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content->{
            content.addAfter(Items.IRON_INGOT,IRON_REFINED_INGOT);
        });
    }*/


    public static void initializeMod() {
        //ModItems.registerToVanillaItemGroups();
        McDonaldsMod.LOGGER.info("Register ModItems!");
    }


}
