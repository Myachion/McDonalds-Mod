package com.myachi.mcdonaldsmod.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.myachi.mcdonaldsmod.McDonaldsMod;
import com.myachi.mcdonaldsmod.material.MaterialFamily;
import com.myachi.mcdonaldsmod.material.ModMaterials;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 材料族的资源生成器：把 {@link ModMaterials} 里的定义变成模型 / 方块状态 / 物品定义 /
 * 掉落表 / 配方，全部写进 {@code src/main/generated}（跑 {@code .\gradlew.bat runDatagen}）。
 *
 * <p>只处理没有标 {@code handWritten} / {@code noGeneratedAssets()} 的部件，
 * 所以迁移期间手写资源不会被覆盖。
 *
 * <p>语言条目（lang）故意<b>不</b>生成：这个 mod 的 lang 是手写的，datagen 再写一份同路径文件
 * 会在打包时互相覆盖。这里改成"检查 + 提示"：缺哪条就在日志里列出来，照着补就行。
 *
 * <p>矿石的<b>世界生成</b>（密度/高度/生物群系）也不在这里 —— 那是用户拍板的数值，
 * 仍然写在 {@code worldgen/} 与 {@code data/mcdonalds-mod/worldgen/}。
 */
public final class MaterialAssetsProvider implements DataProvider {
    /** 打板的工具（和配方里用的一样）。 */
    private static final Identifier HAMMER = Identifier.of(McDonaldsMod.MOD_ID, "iron_refined_hammer");
    /** 研钵标签，配方里写成 {@code #mcdonalds-mod:mortar}。 */
    private static final String MORTAR_TAG = "#" + McDonaldsMod.MOD_ID + ":mortar";

    private final FabricDataOutput output;

