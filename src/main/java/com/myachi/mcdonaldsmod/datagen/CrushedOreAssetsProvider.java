package com.myachi.mcdonaldsmod.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.myachi.mcdonaldsmod.McDonaldsMod;
import com.myachi.mcdonaldsmod.item.CrushedOres;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 粉碎矿的资源生成：物品定义、物品模型，以及<b>粗矿 + 研钵 → 粉碎矿</b>的配方。
 *
 * <p>清单来自 {@link CrushedOres}。配方和 mod 里板/粉的写法一致：研钵用标签
 * {@code #mcdonalds-mod:mortar}（石/精炼铁/下界合金研钵都算），无序合成。
 *
 * <p>输出：
 * <ul>
 *     <li>{@code assets/mcdonalds-mod/items/<id>.json} + {@code models/item/<id>.json}</li>
 *     <li>{@code data/mcdonalds-mod/recipe/crafting/<id>.json}</li>
 * </ul>
 */
public final class CrushedOreAssetsProvider implements DataProvider {
    /** 研钵标签，配方里写成 {@code #mcdonalds-mod:mortar}。 */
    private static final String MORTAR_TAG = "#" + McDonaldsMod.MOD_ID + ":mortar";

    private final FabricDataOutput output;

    public CrushedOreAssetsProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public String getName() {
        return "Crushed ore assets (definitions / models / recipes)";
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        int recipes = 0;
        for (CrushedOres.Entry entry : CrushedOres.all()) {
            for (String path : new String[]{entry.crushedId(), entry.pureId()}) {
                Identifier id = Identifier.of(McDonaldsMod.MOD_ID, path);
                Identifier model = Identifier.of(id.getNamespace(), "item/" + path);
                tasks.add(asset(writer, "models/item", id, itemModel(model)));
                tasks.add(asset(writer, "items", id, itemDefinition(model)));
            }
            // 粗矿 + 研钵 → 粉碎矿（无序）
            if (entry.rawSource() != null) {
                tasks.add(recipe(writer, entry.crushedId(), shapeless(
                        List.of(MORTAR_TAG, net.minecraft.registry.Registries.ITEM.getId(entry.rawSource()).toString()),
                        Identifier.of(McDonaldsMod.MOD_ID, entry.crushedId()), 1)));
                recipes++;
            }
        }
        McDonaldsMod.LOGGER.info("[粉碎矿] 生成 {} 条物品资源 + {} 条合成配方",
                CrushedOres.all().size() * 2, recipes);
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    // ------------------------------------------------------------------
    // JSON 写法
    // ------------------------------------------------------------------

    private static JsonObject shapeless(List<String> ingredients, Identifier result, int count) {
        JsonArray array = new JsonArray();
        ingredients.forEach(array::add);
        JsonObject res = new JsonObject();
        res.addProperty("id", result.toString());
        res.addProperty("count", count);
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shapeless");
        root.add("ingredients", array);
        root.add("result", res);
        return root;
    }

    private static JsonObject itemModel(Identifier texture) {
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", texture.toString());
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:item/generated");
        root.add("textures", textures);
        return root;
    }

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
