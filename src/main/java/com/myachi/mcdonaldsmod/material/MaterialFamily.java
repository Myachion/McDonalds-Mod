package com.myachi.mcdonaldsmod.material;

import com.myachi.mcdonaldsmod.ModBlocks;
import com.myachi.mcdonaldsmod.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.MapColor;
import net.minecraft.item.Item;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 一个"材料族"：同一种材料的 锭 / 粒 / 板 / 粉 / 粗矿 / 粗矿粒 / 材料块 / 矿石 一次登记好。
 *
 * <h2>为什么要有这个类</h2>
 * 这个 mod 里的材料（锡、铅、铝、银、铜、青铜、铀…）都是同一个模子刻出来的：
 * 一个锭 + 板 + 粉 + 9 锭↔1 块的双向配方 + 粉/粗矿烧成锭 + 矿石掉落粗矿。
 * 以前每加一种材料要改 4 个 Java 文件、写十几个 JSON，很容易漏。
 * 现在只要在 {@link ModMaterials} 里写一条：
 *
 * <pre>{@code
 * public static final MaterialFamily NICKEL = MaterialFamily.builder("nickel")
 *         .mapColor(MapColor.LIGHT_GRAY)
 *         .name("镍", "Nickel")           // 只用于 datagen 的"缺语言条目"提示
 *         .ingot().plate().dust().block().raw().ore(0, 2)
 *         .build();
 * }</pre>
 *
 * 然后跑数据生成（{@code .\gradlew.bat runDatagen}）就会补齐模型 / 方块状态 / 物品定义 /
 * 掉落表 / 配方；Java 侧的物品和方块注册在 {@link #build()} 时自动完成。
 *
 * <h2>反例（不标准的材料）怎么处理</h2>
 * <ul>
 *     <li><b>某个部件没有</b>（例如铀没有板）：不调用对应的 {@code .plate()} 就行，
 *         相关配方也会自动不生成；</li>
 *     <li><b>有部件但暂时没有配方</b>（钢块、钻石板）：{@code .blockRecipe(false)} /
 *         {@code .plateRecipe(false)}；</li>
 *     <li><b>配方来源不是自己的锭</b>（铜/金/铁板用的是原版锭）：{@code .ingotFrom(Items.COPPER_INGOT)}；</li>
 *     <li><b>粉的来源是别的物品</b>（青金石粉用青金石）：{@code .dustFrom(Items.LAPIS_LAZULI)}；</li>
 *     <li><b>粉/粗矿不能烧回锭</b>：{@code .dustSmelting(false)} / {@code .rawSmelting(false)}；</li>
 *     <li><b>整族或某个部件的资源还是手写的</b>（迁移期间）：
 *         {@code .noGeneratedAssets()} / {@code .handWritten(Part.ORE, Part.DEEPSLATE_ORE)} ——
 *         datagen 会跳过它们，绝不覆盖手写文件；</li>
 *     <li><b>粗矿粒挂在粗矿上而不是锭上</b>（银/铝下界矿那种）：用 {@code .rawNugget()} 而不是 {@code .nugget()}。</li>
 * </ul>
 *
 * <p>矿石的<b>世界生成</b>（密度/高度/生物群系）不在这里管 —— 那是用户拍板的数值，
 * 仍然写在 {@code worldgen/} + {@code data/mcdonalds-mod/worldgen/} 里。
 */
public final class MaterialFamily {
    /** 材料的部件。id 的拼法见 {@link #id(Part)}。 */
    public enum Part {
        /** 锭：{@code <材料>_ingot}。 */
        INGOT,
        /** 粒：{@code <材料>_nugget}（9 粒 ↔ 1 锭）。 */
        NUGGET,
        /** 板：{@code <材料>_plate}（默认配方：精炼铁锤 + 锭 → 2 板）。 */
        PLATE,
        /** 粉：{@code <材料>_dust}（默认配方：研钵 + 锭 → 1 粉）。 */
        DUST,
        /** 粗矿：{@code raw_<材料>}。 */
        RAW,
        /** 粗矿粒：{@code raw_<材料>_nugget}（9 粒 ↔ 1 粗矿）。 */
        RAW_NUGGET,
        /** 材料块：{@code <材料>_block}。 */
        BLOCK,
        /** 石头里的矿石：{@code <材料>_ore}。 */
        ORE,
        /** 深板岩矿石：{@code deepslate_<材料>_ore}。 */
        DEEPSLATE_ORE,
        /** 下界矿石：{@code nether_<材料>_ore}。 */
        NETHER_ORE;

        public boolean isOre() {
            return this == ORE || this == DEEPSLATE_ORE || this == NETHER_ORE;
        }

        public boolean isBlock() {
            return this == BLOCK || isOre();
        }
    }

    /** 一条材料族要生成哪些配方、参数是多少。默认值在 {@link Builder#build()} 里按部件推导。 */
    public static final class Recipes {
        /** 9 锭 → 1 块。 */
        public boolean blockFromIngots;
        /** 1 块 → 9 锭。 */
        public boolean ingotsFromBlock;
        /** 锤子 + 锭 → 板。 */
        public boolean plateFromIngot;
        /** 研钵 + 来源 → 粉。 */
        public boolean dustFromSource;
        /** 粉 →(熔炉/高炉) 锭。 */
        public boolean dustToIngot;
        /** 粗矿 →(熔炉/高炉) 锭。 */
        public boolean rawToIngot;
        /** 1 锭 → 9 粒。 */
        public boolean nuggetFromIngot;
        /** 9 粒 → 1 锭。 */
        public boolean ingotsFromNuggets;
        /** 1 粗矿 → 9 粗矿粒。 */
        public boolean rawNuggetFromRaw;
        /** 9 粗矿粒 → 1 粗矿。 */
        public boolean rawFromRawNuggets;
        /** 一次锤出几块板（默认 2）。 */
        public int plateCount = 2;
        /** 熔炼经验（默认 0.7）。 */
        public float experience = 0.7F;
        /** 熔炉烧制时间（tick，默认 200；高炉自动取一半）。 */
        public int cookingTime = 200;
    }

    private final String id;
    private final MapColor mapColor;
    private final String zhName;
    private final String enName;
    private final EnumMap<Part, Item> items = new EnumMap<>(Part.class);
    private final EnumMap<Part, Block> blocks = new EnumMap<>(Part.class);
    private final EnumMap<Part, int[]> oreExperience = new EnumMap<>(Part.class);
    private final Recipes recipes;
    private final Set<Part> handWritten;
    /** 不注册自己的锭时，锭的替代来源（原版铜锭/金锭/铁锭那种）。 */
    private final @Nullable Item ingotSource;
    /** 锭从哪来：自己的锭，或者 {@link #ingotSource}。 */
    private final @Nullable Item recipeSource;

    private MaterialFamily(Builder builder) {
        this.id = builder.id;
        this.mapColor = builder.mapColor;
        this.zhName = builder.zhName;
        this.enName = builder.enName;
        this.ingotSource = builder.ingotSource;
        this.handWritten = EnumSet.copyOf(builder.handWritten);

        // ---- 注册物品 / 方块 ----
        EnumSet<Part> parts = builder.parts.clone();
        for (Part part : parts) {
            if (part == Part.INGOT && builder.ingotSource != null) {
                continue; // 用原版锭当来源，不注册自己的锭物品
            }
            String path = id(part);
            if (part.isBlock()) {
                int[] xp = builder.oreExperience.get(part);
                Block block = switch (part) {
                    case BLOCK -> ModBlocks.register(path, Block::new, ModBlocks.metalBlockSettings(this.mapColor));
                    case ORE -> ModBlocks.register(path,
                            settings -> new ExperienceDroppingBlock(UniformIntProvider.create(xp[0], xp[1]), settings),
                            ModBlocks.oreSettings());
                    case DEEPSLATE_ORE -> ModBlocks.register(path,
                            settings -> new ExperienceDroppingBlock(UniformIntProvider.create(xp[0], xp[1]), settings),
                            ModBlocks.deepslateOreSettings());
                    default -> ModBlocks.register(path,
                            settings -> new ExperienceDroppingBlock(UniformIntProvider.create(xp[0], xp[1]), settings),
                            ModBlocks.netherOreSettings());
                };
                this.blocks.put(part, block);
            } else {
                this.items.put(part, ModItems.register(path, Item::new, new Item.Settings()));
            }
        }

        // ---- 推导默认配方计划 ----
        Recipes plan = new Recipes();
        boolean hasIngot = this.items.containsKey(Part.INGOT) || builder.ingotSource != null;
        plan.blockFromIngots = hasIngot && this.blocks.containsKey(Part.BLOCK);
        plan.ingotsFromBlock = this.items.containsKey(Part.INGOT) && this.blocks.containsKey(Part.BLOCK);
        plan.plateFromIngot = hasIngot && this.items.containsKey(Part.PLATE);
        plan.dustFromSource = hasIngot && this.items.containsKey(Part.DUST);
        // 粉能不能烧回锭：默认不生成，只有明确写了 .dustSmelting(true) 的族才有这条配方
        // （目前只有青铜：1 青铜粉 → 1 青铜锭，见用户定的配方表）
        plan.dustToIngot = false;
        plan.rawToIngot = this.items.containsKey(Part.INGOT) && this.items.containsKey(Part.RAW);
        plan.nuggetFromIngot = this.items.containsKey(Part.INGOT) && this.items.containsKey(Part.NUGGET);
        plan.ingotsFromNuggets = plan.nuggetFromIngot;
        plan.rawNuggetFromRaw = this.items.containsKey(Part.RAW) && this.items.containsKey(Part.RAW_NUGGET);
        plan.rawFromRawNuggets = plan.rawNuggetFromRaw;
        builder.applyRecipeOverrides(plan);
        this.recipes = plan;

        this.recipeSource = this.items.containsKey(Part.INGOT)
                ? this.items.get(Part.INGOT)
                : builder.dustSource != null ? builder.dustSource : builder.ingotSource;
    }

    /** 拼接某个部件的注册 id。 */
    public String id(Part part) {
        return switch (part) {
            case RAW -> "raw_" + this.id;
            case RAW_NUGGET -> "raw_" + this.id + "_nugget";
            case ORE -> this.id + "_ore";
            case DEEPSLATE_ORE -> "deepslate_" + this.id + "_ore";
            case NETHER_ORE -> "nether_" + this.id + "_ore";
            case BLOCK -> this.id + "_block";
            case INGOT -> this.id + "_ingot";
            case NUGGET -> this.id + "_nugget";
            case PLATE -> this.id + "_plate";
            case DUST -> this.id + "_dust";
        };
    }

    // ------------------------------------------------------------------
    // 取值
    // ------------------------------------------------------------------

    public String id() {
        return this.id;
    }

    public MapColor mapColor() {
        return this.mapColor;
    }

    /** 中文名（只用于提示语言条目，不会自动写进 lang 文件）。 */
    public String zhName() {
        return this.zhName;
    }

    public String enName() {
        return this.enName;
    }

    public Recipes recipes() {
        return this.recipes;
    }

    /** 这个部件的资源还是手写的（datagen 跳过）。 */
    public boolean isHandWritten(Part part) {
        return this.handWritten.contains(part);
    }

    /** 有没有这个部件。 */
    public boolean has(Part part) {
        return this.items.containsKey(part) || this.blocks.containsKey(part);
    }

    /** 锭的替代来源（原版物品）；只有 {@code ingotFrom(...)} 的族才有。 */
    public @Nullable Item ingotSource() {
        return this.ingotSource;
    }

    /** 板/粉配方的输入物品（默认是自己的锭，没有锭就退回 {@link #ingotSource()}）。 */
    public @Nullable Item recipeSource() {
        return this.recipeSource;
    }

    public @Nullable Item item(Part part) {
        return this.items.get(part);
    }

    public @Nullable Block block(Part part) {
        return this.blocks.get(part);
    }

    /** 矿石的经验范围 [min, max]。 */
    public int[] oreExperience(Part part) {
        return this.oreExperience.getOrDefault(part, new int[]{0, 2});
    }

    public @Nullable Item ingot() {
        return item(Part.INGOT);
    }

    public @Nullable Item nugget() {
        return item(Part.NUGGET);
    }

    public @Nullable Item plate() {
        return item(Part.PLATE);
    }

    public @Nullable Item dust() {
        return item(Part.DUST);
    }

    public @Nullable Item raw() {
        return item(Part.RAW);
    }

    public @Nullable Item rawNugget() {
        return item(Part.RAW_NUGGET);
    }

    public @Nullable Block block() {
        return block(Part.BLOCK);
    }

    public @Nullable Block ore() {
        return block(Part.ORE);
    }

    public @Nullable Block deepslateOre() {
        return block(Part.DEEPSLATE_ORE);
    }

    public @Nullable Block netherOre() {
        return block(Part.NETHER_ORE);
    }

    /** 这个族登记过的所有部件（按枚举顺序）。 */
    public List<Part> parts() {
        List<Part> list = new ArrayList<>();
        for (Part part : Part.values()) {
            if (has(part)) {
                list.add(part);
            }
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * 语言文件需要的键值和建议名字，例如
     * {@code item.mcdonalds-mod.<id>_ingot -> 锡锭}。
     *
     * <p>datagen 会拿它去查 {@code lang/zh_cn.json} / {@code en_us.json}，
     * 缺条目就在日志里列出来（不会自动改 lang 文件，避免覆盖手写内容）。
     */
    public List<LangEntry> langEntries() {
        List<LangEntry> entries = new ArrayList<>();
        for (Part part : parts()) {
            if (part == Part.INGOT && this.ingotSource != null) {
                continue;
            }
            String key = (part.isBlock() ? "block." : "item.") + "mcdonalds-mod." + id(part);
            String en = this.enName.isBlank() ? id(part) : this.enName + " " + partWord(part);
            String zh = this.zhName.isBlank() ? id(part) : this.zhName + partWordZh(part);
            entries.add(new LangEntry(key, en, zh));
        }
        return entries;
    }

    /** 一条语言条目：键 + 建议的英文/中文值。 */
    public record LangEntry(String key, String en, String zh) {
    }

    private static String partWord(Part part) {
        return switch (part) {
            case INGOT -> "Ingot";
            case NUGGET -> "Nugget";
            case PLATE -> "Plate";
            case DUST -> "Dust";
            case RAW -> "Raw Ore";
            case RAW_NUGGET -> "Raw Ore Nugget";
            case BLOCK -> "Block";
            case ORE -> "Ore";
            case DEEPSLATE_ORE -> "Deepslate Ore";
            case NETHER_ORE -> "Nether Ore";
        };
    }

    private static String partWordZh(Part part) {
        return switch (part) {
            case INGOT -> "锭";
            case NUGGET -> "粒";
            case PLATE -> "板";
            case DUST -> "粉";
            case RAW -> "粗矿";
            case RAW_NUGGET -> "粗矿粒";
            case BLOCK -> "块";
            case ORE -> "矿石";
            case DEEPSLATE_ORE -> "深层矿石";
            case NETHER_ORE -> "下界矿石";
        };
    }

    @Override
    public String toString() {
        return "MaterialFamily[" + this.id + " " + this.parts() + "]";
    }

    // ------------------------------------------------------------------
    // 构建器
    // ------------------------------------------------------------------

    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** 材料族构建器。所有 {@code xxx(false)} 都是"反例开关"，默认值见各方法注释。 */
    public static final class Builder {
        private final String id;
        private MapColor mapColor = MapColor.STONE_GRAY;
        private String zhName = "";
        private String enName = "";
        private final EnumSet<Part> parts = EnumSet.noneOf(Part.class);
        private final EnumSet<Part> handWritten = EnumSet.noneOf(Part.class);
        private final EnumMap<Part, int[]> oreExperience = new EnumMap<>(Part.class);
        private final EnumMap<Part, Boolean> overrides = new EnumMap<>(Part.class);
        /** 下面是"只覆盖显式设置过的项"，null = 用默认。 */
        private @Nullable Boolean dustToIngot;
        private @Nullable Boolean rawToIngot;
        private @Nullable Boolean nuggetRecipes;
        private @Nullable Integer plateCount;
        private @Nullable Integer cookingTime;
        private @Nullable Float experience;
        private @Nullable Item ingotSource;
        private @Nullable Item dustSource;

        private Builder(String id) {
            // 小写 + 下划线，和原版命名习惯一致
            this.id = id.toLowerCase(Locale.ROOT).replace(' ', '_');
        }

        /** 材料块 / 矿石的地图颜色。 */
        public Builder mapColor(MapColor mapColor) {
            this.mapColor = mapColor;
            return this;
        }

        /** 中英文名，只用于 datagen 的"缺语言条目"提示。 */
        public Builder name(String zh, String en) {
            this.zhName = zh;
            this.enName = en;
            return this;
        }

        public Builder ingot() {
            this.parts.add(Part.INGOT);
            return this;
        }

        /**
         * 不注册自己的锭，改用原版物品当配方来源（铜、金、铁那种）。
         * 等价于"有锭来源、没有锭物品"。
         */
        public Builder ingotFrom(Item vanillaIngot) {
            this.ingotSource = vanillaIngot;
            return this;
        }

        public Builder nugget() {
            this.parts.add(Part.NUGGET);
            return this;
        }

        public Builder plate() {
            this.parts.add(Part.PLATE);
            return this;
        }

        public Builder dust() {
            this.parts.add(Part.DUST);
            return this;
        }

        /** 粉的研钵配方用别的物品当来源（默认用锭）。 */
        public Builder dustFrom(Item source) {
            this.dustSource = source;
            return this;
        }

        public Builder raw() {
            this.parts.add(Part.RAW);
            return this;
        }

        public Builder rawNugget() {
            this.parts.add(Part.RAW_NUGGET);
            return this;
        }

        public Builder block() {
            this.parts.add(Part.BLOCK);
            return this;
        }

        /** 石头矿石，参数是挖掘经验范围（例如原版锡矿那种 0~2）。 */
        public Builder ore(int experienceMin, int experienceMax) {
            this.parts.add(Part.ORE);
            this.oreExperience.put(Part.ORE, new int[]{experienceMin, experienceMax});
            return this;
        }

        public Builder deepslateOre(int experienceMin, int experienceMax) {
            this.parts.add(Part.DEEPSLATE_ORE);
            this.oreExperience.put(Part.DEEPSLATE_ORE, new int[]{experienceMin, experienceMax});
            return this;
        }

        public Builder netherOre(int experienceMin, int experienceMax) {
            this.parts.add(Part.NETHER_ORE);
            this.oreExperience.put(Part.NETHER_ORE, new int[]{experienceMin, experienceMax});
            return this;
        }

        // ---- 反例开关 ----

        /** 这些部件的资源还是手写的，datagen 跳过（迁移期用，绝不会覆盖手写文件）。 */
        public Builder handWritten(Part... parts) {
            Collections.addAll(this.handWritten, parts);
            return this;
        }

        /** 整族资源都还是手写的。 */
        public Builder noGeneratedAssets() {
            Collections.addAll(this.handWritten, Part.values());
            return this;
        }

        /** 关掉"9 锭 → 1 块"配方（默认：有锭 + 有块就生成）。 */
        public Builder blockRecipe(boolean enabled) {
            this.overrides.put(Part.BLOCK, enabled);
            return this;
        }

        /** 关掉"锤子 + 锭 → 板"配方（默认：有锭 + 有板就生成）。 */
        public Builder plateRecipe(boolean enabled) {
            this.overrides.put(Part.PLATE, enabled);
            return this;
        }

        /** 关掉"研钵 + 来源 → 粉"配方（默认：有来源 + 有粉就生成）。 */
        public Builder dustRecipe(boolean enabled) {
            this.overrides.put(Part.DUST, enabled);
            return this;
        }

        /** 粉能不能烧回锭（默认 true）。 */
        public Builder dustSmelting(boolean enabled) {
            this.dustToIngot = enabled;
            return this;
        }

        /** 粗矿能不能烧成锭（默认 true）。 */
        public Builder rawSmelting(boolean enabled) {
            this.rawToIngot = enabled;
            return this;
        }

        /** 粒的 9↔1 双向配方开关（默认：有锭 + 有粒就生成）。 */
        public Builder nuggetRecipes(boolean enabled) {
            this.nuggetRecipes = enabled;
            return this;
        }

        /** 一次锤出几块板（默认 2）。 */
        public Builder plateCount(int count) {
            this.plateCount = count;
            return this;
        }

        /** 熔炼经验（默认 0.7）。 */
        public Builder experience(float experience) {
            this.experience = experience;
            return this;
        }

        /** 熔炉烧制时间（tick，默认 200）。 */
        public Builder cookingTime(int ticks) {
            this.cookingTime = ticks;
            return this;
        }

        public MaterialFamily build() {
            if (this.parts.contains(Part.PLATE) && this.ingotSource == null && !this.parts.contains(Part.INGOT)) {
                throw new IllegalStateException("材料 " + this.id + " 有板但没有锭来源，先写 .ingot() 或 .ingotFrom(...)");
            }
            if (this.parts.contains(Part.RAW_NUGGET) && !this.parts.contains(Part.RAW)) {
                throw new IllegalStateException("材料 " + this.id + " 有粗矿粒但没有粗矿，先写 .raw()");
            }
            return new MaterialFamily(this);
        }

        /** 把显式的反例开关叠到默认计划上（{@link MaterialFamily} 的构造器调用）。 */
        private void applyRecipeOverrides(Recipes plan) {
            if (this.overrides.containsKey(Part.BLOCK)) {
                boolean enabled = this.overrides.get(Part.BLOCK);
                plan.blockFromIngots = enabled && plan.blockFromIngots;
                plan.ingotsFromBlock = enabled && plan.ingotsFromBlock;
            }
            if (this.overrides.containsKey(Part.PLATE)) {
                plan.plateFromIngot = this.overrides.get(Part.PLATE) && plan.plateFromIngot;
            }
            if (this.overrides.containsKey(Part.DUST)) {
                plan.dustFromSource = this.overrides.get(Part.DUST) && plan.dustFromSource;
            }
            if (this.dustToIngot != null) {
                plan.dustToIngot = this.dustToIngot;
            }
            if (this.rawToIngot != null) {
                plan.rawToIngot = this.rawToIngot;
            }
            if (this.nuggetRecipes != null) {
                plan.nuggetFromIngot = this.nuggetRecipes;
                plan.ingotsFromNuggets = this.nuggetRecipes;
            }
            if (this.plateCount != null) {
                plan.plateCount = this.plateCount;
            }
            if (this.experience != null) {
                plan.experience = this.experience;
            }
            if (this.cookingTime != null) {
                plan.cookingTime = this.cookingTime;
            }
        }
    }
}
