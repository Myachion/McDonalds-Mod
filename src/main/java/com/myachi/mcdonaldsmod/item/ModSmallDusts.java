package com.myachi.mcdonaldsmod.item;

import com.myachi.mcdonaldsmod.McDonaldsMod;
import com.myachi.mcdonaldsmod.ModItems;
import com.myachi.mcdonaldsmod.material.MaterialFamily;
import com.myachi.mcdonaldsmod.material.ModMaterials;
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
 * "小撮粉"：每种粉都配一个 1/9 的小份，**9 小撮粉 ↔ 1 粉**双向配方。
 *
 * <p>贴图由用户提供，放在 {@code resource/texture/item/small_dust/}，
 * 按 {@code small_<粉>_dust.png} 命名装进
 * {@code src/main/resources/assets/mcdonalds-mod/textures/item/}。
 * 物品模型、物品定义、两条配方由 {@code datagen/SmallDustAssetsProvider} 生成。
 *
 * <p><b>特例</b>：盐和糖没有自己的"粉"物品 —— 小盐粉对应 {@code mcdonalds-mod:salt}（食盐）、
 * 小糖粉对应原版 {@code minecraft:sugar}，这两条的注册名在下面显式指定。
 */
public final class ModSmallDusts {
    /** 粉/来源物品（命名空间 id）→ 小撮粉物品；按登记顺序排列，物品栏也照这个顺序。 */
    private static final Map<Identifier, Item> SMALL_DUSTS = new LinkedHashMap<>();
    /** 粉/来源物品 → 大件本体（配方里的原料）。 */
    private static final Map<Identifier, Item> SOURCES = new LinkedHashMap<>();
    /** 粉/来源物品 → 小撮粉的注册名（盐/糖不是按 {@code small_<粉名>} 拼的）。 */
    private static final Map<Identifier, String> PATHS = new LinkedHashMap<>();

    private ModSmallDusts() {
    }

    /** 所有小撮粉的注册名 → 物品（不可变，按登记顺序）。 */
    public static Map<Identifier, Item> all() {
        return Collections.unmodifiableMap(SMALL_DUSTS);
    }

    /** 小撮粉的注册名（路径部分）。datagen 以它为准决定文件名，不自己拼。 */
    public static String pathOf(Identifier dustId) {
        return PATHS.get(dustId);
    }

    /** 小撮粉对应的"粉"（配方里的原料）。 */
    public static Item sourceOf(Identifier dustId) {
        return SOURCES.get(dustId);
    }

    /** 按登记顺序返回所有小撮粉物品（物品栏用）。 */
    public static List<Item> items() {
        return new ArrayList<>(SMALL_DUSTS.values());
    }

    public static int count() {
        return SMALL_DUSTS.size();
    }

    /**
     * 登记全部小撮粉。必须在 {@link ModMaterials#initialize()} 与 {@link ModItems#initializeMod()}
     * 之后调用（要用到那些物品实例）。
     */
    public static void initialize() {
        // 1) 材料族的粉：自动跟着走
        for (MaterialFamily family : ModMaterials.all()) {
            Item dust = family.dust();
            if (dust != null) {
                add(dust);
            }
        }

        // 2) 非材料族的粉
        add(ModItems.COPPER_DUST);
        add(ModItems.IRON_DUST);
        add(ModItems.GOLD_DUST);
        add(ModItems.COAL_DUST);
        add(ModItems.CARBON_DUST);
        add(ModItems.CLAY_DUST);
        add(ModItems.DIAMOND_DUST);
        add(ModItems.END_STONE_DUST);
        add(ModItems.LAPIS_DUST);
        add(ModItems.LITHIUM_DUST);
        add(ModItems.OBSIDIAN_DUST);
        add(ModItems.QUARTZ_DUST);
        add(ModItems.SILICON_DIOXIDE_DUST);
        add(ModItems.SULFUR_DUST);
        add(ModItems.BERYLLIUM_DUST);
        add(ModItems.IRIDIUM_DUST);
        add(ModItems.FLINT_DUST);

        // 3) 盐和糖用本体当大件
        add(ModItems.SALT, "small_salt_dust");
        add(Items.SUGAR, "small_sugar_dust");

        McDonaldsMod.LOGGER.info("Registry ModSmallDusts! {} 种小撮粉", SMALL_DUSTS.size());
    }

    /** 默认命名：{@code small_<大件的注册名>}。 */
    private static void add(Item source) {
        Identifier id = net.minecraft.registry.Registries.ITEM.getId(source);
        if (id == null) {
            return;
        }
        add(source, "small_" + id.getPath());
    }

    /** 指定注册名（盐/糖这类命名对不上的）。 */
    private static void add(Item source, String smallPath) {
        Identifier id = net.minecraft.registry.Registries.ITEM.getId(source);
        if (id == null || SMALL_DUSTS.containsKey(id)) {
            return;
        }
        SMALL_DUSTS.put(id, register(smallPath));
        SOURCES.put(id, source);
        PATHS.put(id, smallPath);
    }

    private static Item register(String path) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(McDonaldsMod.MOD_ID, path));
        return Items.register(key, Item::new, new Item.Settings());
    }
}
