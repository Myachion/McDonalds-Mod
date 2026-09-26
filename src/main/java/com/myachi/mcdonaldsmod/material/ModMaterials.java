package com.myachi.mcdonaldsmod.material;

import com.myachi.mcdonaldsmod.McDonaldsMod;
import net.minecraft.block.MapColor;

import java.util.List;

/**
 * 所有"材料族"的定义表。加一种新材料 = 在这里加一条。
 *
 * <p>当前迁移状态（见 {@code docs/material-guide.md}）：
 * <ul>
 *     <li><b>锡</b>：Java 注册 + 模型 + 掉落 + 配方全部由材料族生成（第一个完整迁移的样板）；</li>
 *     <li>铅 / 铝 / 银 / 青铜 / 铀 / 钢块：Java 注册已经统一到这里，
 *         但资源（模型/掉落/配方）仍是手写 JSON，所以标了 {@code noGeneratedAssets()}；
 *         要迁移的话，删掉对应的手写文件、去掉这个开关、跑 {@code runDatagen} 即可。</li>
 * </ul>
 *
 * <p>反例写法示例（都能在这个文件里看到）：
 * <ul>
 *     <li>铀没有板 → 不写 {@code .plate()}；</li>
 *     <li>青铜没有粗矿/矿石 → 不写 {@code .raw()} / {@code .ore(...)}；</li>
 *     <li>银、铝的粒挂在粗矿上 → {@code .rawNugget()} 而不是 {@code .nugget()}；</li>
 *     <li>钢块暂时没有配方 → {@code .blockRecipe(false)}。</li>
 * </ul>
 */
public final class ModMaterials {
    /** 锡：完整的标准金属（锭/板/粉/块/粗矿/矿石）。资源已全量迁移到材料族生成。 */
    public static final MaterialFamily TIN = MaterialFamily.builder("tin")
            .mapColor(MapColor.WHITE_GRAY)
            .name("锡", "Tin")
            .ingot().plate().dust().block().raw().ore(0, 2)
            .build();

    /** 铅：比锡多了深层矿。 */
    public static final MaterialFamily LEAD = MaterialFamily.builder("lead")
            .mapColor(MapColor.DEEPSLATE_GRAY)
            .name("铅", "Lead")
            .ingot().plate().dust().block().raw().ore(0, 2).deepslateOre(0, 2)
            .noGeneratedAssets()
            .build();

    /** 铝：没有深层矿，下界矿掉粗铝粒。 */
    public static final MaterialFamily ALUMINUM = MaterialFamily.builder("aluminum")
            .mapColor(MapColor.LIGHT_BLUE_GRAY)
            .name("铝", "Aluminum")
            .ingot().plate().dust().block().raw().rawNugget().ore(0, 2).netherOre(0, 2)
            .noGeneratedAssets()
            .build();

    /** 银：主世界 + 深层 + 下界三种矿，下界矿掉粗银粒。 */
    public static final MaterialFamily SILVER = MaterialFamily.builder("silver")
            .mapColor(MapColor.LIGHT_GRAY)
            .name("银", "Silver")
            .ingot().plate().dust().block().raw().rawNugget()
            .ore(0, 3).deepslateOre(0, 3).netherOre(0, 3)
            .noGeneratedAssets()
            .build();

    /** 青铜：合金，没有粗矿和矿石；锭由青铜粉烧出来。 */
    public static final MaterialFamily BRONZE = MaterialFamily.builder("bronze")
            .mapColor(MapColor.ORANGE)
            .name("青铜", "Bronze")
            .ingot().plate().dust().block()
            .dustSmelting(true)   // 反例：只有青铜粉能烧回锭
            .noGeneratedAssets()
            .build();

    /** 铀：没有板（反例：缺部件）。 */
    public static final MaterialFamily URANIUM = MaterialFamily.builder("uranium")
            .mapColor(MapColor.LICHEN_GREEN)
            .name("铀", "Uranium")
            .ingot().dust().block().raw().ore(0, 4).deepslateOre(0, 4)
            .noGeneratedAssets()
            .build();

    /** 钢块：只有方块、暂时没有配方（反例：blockRecipe(false)）。钢锭还没加。 */
    public static final MaterialFamily STEEL = MaterialFamily.builder("steel")
            .mapColor(MapColor.IRON_GRAY)
            .name("钢", "Steel")
            .block().blockRecipe(false)
            .noGeneratedAssets()
            .build();

    private ModMaterials() {
    }

    /** 所有材料族（datagen 和物品栏用这个遍历）。 */
    public static List<MaterialFamily> all() {
        return List.of(TIN, LEAD, ALUMINUM, SILVER, BRONZE, URANIUM, STEEL);
    }

    public static void initialize() {
        McDonaldsMod.LOGGER.info("Registry ModMaterials! {} families", all().size());
    }
}
