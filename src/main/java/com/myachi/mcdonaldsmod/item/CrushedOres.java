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
 * 粉碎矿（crushed ore）与纯净粉碎矿（purified crushed ore）。
 *
 * <p>17 级加工链的第一步：矿石 → 粗矿 →（研钵）→ <b>粉碎矿</b> →（以后）→ 纯净粉碎矿 → 粉。
 * 本类只负责登记物品；物品定义 / 模型 / 配方由
 * {@code datagen/CrushedOreAssetsProvider} 生成。
 *
 * <table>
 *   <tr><th>注册名</th><th>中文</th><th>英文</th></tr>
 *   <tr><td>{@code crushed_<材料>}</td><td>粉碎 x 矿石</td><td>Crushed x Ore</td></tr>
 *   <tr><td>{@code pure_crushed_<材料>}</td><td>纯净的粉碎 x 矿石</td><td>Purified Crushed x Ore</td></tr>
 * </table>
 *
 * <p>铜 / 铁 / 金用的是**原版矿石**，所以它们的粉碎矿也保留原版的中文名
 * （粉碎铜矿石、粉碎铁矿石、粉碎金矿石）。
 */
public final class CrushedOres {
    /** 一条粉碎矿：材料 id、中文名、英文名、粗矿原料。 */
    public record Entry(String material, String zh, String en, Item rawSource) {
        public String crushedId() {
            return "crushed_" + this.material;
        }

        public String pureId() {
            return "pure_crushed_" + this.material;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<String, Item> CRUSHED = new LinkedHashMap<>();
    private static final Map<String, Item> PURE = new LinkedHashMap<>();
    private static boolean initialized;

    private CrushedOres() {
    }

    /** 所有条目（顺序 = 物品栏顺序）。未初始化时为空。 */
    public static List<Entry> all() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static Item crushed(String material) {
        return CRUSHED.get(material);
    }

    public static Item pure(String material) {
        return PURE.get(material);
    }

    /** 登记物品 + 建立"材料 -> 条目"的查找表。 */
    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        // 材料 id, 中文, 英文, 粗矿原料（铜/铁/金用原版粗矿）
        add("tin", "锡", "Tin", null);
        add("lead", "铅", "Lead", null);
        add("aluminum", "铝", "Aluminum", null);
        add("silver", "银", "Silver", null);
        add("uranium", "铀", "Uranium", null);
        add("tungsten", "钨", "Tungsten", null);
        add("copper", "铜", "Copper", Items.RAW_COPPER);
        add("iron", "铁", "Iron", Items.RAW_IRON);
        add("gold", "金", "Gold", Items.RAW_GOLD);

        McDonaldsMod.LOGGER.info("Registry CrushedOres! {} 种粉碎矿 + {} 种纯净粉碎矿",
                CRUSHED.size(), PURE.size());
    }

    private static void add(String material, String zh, String en, Item vanillaRaw) {
        Item raw = vanillaRaw;
        if (raw == null) {
            // 模组自己的粗矿：raw_<材料>，在 ModItems / 材料族里注册
            raw = net.minecraft.registry.Registries.ITEM.get(
                    Identifier.of(McDonaldsMod.MOD_ID, "raw_" + material));
        }
        Entry entry = new Entry(material, zh, en, raw);
        ENTRIES.add(entry);
        CRUSHED.put(material, register(entry.crushedId()));
        PURE.put(material, register(entry.pureId()));
    }

    private static Item register(String path) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(McDonaldsMod.MOD_ID, path));
        return Items.register(key, Item::new, new Item.Settings());
    }
}
