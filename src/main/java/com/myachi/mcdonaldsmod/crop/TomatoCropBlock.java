package com.myachi.mcdonaldsmod.crop;

import com.mojang.serialization.MapCodec;
import com.myachi.mcdonaldsmod.ModItems;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.ItemConvertible;

/** 番茄作物：4 个生长阶段，成熟后一次性收获。通用逻辑见 {@link SimpleCropBlock}。 */
public class TomatoCropBlock extends SimpleCropBlock {
    public static final MapCodec<TomatoCropBlock> CODEC = createCodec(TomatoCropBlock::new);

    public TomatoCropBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<TomatoCropBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.TOMATO_SEEDS;
    }
}
