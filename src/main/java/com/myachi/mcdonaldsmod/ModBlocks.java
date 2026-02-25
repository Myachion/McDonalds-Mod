package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.specialItem.CheeseCakeBlock;
import com.myachi.mcdonaldsmod.specialItem.ChocolateCakeBlock;
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



    private static Block register(String path, Function<AbstractBlock.Settings, Block> factory, AbstractBlock.Settings settings) {
        final Identifier identifier = Identifier.of(McDonaldsMod.MOD_ID, path);
        final RegistryKey<Block> registryKey = RegistryKey.of(RegistryKeys.BLOCK, identifier);

        final Block block = Blocks.register(registryKey, factory, settings);
        Items.register(block);
        return block;
    }

    public static void initializeModBlocks() {
    }

}
