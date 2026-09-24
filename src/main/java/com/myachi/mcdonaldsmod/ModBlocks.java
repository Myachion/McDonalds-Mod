package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.specialItem.CheeseCakeBlock;
import com.myachi.mcdonaldsmod.specialItem.ChocolateCakeBlock;
import com.myachi.mcdonaldsmod.crop.CornCropBlock;
import com.myachi.mcdonaldsmod.crop.BlueberryBushBlock;
import com.myachi.mcdonaldsmod.crop.OnionCropBlock;
import com.myachi.mcdonaldsmod.crop.TomatoCropBlock;
import com.myachi.mcdonaldsmod.machine.TestGeneratorBlock;
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
            "test_battery_box", Block::new,
            AbstractBlock.Settings.create()
                    .requiresTool()
                    .strength(3.0F, 3.0F)
                    .sounds(BlockSoundGroup.IRON)
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

    public static void initializeModBlocks() {
    }

}
