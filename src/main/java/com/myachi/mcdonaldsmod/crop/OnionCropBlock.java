package com.myachi.mcdonaldsmod.crop;

import com.mojang.serialization.MapCodec;
import com.myachi.mcdonaldsmod.ModItems;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemConvertible;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * 洋葱作物：4 个生长阶段（age 0~3），成熟后一次性收获。
 *
 * <p>{@link CropBlock} 的 {@code MAX_AGE} / {@code AGE} 是静态共享的 8 阶段版本，所以这里像原版
 * 甜菜（{@code BeetrootsBlock}）一样换成 {@code Properties.AGE_3} 并覆写
 * {@link #getAgeProperty()}、{@link #getMaxAge()} 与 {@link #appendProperties}。
 */
public class OnionCropBlock extends CropBlock {
    public static final MapCodec<OnionCropBlock> CODEC = createCodec(OnionCropBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntProperty AGE = Properties.AGE_3;
    private static final VoxelShape[] SHAPES_BY_AGE =
            Block.createShapeArray(MAX_AGE, age -> Block.createColumnShape(16.0, 0.0, 2 + age * 5));

    public OnionCropBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<OnionCropBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected IntProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.ONION_SEEDS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES_BY_AGE[this.getAge(state)];
    }
}
