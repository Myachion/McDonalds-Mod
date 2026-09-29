package com.myachi.mcdonaldsmod.datagen;

import com.google.gson.JsonObject;
import com.myachi.mcdonaldsmod.McDonaldsMod;
import com.myachi.mcdonaldsmod.item.CasingItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 外壳的资源生成：物品定义 + 物品模型。
 *
 * <p>贴图是用户手绘的，放在 {@code resource/texture/item/casing/}，
 * 所以这里只生成 1.21.4+ 需要的两份 JSON：
 * <ul>
 *     <li>{@code assets/mcdonalds-mod/items/<id>.json}（物品定义）</li>
 *     <li>{@code assets/mcdonalds-mod/models/item/<id>.json}（模型，layer0 指向 {@code mcdonalds-mod:item/<贴图名>}）</li>
 * </ul>
 *
 * <p>注意 layer0 的路径要写成 {@code <ns>:item/<贴图名>} —— 命名空间相对路径会被解析成
 * {@code textures/<路径>.png}，写错就会变成紫黑"缺失贴图"。
 */
public final class CasingAssetsProvider implements DataProvider {
    private final FabricDataOutput output;

    public CasingAssetsProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public String getName() {
        return "Casing assets (definitions / models)";
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        for (CasingItems.Entry entry : CasingItems.all()) {
            Identifier id = Identifier.of(McDonaldsMod.MOD_ID, entry.id());
            // 贴图放在 textures/item/casing/ 子目录里，所以 layer0 要带 casing/ 前缀；
            // 贴图名可能和注册名不同（例如精炼铁的素材是 iron_refined.png）。
            // 注意：layer0 的路径是"命名空间相对路径"，会被解析成 textures/<路径>.png
            Identifier texture = Identifier.of(McDonaldsMod.MOD_ID, "item/casing/" + entry.texture());
            // 模型 id 固定 = 物品 id（模型文件写在 models/item/<id>.json）
            Identifier model = Identifier.of(McDonaldsMod.MOD_ID, "item/" + entry.id());
            tasks.add(asset(writer, "models/item", id, itemModel(texture)));
            tasks.add(asset(writer, "items", id, itemDefinition(model)));
        }
        McDonaldsMod.LOGGER.info("[外壳] 生成 {} 种外壳的物品定义与模型", CasingItems.all().size());
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    private static JsonObject itemModel(Identifier texture) {
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", texture.toString());
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:item/generated");
        root.add("textures", textures);
        return root;
    }

    /**
     * 1.21.4+ 的物品定义：引用一个模型 id（命名空间相对路径，不含 .json）。
     *
     * @param model 模型 id，形如 {@code mcdonalds-mod:item/tin_casing}
     */
    private static JsonObject itemDefinition(Identifier model) {
        JsonObject inner = new JsonObject();
        inner.addProperty("type", "minecraft:model");
        inner.addProperty("model", model.toString());
        JsonObject root = new JsonObject();
        root.add("model", inner);
        return root;
    }

    private CompletableFuture<?> asset(DataWriter writer, String directory, Identifier id, JsonObject json) {
        return DataProvider.writeToPath(writer, json,
                this.output.getResolver(DataOutput.OutputType.RESOURCE_PACK, directory).resolveJson(id));
    }
}
