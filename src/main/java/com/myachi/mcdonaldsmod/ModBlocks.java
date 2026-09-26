package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.specialItem.CheeseCakeBlock;
import com.myachi.mcdonaldsmod.specialItem.ChocolateCakeBlock;
import com.myachi.mcdonaldsmod.crop.CornCropBlock;
import com.myachi.mcdonaldsmod.crop.BlueberryBushBlock;
import com.myachi.mcdonaldsmod.crop.OnionCropBlock;
import com.myachi.mcdonaldsmod.crop.TomatoCropBlock;
import com.myachi.mcdonaldsmod.energy.CableBlock;
import com.myachi.mcdonaldsmod.energy.CableConnections;
import com.myachi.mcdonaldsmod.machine.TestGeneratorBlock;
import com.myachi.mcdonaldsmod.machine.TestBatteryBoxBlock;
import com.myachi.mcdonaldsmod.machine.ElectricFurnaceBlock;
import net.minecraft.block.*;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.function.Function;

public class ModBlocks {
    //Register Function
    public static final Block IRON_REFINED_BLOCK = register(
            "iron_refined_block", Block::new ,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.IRON_GRAY)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresTool()
                    .strength(7.0F, 10.0F)
                    .sounds(BlockSoundGroup.IRON)
    );

    /*
     * 材料块：九个锭压成一个块，也能拆回九个锭。
     * 金属材料（铝/青铜/铅/银/钢/锡/铀）的材料块已经在 material/ModMaterials.java
     * 里跟着材料族登记；这里只剩木炭块这种不属于任何材料族的方块。
     * 木炭块和原版煤炭块一样可以当燃料烧 16000 tick。
     */
    public static final Block CHARCOAL_BLOCK = register(
            "charcoal_block", Block::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresTool()
                    .strength(5.0F, 6.0F)
                    .sounds(BlockSoundGroup.STONE));

    public static final Block SALT_ORE = register(
            "salt_ore", settings -> new ExperienceDroppingBlock(UniformIntProvider.create(0, 1), settings),
            AbstractBlock.Settings.create()
                    .requiresTool()
                    .strength(3.0F, 3.0F)
                    .sounds(BlockSoundGroup.STONE)
    );

    public static final Block DEEPSLATE_SALT_ORE = register(
            "deepslate_salt_ore", settings -> new ExperienceDroppingBlock(UniformIntProvider.create(0, 1), settings),
            AbstractBlock.Settings.create()
                    .requiresTool()
                    .strength(4.5F, 6.0F)
                    .sounds(BlockSoundGroup.DEEPSLATE)
    );

    /*
     * 金属矿石（锡/铅/铝/银/铀及其深层、下界变种）在 material/ModMaterials.java 里
     * 由材料族用 ExperienceDroppingBlock 注册（挖掉掉经验，数值写在材料族定义里）。
     * 这里只剩盐矿这种不属于材料族的矿石。
     * 生成参数见 ModOreGeneration 和 worldgen 下的数据文件。
     */

    public static final Block MACHINE_SHELL = register(
            "machine_shell", Block::new ,
            AbstractBlock.Settings.create()
                    .requiresTool()
                    .strength(3.0F, 3.0F)
                    .sounds(BlockSoundGroup.IRON)
    );

    // 电网测试用的两个方块：硬度和音效照抄机器外壳，交互是各自的界面（右键打开）。
    public static final Block TEST_GENERATOR = register(
            "test_generator", TestGeneratorBlock::new,
            AbstractBlock.Settings.create()
                    .requiresTool()
                    .strength(3.0F, 3.0F)
                    .sounds(BlockSoundGroup.IRON)
    );

    public static final Block TEST_BATTERY_BOX = register(
            "test_battery_box", TestBatteryBoxBlock::new,
            AbstractBlock.Settings.create()
                    .requiresTool()
                    .strength(3.0F, 3.0F)
                    .sounds(BlockSoundGroup.IRON)
    );

    /**
     * 电炉：额定输入 32 V / 4 A，缓冲区 2.5 kJ，烧制功率 96 W（从缓冲区取电），
     * 速度和高炉一致；能烧的东西和熔炉一样。六面都能接电缆。
     */
    public static final Block ELECTRIC_FURNACE = register(
            "electric_furnace", ElectricFurnaceBlock::new,
            AbstractBlock.Settings.create()
                    .requiresTool()
                    .strength(3.0F, 3.0F)
                    .sounds(BlockSoundGroup.IRON)
    );

    /*
     * 电缆方块：细杆状小方块，六向自动连接。
     * 参数依次是：额定电压(V) / 横截面(像素) / 额定电流(A) / 每格电阻(毫欧)。
     * 目前加入的八种（额定电流整体翻倍，电损按用户 2026-09 定的新表）：
     * 锡 32V 16A 0.05Ω / 铜 128V 32A 0.02Ω / 铜x2 128V 128A 0.01Ω / 精炼铁 512V 32A 0.05Ω /
     * 金 512V 64A 0.02Ω / 金x2 512V 256A 0.01Ω / 铁 2048V 128A 0.04Ω / 玻璃纤维 8192V 256A 0.005Ω。
     * x2 是同一材质的加粗版（横截面 6 像素，和铁导线一样粗），载流量更大、电阻更低。
     * （铝、超导还没做。）
     * 物品形式就是这些方块的 BlockItem，注册名和以前的导线物品完全一样。
     */
    public static final Block TIN_CABLE = register(
            "tin_cable",
            settings -> new CableBlock(32, CableBlock.THIN, 16, 50, settings),
            cableSettings(MapColor.LIGHT_GRAY)
    );

    public static final Block COPPER_CABLE = register(
            "copper_cable",
            settings -> new CableBlock(128, CableBlock.THIN, 32, 20, settings),
            cableSettings(MapColor.ORANGE)
    );

    /** 2x 铜导线：加粗版，载流量翻两番（128 A）、电阻减半。 */
    public static final Block COPPER_CABLE_X2 = register(
            "copper_cable_x2",
            settings -> new CableBlock(128, CableBlock.THICK, 128, 10, settings),
            cableSettings(MapColor.ORANGE)
    );

    public static final Block IRON_REFINED_CABLE = register(
            "iron_refined_cable",
            settings -> new CableBlock(512, CableBlock.THICK, 32, 50, settings),
            cableSettings(MapColor.IRON_GRAY)
    );

    public static final Block GOLD_CABLE = register(
            "gold_cable",
            settings -> new CableBlock(512, CableBlock.THIN, 64, 20, settings),
            cableSettings(MapColor.GOLD)
    );

    /** 2x 金导线：加粗版，载流量翻两番（256 A）、电阻减半。 */
    public static final Block GOLD_CABLE_X2 = register(
            "gold_cable_x2",
            settings -> new CableBlock(512, CableBlock.THICK, 256, 10, settings),
            cableSettings(MapColor.GOLD)
    );

    public static final Block IRON_CABLE = register(
            "iron_cable",
            settings -> new CableBlock(2048, CableBlock.THICK, 128, 40, settings),
            cableSettings(MapColor.STONE_GRAY)
    );

    public static final Block FIBERGLASS_CABLE = register(
            "fiberglass_cable",
            settings -> new CableBlock(8192, CableBlock.THIN, 256, 5, settings),
            cableSettings(MapColor.LIGHT_BLUE)
    );

    public static final Block CHEESE_CAKE = register(
            "cheese_cake", CheeseCakeBlock::new,
            AbstractBlock.Settings.create().solid()
                    .strength(0.5F).sounds(BlockSoundGroup.WOOL)
                    .pistonBehavior(PistonBehavior.DESTROY)
    );
    public static final Block CHOCOLATE_CAKE = register(
            "chocolate_cake", ChocolateCakeBlock::new,
            AbstractBlock.Settings.create().solid()
                    .strength(0.5F).sounds(BlockSoundGroup.WOOL)
                    .pistonBehavior(PistonBehavior.DESTROY)
    );

    // 作物：方块本身不注册同名物品，种子物品由 ModItems 以 BlockItem 的形式注册。
    public static final Block TOMATO_CROP = registerBlockOnly(
            "tomato_crop", TomatoCropBlock::new, cropSettings()
    );

    public static final Block ONION_CROP = registerBlockOnly(
            "onion_crop", OnionCropBlock::new, cropSettings()
    );

    public static final Block CORN_CROP = registerBlockOnly(
            "corn_crop", CornCropBlock::new, cropSettings()
    );

    // 蓝莓丛：像甜浆果一样种在泥土/草方块上（不是耕地），所以用 PlantBlock 的默认判定。
    public static final Block BLUEBERRY_BUSH = registerBlockOnly(
            "blueberry_bush", BlueberryBushBlock::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.DARK_GREEN)
                    .ticksRandomly()
                    .noCollision()
                    .sounds(BlockSoundGroup.SWEET_BERRY_BUSH)
                    .pistonBehavior(PistonBehavior.DESTROY)
    );


    public static Block register(String path, Function<AbstractBlock.Settings, Block> factory, AbstractBlock.Settings settings) {
        final Identifier identifier = Identifier.of(McDonaldsMod.MOD_ID, path);
        final RegistryKey<Block> registryKey = RegistryKey.of(RegistryKeys.BLOCK, identifier);

        final Block block = Blocks.register(registryKey, factory, settings);
        Items.register(block);
        return block;
    }

    /**
     * 只注册方块，不生成与方块同名的物品。
     * 作物方块的物品是种子（{@code tomato_seeds} 等），在 {@link ModItems} 里以 BlockItem 注册。
     */
    private static Block registerBlockOnly(String path, Function<AbstractBlock.Settings, Block> factory, AbstractBlock.Settings settings) {
        final Identifier identifier = Identifier.of(McDonaldsMod.MOD_ID, path);
        final RegistryKey<Block> registryKey = RegistryKey.of(RegistryKeys.BLOCK, identifier);

        return Blocks.register(registryKey, factory, settings);
    }

    /** 与小麦等原版作物一致的方块属性。 */
    private static AbstractBlock.Settings cropSettings() {
        return AbstractBlock.Settings.create()
                .mapColor(MapColor.DARK_GREEN)
                .noCollision()
                .ticksRandomly()
                .breakInstantly()
                .sounds(BlockSoundGroup.CROP)
                .pistonBehavior(PistonBehavior.DESTROY);
    }

    /** 金属材料块：硬度、抗爆、音色都和原版铁块一致，需要镐子才掉落。 */
    public static AbstractBlock.Settings metalBlockSettings(MapColor mapColor) {
        return AbstractBlock.Settings.create()
                .mapColor(mapColor)
                .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                .requiresTool()
                .strength(5.0F, 6.0F)
                .sounds(BlockSoundGroup.METAL);
    }

    /** 石头里的矿石：和原版石头矿一致的硬度与音效。 */
    public static AbstractBlock.Settings oreSettings() {
        return AbstractBlock.Settings.create()
                .requiresTool()
                .strength(3.0F, 3.0F)
                .sounds(BlockSoundGroup.STONE);
    }

    /** 深板岩里的矿石：和原版深层矿一致。 */
    public static AbstractBlock.Settings deepslateOreSettings() {
        return AbstractBlock.Settings.create()
                .requiresTool()
                .strength(4.5F, 3.0F)
                .sounds(BlockSoundGroup.DEEPSLATE);
    }

    /** 下界矿石：嵌在地狱岩里，音效同原版下界矿。 */
    public static AbstractBlock.Settings netherOreSettings() {
        return AbstractBlock.Settings.create()
                .requiresTool()
                .strength(3.0F, 3.0F)
                .sounds(BlockSoundGroup.NETHER_ORE);
    }

    /**
     * 电缆方块属性：不遮光、一挖就掉，被活塞推动时直接破坏而不是推走。
      * 碰撞箱不在这里设置——{@link CableBlock#getCollisionShape} 会跟着连接状态返回
      * 与外形完全一致的形状（中心块 + 已连接的臂）。
      */
    private static AbstractBlock.Settings cableSettings(MapColor mapColor) {
        return AbstractBlock.Settings.create()
                .mapColor(mapColor)
                .nonOpaque()
                .breakInstantly()
                .sounds(BlockSoundGroup.COPPER)
                .pistonBehavior(PistonBehavior.DESTROY);
    }

    public static void initializeModBlocks() {
        /*
         * 电缆接线登记：继承 MachineBlock / AbstractMachineBlock 的机器会在构造器里
         * 自动登记"六面可接"，一般不用在这里写。要限制端口、或者给不是这两个基类的
         * 方块登记时，才在这里加一行：
         *
         *   CableConnections.only(ModBlocks.MY_MACHINE, Direction.UP);     // 只有顶面能接
         *   CableConnections.custom(ModBlocks.MY_MACHINE,                  // 只有正面能接
         *           CableConnectable.facing(MyMachineBlock.FACING));
         *
         * 不登记 = 不能接电缆（也可以把方块写进数据包标签 mcdonalds-mod:cable_connectable，
         * 测试电池盒就在那里留了一份）。
         */
    }

}
