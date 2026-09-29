package com.myachi.mcdonaldsmod.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.myachi.mcdonaldsmod.McDonaldsMod;
import com.myachi.mcdonaldsmod.item.ModSmallDusts;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 粉类的资源生成，两类东西：
 *
 * <h2>1. 补出来的大粉的物品模型</h2>
 * {@code sulfur_dust / beryllium_dust / iridium_dust / flint_dust / energium_dust} 这几个粉是
 * 后来补的、**没有手写模型文件**（早先那批粉的模型在 {@code resources/models/item/} 里），
 * 不生成的话物品栏里就是紫黑块。
 *
 * <h2>2. 小撮粉的物品定义 / 模型 / 配方</h2>
 * 清单来自 {@link ModSmallDusts}：9 小撮粉 ↔ 1 粉 双向；加一种粉就自动多一套资源。
 * 输出到 {@code src/main/generated}。
 */
public final class SmallDustAssetsProvider implements DataProvider {
    /** 需要生成模型的大粉（注册名）。 */
    private static final List<String> BASE_DUSTS = List.of(
            "sulfur_dust", "beryllium_dust", "iridium_dust", "flint_dust", "energium_dust");

    private final FabricDataOutput output;

    public SmallDustAssetsProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public String getName() {
        return "Dust assets (item models / small dust definitions, models, recipes)";
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        List<CompletableFuture<?>> tasks = new ArrayList<>();

        // ---- 1. 大粉：物品定义 + 模型（两样都写，1.21.4+ 最稳） ----
        for (String path : BASE_DUSTS) {
            Identifier id = Identifier.of(McDonaldsMod.MOD_ID, path);
            Identifier model = Identifier.of(id.getNamespace(), "item/" + path);
            tasks.add(asset(writer, "items", id, itemDefinition(model)));
            tasks.add(asset(writer, "models/item", id, itemModel(model)));
        }

        // ---- 2. 小撮粉：物品定义 + 模型 + 两条配方 ----
        for (Map.Entry<Identifier, ?> entry : ModSmallDusts.all().entrySet()) {
            Identifier dustId = entry.getKey();
            // 注册名以 ModSmallDusts 为准（盐/糖不是按 small_<粉名> 拼的）
            String smallPath = ModSmallDusts.pathOf(dustId);
            Identifier smallId = Identifier.of(McDonaldsMod.MOD_ID, smallPath);
            Identifier smallModel = Identifier.of(smallId.getNamespace(), "item/" + smallPath);

            tasks.add(asset(writer, "models/item", smallId, itemModel(smallModel)));
            tasks.add(asset(writer, "items", smallId, itemDefinition(smallModel)));
            // 9 小撮粉 → 1 粉（3×3 有序） / 1 粉 → 9 小撮粉（无序）
            tasks.add(recipe(writer, smallPath, threeByThree(smallId, dustId)));
            tasks.add(recipe(writer, smallPath + "_from_dust", shapeless(dustId, smallId)));
        }

        McDonaldsMod.LOGGER.info("[粉] 生成 {} 个大粉 + {} 种小撮粉的资源",
                BASE_DUSTS.size(), ModSmallDusts.count());
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    // ------------------------------------------------------------------
    // 配方 / 模型写法
    // ------------------------------------------------------------------

    /** 9 个小撮粉 → 1 个粉：3×3 有序配方（和原版"9 个粒压锭"同形状）。 */
    private static JsonObject threeByThree(Identifier small, Identifier dust) {
        JsonArray pattern = new JsonArray();
        pattern.add("###");
        pattern.add("###");
        pattern.add("###");
        JsonObject key = new JsonObject();
        key.addProperty("#", small.toString());
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shaped");
        root.add("key", key);
        root.add("pattern", pattern);
        root.add("result", result(dust, 1));
        return root;
    }

    /** 1 个粉 → 9 个小撮粉：无序配方。 */
    private static JsonObject shapeless(Identifier dust, Identifier small) {
        JsonArray ingredients = new JsonArray();
        ingredients.add(dust.toString());
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shapeless");
        root.add("ingredients", ingredients);
        root.add("result", result(small, 9));
        return root;
    }

    private static JsonObject result(Identifier item, int count) {
        JsonObject result = new JsonObject();
        result.addProperty("id", item.toString());
        result.addProperty("count", count);
        return result;
    }

    /** 一层贴图的普通物品模型；贴图在 {@code assets/<ns>/textures/item/<id>.png}。 */
    private static JsonObject itemModel(Identifier texture) {
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", texture.toString());
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:item/generated");
        root.add("textures", textures);
        return root;
    }

    /** 1.21.4+ 的物品定义。 */
    private static JsonObject itemDefinition(Identifier model) {
        JsonObject inner = new JsonObject();
        inner.addProperty("type", "minecraft:model");
        inner.addProperty("model", model.toString());
        JsonObject root = new JsonObject();
        root.add("model", inner);
        return root;
    }

    // ------------------------------------------------------------------
    // 写文件
    // ------------------------------------------------------------------

    private CompletableFuture<?> asset(DataWriter writer, String directory, Identifier id, JsonObject json) {
        return DataProvider.writeToPath(writer, json,
                this.output.getResolver(DataOutput.OutputType.RESOURCE_PACK, directory).resolveJson(id));
    }

    private CompletableFuture<?> recipe(DataWriter writer, String name, JsonObject json) {
        return DataProvider.writeToPath(writer, json,
                this.output.getResolver(DataOutput.OutputType.DATA_PACK, "recipe/crafting")
                        .resolveJson(Identifier.of(McDonaldsMod.MOD_ID, name)));
    }
}