    public MaterialAssetsProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        List<MaterialFamily> generated = new ArrayList<>();
        for (MaterialFamily family : ModMaterials.all()) {
            boolean any = false;
            for (MaterialFamily.Part part : family.parts()) {
                if (family.isHandWritten(part)) {
                    continue;
                }
                any = true;
                generatePart(writer, family, part, tasks);
            }
            if (any) {
                generateRecipes(writer, family, tasks);
                generated.add(family);
            }
        }
        if (!generated.isEmpty()) {
            reportMissingLang(generated);
            McDonaldsMod.LOGGER.info("[材料族] 生成了 {} 个族的资源：{}", generated.size(), generated);
        }
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Material assets (models / loot / recipes)";
    }

    // ------------------------------------------------------------------
    // 模型 / 方块状态 / 物品定义 / 掉落表
    // ------------------------------------------------------------------

    private void generatePart(DataWriter writer, MaterialFamily family, MaterialFamily.Part part,
                              List<CompletableFuture<?>> tasks) {
        Identifier id = partId(family, part);
        if (part == MaterialFamily.Part.BLOCK) {
            tasks.add(asset(writer, "blockstates", id, blockState(blockModel(id))));
            tasks.add(asset(writer, "models/block", id, cubeAll(Identifier.of(id.getNamespace(), "block/" + id.getPath()))));
            tasks.add(asset(writer, "items", id, itemDefinition(blockModel(id))));
            tasks.add(data(writer, "loot_table/blocks", id, selfLoot(id)));
            return;
        }
        if (part.isOre()) {
            tasks.add(asset(writer, "blockstates", id, blockState(blockModel(id))));
            tasks.add(asset(writer, "models/block", id, cubeAll(Identifier.of(id.getNamespace(), "block/" + id.getPath()))));
            tasks.add(asset(writer, "items", id, itemDefinition(blockModel(id))));
            if (part == MaterialFamily.Part.NETHER_ORE) {
                // 下界矿：4~7 个粗矿粒（时运加成），精准采集掉方块
                tasks.add(data(writer, "loot_table/blocks", id,
                        oreLoot(id, itemId(family, MaterialFamily.Part.RAW_NUGGET), 4, 7)));
            } else {
                // 主世界矿：1 个粗矿（时运 ore_drops），精准采集掉方块
                tasks.add(data(writer, "loot_table/blocks", id,
                        oreLoot(id, itemId(family, MaterialFamily.Part.RAW), -1, -1)));
            }
            return;
        }
        // 贴图路径是 assets/<ns>/textures/item/<id>.png，和模型路径同名（models/item/<id>.json）
        tasks.add(asset(writer, "models/item", id, generatedItem(itemModel(id))));
        tasks.add(asset(writer, "items", id, itemDefinition(itemModel(id))));
    }

    /** {@code {"variants": {"": {"model": ...}}}}。 */
    private static JsonObject blockState(Identifier model) {
        JsonObject entry = new JsonObject();
        entry.addProperty("model", model.toString());
        JsonObject variants = new JsonObject();
        variants.add("", entry);
        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return root;
    }

    /** 六面同贴图的方块模型。 */
    private static JsonObject cubeAll(Identifier texture) {
        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture.toString());
        JsonObject root = new JsonObject();
        root.addProperty("parent", "block/cube_all");
        root.add("textures", textures);
        return root;
    }

    /** 普通物品模型（一层贴图）。 */
    private static JsonObject generatedItem(Identifier texture) {
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", texture.toString());
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:item/generated");
        root.add("textures", textures);
        return root;
    }

    /** 1.21.4+ 的物品定义（{@code assets/<ns>/items/<id>.json}）。 */
    private static JsonObject itemDefinition(Identifier model) {
        JsonObject modelObject = new JsonObject();
        modelObject.addProperty("type", "minecraft:model");
        modelObject.addProperty("model", model.toString());
        JsonObject root = new JsonObject();
        root.add("model", modelObject);
        return root;
    }

    /** 材料块：掉自己。 */
    private static JsonObject selfLoot(Identifier blockId) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", blockId.toString());
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:survives_explosion");
        JsonArray conditions = new JsonArray();
        conditions.add(condition);
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.add("entries", arrayOf(entry));
        pool.add("conditions", conditions);
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        root.add("pools", arrayOf(pool));
        return root;
    }

    /**
     * 矿石掉落：精准采集掉方块，否则掉 {@code drop}。
     * {@code min}/{@code max} 给负数表示"只掉 1 个"，否则先 set_count 再时运加成。
     */
    private static JsonObject oreLoot(Identifier oreId, Identifier dropId, int min, int max) {
        JsonObject silkTouch = new JsonObject();
        silkTouch.addProperty("type", "minecraft:item");
        silkTouch.addProperty("name", oreId.toString());
        silkTouch.add("conditions", arrayOf(silkTouchCondition()));

        JsonObject drop = new JsonObject();
        drop.addProperty("type", "minecraft:item");
        drop.addProperty("name", dropId.toString());
        JsonArray functions = new JsonArray();
        if (min >= 0) {
            JsonObject count = new JsonObject();
            count.addProperty("type", "minecraft:uniform");
            count.addProperty("min", (double) min);
            count.addProperty("max", (double) max);
            JsonObject setCount = new JsonObject();
            setCount.addProperty("function", "minecraft:set_count");
            setCount.addProperty("add", false);
            setCount.add("count", count);
            functions.add(setCount);
        }
        JsonObject bonus = new JsonObject();
        bonus.addProperty("function", "minecraft:apply_bonus");
        bonus.addProperty("enchantment", "minecraft:fortune");
        bonus.addProperty("formula", "minecraft:ore_drops");
        functions.add(bonus);
        JsonObject decay = new JsonObject();
        decay.addProperty("function", "minecraft:explosion_decay");
        functions.add(decay);
        drop.add("functions", functions);

        JsonObject alternatives = new JsonObject();
        alternatives.addProperty("type", "minecraft:alternatives");
        JsonArray children = new JsonArray();
        children.add(silkTouch);
        children.add(drop);
        alternatives.add("children", children);

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0);
        pool.addProperty("bonus_rolls", 0.0);
        pool.add("entries", arrayOf(alternatives));

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        root.add("pools", arrayOf(pool));
        root.addProperty("random_sequence", oreId.getNamespace() + ":blocks/" + oreId.getPath());
        return root;
    }

    private static JsonObject silkTouchCondition() {
        JsonObject levels = new JsonObject();
        levels.addProperty("min", 1);
        JsonObject enchantment = new JsonObject();
        enchantment.addProperty("enchantments", "minecraft:silk_touch");
        enchantment.add("levels", levels);
        JsonArray enchantments = new JsonArray();
        enchantments.add(enchantment);
        JsonObject predicates = new JsonObject();
        predicates.add("minecraft:enchantments", enchantments);
        JsonObject predicate = new JsonObject();
        predicate.add("predicates", predicates);
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:match_tool");
        condition.add("predicate", predicate);
        return condition;
    }

    // ------------------------------------------------------------------
    // 配方
    // ------------------------------------------------------------------

    private void generateRecipes(DataWriter writer, MaterialFamily family, List<CompletableFuture<?>> tasks) {
        MaterialFamily.Recipes plan = family.recipes();
        Identifier ingot = itemId(family, MaterialFamily.Part.INGOT);
        Identifier block = blockId(family, MaterialFamily.Part.BLOCK);
        // 配方来源：自己的锭，或者 ingotFrom(...) 给的原版锭
        Identifier source = family.recipeSource() == null ? null : Registries.ITEM.getId(family.recipeSource());
        Identifier plate = itemId(family, MaterialFamily.Part.PLATE);
        Identifier dust = itemId(family, MaterialFamily.Part.DUST);
        Identifier raw = itemId(family, MaterialFamily.Part.RAW);
        Identifier nugget = itemId(family, MaterialFamily.Part.NUGGET);
        Identifier rawNugget = itemId(family, MaterialFamily.Part.RAW_NUGGET);

        if (plan.blockFromIngots && source != null && block != null) {
            tasks.add(recipe(writer, "crafting", id(family.id() + "_block"),
                    shaped3x3(source, "W", block, 1)));
        }
        if (plan.ingotsFromBlock && block != null && ingot != null) {
            tasks.add(recipe(writer, "crafting", id(family.id() + "_ingot_from_block"),
                    shapeless(List.of(block.toString()), ingot, 9, null, null)));
        }
        if (plan.plateFromIngot && source != null && plate != null) {
            tasks.add(recipe(writer, "crafting", id(family.id() + "_plate"),
                    shapeless(List.of(HAMMER.toString(), source.toString()), plate, plan.plateCount, null, null)));
        }
        if (plan.dustFromSource && dust != null) {
            if (source != null) {
                tasks.add(recipe(writer, "crafting", id(family.id() + "_dust"),
                        shapeless(List.of(MORTAR_TAG, source.toString()), dust, 1, null, null)));
            }
        }
        if (plan.rawToIngot && raw != null && ingot != null) {
            addCooking(writer, tasks, family, ingot, raw, false);
        }
        if (plan.dustToIngot && dust != null && ingot != null) {
            // 粗矿和粉都能烧的时候，粉用单独的配方名，避免和粗矿的 <材料>_ingot 撞车
            addCooking(writer, tasks, family, ingot, dust, plan.rawToIngot);
        }
        if (plan.nuggetFromIngot && nugget != null && ingot != null) {
            tasks.add(recipe(writer, "crafting", id(family.id() + "_nugget"),
                    shapeless(List.of(ingot.toString()), nugget, 9, "misc", null)));
            tasks.add(recipe(writer, "crafting", id(family.id() + "_ingot_from_nuggets"),
                    shaped3x3(nugget, "#", ingot, 1, "misc", null)));
        }
        if (plan.rawNuggetFromRaw && raw != null && rawNugget != null) {
            String group = "raw_" + family.id();
            tasks.add(recipe(writer, "crafting", id(raw.getPath()),
                    shapeless(List.of(raw.toString()), rawNugget, 9, "misc", group)));
            tasks.add(recipe(writer, "crafting", id(raw.getPath() + "_from_nuggets"),
                    shaped3x3(rawNugget, "#", raw, 1, "misc", group)));
        }
    }

    private void addCooking(DataWriter writer, List<CompletableFuture<?>> tasks, MaterialFamily family,
                            Identifier ingot, Identifier input, boolean rename) {
        MaterialFamily.Recipes plan = family.recipes();
        String base = rename ? ingot.getPath() + "_from_dust" : ingot.getPath();
        tasks.add(recipe(writer, "smelting", id(base),
                cooking("minecraft:smelting", input, ingot, plan.experience, plan.cookingTime)));
        tasks.add(recipe(writer, "blasting", id(base),
                cooking("minecraft:blasting", input, ingot, plan.experience, Math.max(1, plan.cookingTime / 2))));
    }

    /** 3×3 全是同一种材料（9 个锭压块、9 个粒压粗矿）。 */
    private static JsonObject shaped3x3(Identifier ingredient, String keyChar, Identifier result, int count) {
        return shaped3x3(ingredient, keyChar, result, count, null, null);
    }

    private static JsonObject shaped3x3(Identifier ingredient, String keyChar, Identifier result, int count,
                                        @Nullable String category, @Nullable String group) {
        JsonArray pattern = new JsonArray();
        pattern.add(keyChar + keyChar + keyChar);
        pattern.add(keyChar + keyChar + keyChar);
        pattern.add(keyChar + keyChar + keyChar);
        JsonObject key = new JsonObject();
        key.addProperty(keyChar, ingredient.toString());
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shaped");
        if (category != null) {
            root.addProperty("category", category);
        }
        if (group != null) {
            root.addProperty("group", group);
        }
        root.add("key", key);
        root.add("pattern", pattern);
        root.add("result", result(result, count));
        return root;
    }

    private static JsonObject shapeless(List<String> ingredients, Identifier result, int count,
                                        @Nullable String category, @Nullable String group) {
        JsonArray array = new JsonArray();
        ingredients.forEach(array::add);
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shapeless");
        if (category != null) {
            root.addProperty("category", category);
        }
        if (group != null) {
            root.addProperty("group", group);
        }
        root.add("ingredients", array);
        root.add("result", result(result, count));
        return root;
    }

    private static JsonObject cooking(String type, Identifier input, Identifier resultId, float experience, int time) {
        JsonObject root = new JsonObject();
        root.addProperty("type", type);
        root.addProperty("category", "misc");
        root.addProperty("cookingtime", time);
        root.addProperty("experience", experience);
        root.addProperty("ingredient", input.toString());
        root.add("result", result(resultId, 1));
        return root;
    }

    private static JsonObject result(Identifier item, int count) {
        JsonObject result = new JsonObject();
        result.addProperty("id", item.toString());
        result.addProperty("count", count);
        return result;
    }

    // ------------------------------------------------------------------
    // 写文件 + 语言检查
    // ------------------------------------------------------------------

    private CompletableFuture<?> asset(DataWriter writer, String directory, Identifier id, JsonObject json) {
        return DataProvider.writeToPath(writer, json,
                this.output.getResolver(DataOutput.OutputType.RESOURCE_PACK, directory).resolveJson(id));
    }

    private CompletableFuture<?> data(DataWriter writer, String directory, Identifier id, JsonObject json) {
        return DataProvider.writeToPath(writer, json,
                this.output.getResolver(DataOutput.OutputType.DATA_PACK, directory).resolveJson(id));
    }

    private CompletableFuture<?> recipe(DataWriter writer, String directory, Identifier id, JsonObject json) {
        return data(writer, "recipe/" + directory, id, json);
    }

    private static Identifier id(String path) {
        return Identifier.of(McDonaldsMod.MOD_ID, path);
    }

    private static Identifier partId(MaterialFamily family, MaterialFamily.Part part) {
        return id(family.id(part));
    }

    private static @Nullable Identifier itemId(MaterialFamily family, MaterialFamily.Part part) {
        return family.item(part) == null ? null : Registries.ITEM.getId(family.item(part));
    }

    private static @Nullable Identifier blockId(MaterialFamily family, MaterialFamily.Part part) {
        return family.block(part) == null ? null : Registries.BLOCK.getId(family.block(part));
    }

    private static Identifier blockModel(Identifier blockId) {
        return Identifier.of(blockId.getNamespace(), "block/" + blockId.getPath());
    }

    private static Identifier itemModel(Identifier itemId) {
        return Identifier.of(itemId.getNamespace(), "item/" + itemId.getPath());
    }

    private static JsonArray arrayOf(JsonElement... elements) {
        JsonArray array = new JsonArray();
        for (JsonElement element : elements) {
            array.add(element);
        }
        return array;
    }

    /**
     * 语言条目只提示不生成：把每个族缺的键打印出来，照着往
     * {@code src/main/resources/assets/mcdonalds-mod/lang/*.json} 里补。
     */
    private void reportMissingLang(List<MaterialFamily> families) {
        // 从 classpath 读（datagen 的工作目录是 build/datagen，用相对路径找不到源码目录）
        Map<String, String> zh = readLang("zh_cn.json");
        Map<String, String> en = readLang("en_us.json");
        if (zh.isEmpty() && en.isEmpty()) {
            McDonaldsMod.LOGGER.warn("[材料族] classpath 上找不到 lang 文件，跳过语言检查");
            return;
        }
        int missing = 0;
        for (MaterialFamily family : families) {
            for (MaterialFamily.LangEntry entry : family.langEntries()) {
                if (!zh.containsKey(entry.key())) {
                    McDonaldsMod.LOGGER.warn("[材料族] zh_cn.json 缺条目：\"{}\": \"{}\"", entry.key(), entry.zh());
                    missing++;
                }
                if (!en.containsKey(entry.key())) {
                    McDonaldsMod.LOGGER.warn("[材料族] en_us.json 缺条目：\"{}\": \"{}\"", entry.key(), entry.en());
                    missing++;
                }
            }
        }
        if (missing == 0) {
            McDonaldsMod.LOGGER.info("[材料族] 语言条目齐全");
        }
    }

    private static Map<String, String> readLang(String fileName) {
        Map<String, String> entries = new HashMap<>();
        String resource = "/assets/" + McDonaldsMod.MOD_ID + "/lang/" + fileName;
        InputStream stream = MaterialAssetsProvider.class.getResourceAsStream(resource);
        if (stream == null) {
            return entries;
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                entries.put(entry.getKey(), entry.getValue().getAsString());
            }
        } catch (IOException | IllegalStateException exception) {
            McDonaldsMod.LOGGER.warn("[材料族] 读取 {} 失败：{}", resource, exception.getMessage());
        }
        return entries;
    }
}
