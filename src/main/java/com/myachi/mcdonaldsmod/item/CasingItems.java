package com.myachi.mcdonaldsmod.item;

import com.myachi.mcdonaldsmod.McDonaldsMod;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 外壳（casing）。
 *
 * <p>注册名统一为 {@code <材料>_casing}，例如 {@code tin_casing}、{@code iron_refined_casing}。
 * 贴图放在 {@code resource/texture/item/casing/}，文件名为 {@code <贴图名>.png}
 * （精炼铁那张素材名是 {@code iron_refined.png}，注册名仍然是 {@code iron_refined_casing}）。
 *
 * <p>命名规则（用户指定）：
 * <ul>
 *     <li>材料名多于一个字：<b>x 外壳</b>，例如 青铜外壳、精炼铁外壳、铝外壳、石墨烯外壳；</li>
 *     <li>材料名只有一个字：<b>x 质外壳</b>，例如 锡质外壳、铅质外壳、铜质外壳、金质外壳、钢质外壳、钨质外壳。</li>
 * </ul>
 * 英文一律 {@code <Material> Casing}。
 *
 * <p>资源（物品定义 / 模型 / 语言）由 {@code datagen/CasingAssetsProvider} 生成。
 */
public final class CasingItems {
    /**
     * 一条外壳。
     *
     * @param material  材料 id（= 注册名前缀，也是贴图基础名）
     * @param texture   贴图文件名（不含 .png）；精炼铁这类素材名与注册名不同的才需要单独指定
     * @param zhName    中文材料名
     * @param enName    英文材料名
     */
    public record Entry(String material, String texture, String zhName, String enName) {
        public String id() {
            return this.material + "_casing";
        }

        /**
         * 中文名：材料名只有一个字时写成"x质外壳"，否则写"x外壳"。
         */
        public String zhDisplay() {
            return this.zhName.length() == 1 ? this.zhName + "质外壳" : this.zhName + "外壳";
        }

        public String enDisplay() {
            return this.enName + " Casing";
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<String, Item> BY_MATERIAL = new LinkedHashMap<>();
    private static boolean initialized;

    private CasingItems() {
    }

    /** 全部外壳（顺序 = 物品栏顺序）。 */
    public static List<Entry> all() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static Item get(String material) {
        return BY_MATERIAL.get(material);
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        // 材料 id, 贴图名, 中文材料名, 英文材料名
        add("tin", "tin", "锡", "Tin");
        add("lead", "lead", "铅", "Lead");
        add("copper", "copper", "铜", "Copper");
        add("gold", "gold", "金", "Gold");
        add("iron", "iron", "铁", "Iron");
        add("steel", "steel", "钢", "Steel");
        add("aluminum", "aluminum", "铝", "Aluminum");
        add("silver", "silver", "银", "Silver");
        add("bronze", "bronze", "青铜", "Bronze");
        add("iron_refined", "iron_refined", "精炼铁", "Refined Iron");
        add("tungsten", "tungsten", "钨", "Tungsten");
        add("tungsten_steel", "tungsten_steel", "钨钢", "Tungsten Steel");
        add("graphene", "graphene", "石墨烯", "Graphene");

        McDonaldsMod.LOGGER.info("Registry CasingItems! {} 种外壳", ENTRIES.size());
    }

    private static void add(String material, String texture, String zh, String en) {
        Entry entry = new Entry(material, texture, zh, en);
        ENTRIES.add(entry);
        BY_MATERIAL.put(material, register(entry.id()));
    }

    private static Item register(String path) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(McDonaldsMod.MOD_ID, path));
        return Items.register(key, Item::new, new Item.Settings());
    }
}
