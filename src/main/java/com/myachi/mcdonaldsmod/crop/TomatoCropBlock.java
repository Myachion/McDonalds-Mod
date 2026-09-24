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

/** 番茄作物：4 个生长阶段（age 0~3），成熟后一次性收获。 */
public class TomatoCropBlock extends CropBlock {
    public static final MapCodec<TomatoCropBlock> CODEC = createCodec(TomatoCropBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntProperty AGE = Properties.AGE_3;
    private static final VoxelShape[] SHAPES_BY_AGE =
            Block.createShapeArray(MAX_AGE, age -> Block.createColumnShape(16.0, 0.0, 2 + age * 5));

    public TomatoCropBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<TomatoCropBlock> getCodec() {
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
        return ModItems.TOMATO_SEEDS;
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
