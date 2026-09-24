package com.myachi.mcdonaldsmod.worldgen;

import com.myachi.mcdonaldsmod.McDonaldsMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * 盐矿的世界生成注入。
 *
 * <p>矿脉形态与投放密度全部由数据包描述，本类只负责把三档 placed_feature
 * 挂到对应的生物群系标签上：
 * <ul>
 *   <li>{@code data/mcdonalds-mod/worldgen/configured_feature/salt_ore.json}</li>
 *   <li>{@code data/mcdonalds-mod/worldgen/placed_feature/salt_ore_dense.json}</li>
 *   <li>{@code data/mcdonalds-mod/worldgen/placed_feature/salt_ore_normal.json}</li>
 *   <li>{@code data/mcdonalds-mod/worldgen/placed_feature/salt_ore_sparse.json}</li>
 *   <li>{@code data/mcdonalds-mod/tags/worldgen/biome/salt_ore_*.json}</li>
 * </ul>
 *
 * <p>三档生物群系标签必须保持互斥，否则同一个生物群系会同时拿到两档的
 * count，实际密度会叠加。
 */
public final class ModOreGeneration {
    /** 高盐：海洋、河流与海滩、干旱盐碱地。count 16 / size 9。 */
    public static final RegistryKey<PlacedFeature> SALT_ORE_DENSE = placedFeature("salt_ore_dense");
    /** 中盐：温带、山地与洞穴生物群系。count 10 / size 9。 */
    public static final RegistryKey<PlacedFeature> SALT_ORE_NORMAL = placedFeature("salt_ore_normal");
    /** 低盐：丛林、沼泽、蘑菇岛与极寒生物群系。count 4 / size 9。 */
    public static final RegistryKey<PlacedFeature> SALT_ORE_SPARSE = placedFeature("salt_ore_sparse");

    public static final TagKey<Biome> DENSE_BIOMES = biomeTag("salt_ore_dense");
    public static final TagKey<Biome> NORMAL_BIOMES = biomeTag("salt_ore_normal");
    public static final TagKey<Biome> SPARSE_BIOMES = biomeTag("salt_ore_sparse");

    private ModOreGeneration() {
    }

    public static void initialize() {
        addFeature(DENSE_BIOMES, SALT_ORE_DENSE);
        addFeature(NORMAL_BIOMES, SALT_ORE_NORMAL);
        addFeature(SPARSE_BIOMES, SALT_ORE_SPARSE);

        McDonaldsMod.LOGGER.info("Registry ModOreGeneration!");
    }

    private static void addFeature(TagKey<Biome> biomes, RegistryKey<PlacedFeature> placedFeature) {
        BiomeModifications.addFeature(
                BiomeSelectors.tag(biomes),
                GenerationStep.Feature.UNDERGROUND_ORES,
                placedFeature
        );
    }

    private static RegistryKey<PlacedFeature> placedFeature(String path) {
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(McDonaldsMod.MOD_ID, path));
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.of(RegistryKeys.BIOME, Identifier.of(McDonaldsMod.MOD_ID, path));
    }
}
