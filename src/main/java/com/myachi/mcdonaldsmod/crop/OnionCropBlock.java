package com.myachi.mcdonaldsmod.crop;

import com.mojang.serialization.MapCodec;
import com.myachi.mcdonaldsmod.ModItems;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.ItemConvertible;

/** 洋葱作物：4 个生长阶段，成熟后一次性收获。通用逻辑见 {@link SimpleCropBlock}。 */
public class OnionCropBlock extends SimpleCropBlock {
    public static final MapCodec<OnionCropBlock> CODEC = createCodec(OnionCropBlock::new);

    public OnionCropBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<OnionCropBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.ONION_SEEDS;
    }
}
