package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.item.CrushedOres;
import com.myachi.mcdonaldsmod.item.ModSmallDusts;
import com.myachi.mcdonaldsmod.material.MaterialFamily;
import com.myachi.mcdonaldsmod.material.ModMaterials;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * 创造模式物品栏，按用途分成三栏：
 *
 * <ol>
 *     <li><b>材料</b> {@code mcdonald_materials} —— 矿石 / 粗矿 / 材料块 / 锭 / 板 / 粉 / 小撮粉；</li>
 *     <li><b>食物</b> {@code mcdonald_food} —— 农作物、种子、食材、做好的食物；</li>
 *     <li><b>其他</b> {@code mcdonald_misc} —— 机器、电缆、工具、零件、盐这类杂项。</li>
 * </ol>
 *
 * <p>栏位顺序就是注册顺序（谁先注册谁在前）。老的单栏 {@code mcdonald_group} 已经拆掉，
 * 原来那一栏的 id 留给"材料"继续用，避免外部（JEI 收藏、整合包脚本）失效。
 */
public final class ModItemGroups {
    // ------------------------------------------------------------------
    // 1. 材料：矿石 / 粗矿 / 材料块 / 锭 / 板 / 粉 / 小撮粉
    // ------------------------------------------------------------------
    public static final ItemGroup MATERIALS = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.IRON_REFINED_INGOT))
            .displayName(Text.translatable("itemGroup.mcdonalds-mod.materials"))
            .entries((context, entries) -> {
                // ---- 矿石 ----
                entries.add(ModBlocks.SALT_ORE);
                entries.add(ModBlocks.DEEPSLATE_SALT_ORE);
                for (MaterialFamily family : ModMaterials.all()) {
                    for (MaterialFamily.Part part : new MaterialFamily.Part[]{
                            MaterialFamily.Part.ORE, MaterialFamily.Part.DEEPSLATE_ORE,
                            MaterialFamily.Part.NETHER_ORE, MaterialFamily.Part.END_ORE}) {
                        if (family.has(part)) {
                            entries.add(family.block(part));
                        }
                    }
                }

                // ---- 粗矿 / 粗矿粒 ----
                entries.add(ModItems.RAW_SALT);
                for (MaterialFamily family : ModMaterials.all()) {
                    for (MaterialFamily.Part part : new MaterialFamily.Part[]{
                            MaterialFamily.Part.RAW, MaterialFamily.Part.RAW_NUGGET}) {
                        Item item = family.item(part);
                        if (item != null) {
                            entries.add(item);
                        }
                    }
                }

                // ---- 粉碎矿 / 纯净粉碎矿 ----
                for (CrushedOres.Entry crushed : CrushedOres.all()) {
                    entries.add(CrushedOres.crushed(crushed.material()));
                }
                for (CrushedOres.Entry crushed : CrushedOres.all()) {
                    entries.add(CrushedOres.pure(crushed.material()));
                }

                // ---- 材料块 ----
                entries.add(ModBlocks.IRON_REFINED_BLOCK);
                entries.add(ModBlocks.CHARCOAL_BLOCK);
                for (MaterialFamily family : ModMaterials.all()) {
                    if (family.has(MaterialFamily.Part.BLOCK)) {
                        entries.add(family.block(MaterialFamily.Part.BLOCK));
                    }
                }

                // ---- 锭 ----
                entries.add(ModItems.IRON_REFINED_INGOT);
                for (MaterialFamily family : ModMaterials.all()) {
                    Item ingot = family.item(MaterialFamily.Part.INGOT);
                    if (ingot != null) {
                        entries.add(ingot);
                    }
                }
                entries.add(ModItems.IRIDIUM);

                // ---- 板 ----
                for (MaterialFamily family : ModMaterials.all()) {
                    Item plate = family.item(MaterialFamily.Part.PLATE);
                    if (plate != null) {
                        entries.add(plate);
                    }
                }
                entries.add(ModItems.IRON_REFINED_PLATE);
                entries.add(ModItems.IRON_PLATE);
                entries.add(ModItems.COPPER_PLATE);
                entries.add(ModItems.GOLD_PLATE);
                entries.add(ModItems.LAPIS_PLATE);
                entries.add(ModItems.OBSIDIAN_PLATE);
                entries.add(ModItems.REDSTONE_PLATE);
                entries.add(ModItems.DIAMOND_PLATE);
                entries.add(ModItems.IRIDIUM_PLATE);

                // ---- 粉 ----
                for (MaterialFamily family : ModMaterials.all()) {
                    Item dust = family.item(MaterialFamily.Part.DUST);
                    if (dust != null) {
                        entries.add(dust);
                    }
                }
                entries.add(ModItems.COPPER_DUST);
                entries.add(ModItems.GOLD_DUST);
                entries.add(ModItems.IRON_DUST);
                entries.add(ModItems.COAL_DUST);
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
                entries.add(ModItems.SULFUR_DUST);
                entries.add(ModItems.BERYLLIUM_DUST);
                entries.add(ModItems.IRIDIUM_DUST);
                entries.add(ModItems.FLINT_DUST);
                entries.add(ModItems.ENERGIUM_DUST);

                // ---- 小撮粉 ----
                for (Item smallDust : ModSmallDusts.items()) {
                    entries.add(smallDust);
                }
            })
            .build();

    // ------------------------------------------------------------------
    // 2. 食物 / 农作物
    // ------------------------------------------------------------------
    public static final ItemGroup FOOD = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.CHEESE_HAMBURGER))
            .displayName(Text.translatable("itemGroup.mcdonalds-mod.food"))
            .entries((context, entries) -> {
                // ---- 农作物与种子 ----
                entries.add(ModItems.TOMATO);
                entries.add(ModItems.ONION);
                entries.add(ModItems.CORN);
                entries.add(ModItems.BLUEBERRY);
                entries.add(ModItems.TOMATO_SEEDS);
                entries.add(ModItems.ONION_SEEDS);
                entries.add(ModItems.CORN_SEEDS);
                entries.add(ModItems.BLUEBERRY_BUSH);
                entries.add(ModItems.SOYBEANS);
                entries.add(ModItems.TEA_LEAVES);

                // ---- 生食材 ----
                entries.add(ModItems.SALT);
                entries.add(ModItems.VEGETABLE_OIL);
                entries.add(ModItems.FLOUR);
                entries.add(ModItems.DOUGH);
                entries.add(ModItems.SUGARY_DOUGH);
                entries.add(ModItems.COCOA_POWDER);
                entries.add(ModItems.CREAM);
                entries.add(ModItems.BUTTER);
                entries.add(ModItems.CHEESE);
                entries.add(ModItems.SOYBEAN_MILK);
                entries.add(ModItems.SOYBEAN_MEAL);
                entries.add(ModItems.POTATO_STRIPS);
                entries.add(ModItems.CHOCOLATE);
                entries.add(ModItems.MILK_CHOCOLATE);

                // ---- 主菜 ----
                entries.add(ModItems.CHEESE_HAMBURGER);
                entries.add(ModItems.CHICKEN_BURGER);
                entries.add(ModItems.HOT_DOG);
                entries.add(ModItems.SAUSAGE);
                entries.add(ModItems.COOKED_SAUSAGE);
                entries.add(ModItems.FRIED_FISH);
                entries.add(ModItems.FRENCH_FRIES);
                entries.add(ModItems.COOKED_EGG);
                entries.add(ModItems.COOKED_CORN);
                entries.add(ModItems.TOFU);
                entries.add(ModItems.BAKED_BEANS);

                // ---- 汤 / 粥 / 麦片 ----
                entries.add(ModItems.SALAD);
                entries.add(ModItems.CREAM_OF_MUSHROOM_SOUP);
                entries.add(ModItems.BEEF_STEW);
                entries.add(ModItems.PORRIDGE);
                entries.add(ModItems.CEREAL);
                entries.add(ModItems.VEGETABLE_SOUP);
                entries.add(ModItems.CREAM_OF_VEGETABLE_SOUP);
                entries.add(ModItems.PUMPKIN_SOUP);

                // ---- 甜点 ----
                entries.add(ModBlocks.CHEESE_CAKE);
                entries.add(ModBlocks.CHOCOLATE_CAKE);
                entries.add(ModItems.APPLE_PIE);
                entries.add(ModItems.APPLE_SANDWICH_COOKIE);

                // ---- 饮料与杂项 ----
                entries.add(ModItems.FIJI_CUP);
                entries.add(ModItems.FULL_FIJI_CUP);
                entries.add(ModItems.CHUM);
                entries.add(ModItems.CHUM_ON_STICK);
            })
            .build();

    // ------------------------------------------------------------------
    // 3. 其他：机器 / 电缆 / 工具 / 零件
    // ------------------------------------------------------------------
    public static final ItemGroup MISC = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModBlocks.MACHINE_SHELL))
            .displayName(Text.translatable("itemGroup.mcdonalds-mod.misc"))
            .entries((context, entries) -> {
                // ---- 机器 ----
                entries.add(ModBlocks.MACHINE_SHELL);
                entries.add(ModBlocks.TEST_GENERATOR);
                entries.add(ModBlocks.TEST_BATTERY_BOX);
                entries.add(ModBlocks.ELECTRIC_FURNACE);

                // ---- 电缆与仪表 ----
                entries.add(ModItems.TIN_CABLE);
                entries.add(ModItems.COPPER_CABLE);
                entries.add(ModItems.COPPER_CABLE_X2);
                entries.add(ModItems.IRON_REFINED_CABLE);
                entries.add(ModItems.GOLD_CABLE);
                entries.add(ModItems.GOLD_CABLE_X2);
                entries.add(ModItems.IRON_CABLE);
                entries.add(ModItems.FIBERGLASS_CABLE);
                entries.add(ModItems.METER);

                // ---- 工具 ----
                entries.add(ModItems.STONE_MORTAR);
                entries.add(ModItems.IRON_REFINED_MORTAR);
                entries.add(ModItems.NETHERITE_MORTAR);
                entries.add(ModItems.IRON_REFINED_HAMMER);
                entries.add(ModItems.IRON_REFINED_CUTTER);

                // ---- 零件 ----
                entries.add(ModItems.COPPER_STRIPS);
                entries.add(ModItems.IRIDIUM_SHARD);
                entries.add(ModItems.CIRCUIT_BOARD);
            })
            .build();

    private ModItemGroups() {
    }

    public static void initializeModItemGroups() {
        // 注册顺序 = 物品栏里的排列顺序
        Registry.register(Registries.ITEM_GROUP,
                Identifier.of(McDonaldsMod.MOD_ID, "mcdonald_group"), MATERIALS);
        Registry.register(Registries.ITEM_GROUP,
                Identifier.of(McDonaldsMod.MOD_ID, "mcdonald_food"), FOOD);
        Registry.register(Registries.ITEM_GROUP,
                Identifier.of(McDonaldsMod.MOD_ID, "mcdonald_misc"), MISC);
        McDonaldsMod.LOGGER.info("Registry ItemGroups（材料 / 食物 / 其他 三栏）");
    }
}
