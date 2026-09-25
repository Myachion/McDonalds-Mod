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
import net.minecraft.block.*;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModBlocks {
    //Register Function
    public static final Block STEEL_BLOCK = register(
            "steel_block", Block::new ,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.IRON_GRAY)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresTool()
                    .strength(7.0F, 10.0F)
                    .sounds(BlockSoundGroup.IRON)
    );

    public static final Block SALT_ORE = register(
            "salt_ore", Block::new ,
            AbstractBlock.Settings.create()
                    .requiresTool()
                    .strength(3.0F, 3.0F)
                    .sounds(BlockSoundGroup.STONE)
    );

    public static final Block DEEPSLATE_SALT_ORE = register(
            "deepslate_salt_ore", Block::new ,
            AbstractBlock.Settings.create()
                    .requiresTool()
                    .strength(4.5F, 6.0F)
                    .sounds(BlockSoundGroup.DEEPSLATE)
    );

    public static final Block MACHINE_SHELL = register(
            "machine_shell", Block::new ,
            AbstractBlock.Settings.create()
                    .requiresTool()
                    .strength(3.0F, 3.0F)
                    .sounds(BlockSoundGroup.IRON)
    );

    // 电网测试用的两个方块：数据暂时照抄机器外壳，暂时没有任何交互。
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

    /*
     * 电缆方块：细杆状小方块，六向自动连接。
     * 参数依次是：额定电压(V) / 横截面(像素) / 额定电流(A) / 每格电阻(毫欧)。
     * 目前加入的六种：锡 32V / 铜 128V / 钢 512V / 金 512V / 铁 2048V / 玻璃纤维 8192V。
     * （铝、超导、以及各种"x2"加粗版还没做。）
     * 物品形式就是这些方块的 BlockItem，注册名和以前的导线物品完全一样。
     */
    public static final Block TIN_CABLE = register(
            "tin_cable",
            settings -> new CableBlock(32, CableBlock.THIN, 8, 100, settings),
            cableSettings(MapColor.LIGHT_GRAY)
    );

    public static final Block COPPER_CABLE = register(
            "copper_cable",
            settings -> new CableBlock(128, CableBlock.THIN, 16, 50, settings),
            cableSettings(MapColor.ORANGE)
    );

    public static final Block STEEL_CABLE = register(
            "steel_cable",
            settings -> new CableBlock(512, CableBlock.THICK, 16, 100, settings),
            cableSettings(MapColor.IRON_GRAY)
    );

    public static final Block GOLD_CABLE = register(
            "gold_cable",
            settings -> new CableBlock(512, CableBlock.THIN, 32, 50, settings),
            cableSettings(MapColor.GOLD)
    );

    public static final Block IRON_CABLE = register(
            "iron_cable",
            settings -> new CableBlock(2048, CableBlock.THICK, 64, 60, settings),
            cableSettings(MapColor.STONE_GRAY)
    );

    public static final Block FIBERGLASS_CABLE = register(
            "fiberglass_cable",
            settings -> new CableBlock(8192, CableBlock.THIN, 64, 10, settings),
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


    private static Block register(String path, Function<AbstractBlock.Settings, Block> factory, AbstractBlock.Settings settings) {
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
         * 电缆接线登记：想让新机器能被电缆连接，在这里加一行就行。
         *
         *   CableConnections.always(ModBlocks.MY_MACHINE);                 // 六面都能接
         *   CableConnections.only(ModBlocks.MY_MACHINE, Direction.UP);     // 只有顶面能接
         *   CableConnections.custom(ModBlocks.MY_MACHINE,                  // 只有正面能接
         *           CableConnectable.facing(MyMachineBlock.FACING));
         *
         * 不登记 = 不能接电缆。测试电池盒走的是数据包标签那一套
         * （data/mcdonalds-mod/tags/block/cable_connectable.json），效果同样是六面可连。
         */
        CableConnections.always(TEST_GENERATOR);
    }

}
