package com.myachi.mcdonaldsmod.crop;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * 一次性耕田作物的公共实现：4 个生长阶段（age 0~3），长到 age 3 就是成熟，
 * 收获后整株消失（掉落逻辑在掉落表里，见 {@code data/mcdonalds-mod/loot_table/blocks/}）。
 *
 * <p>{@link CropBlock} 自带的 {@code AGE} 是 8 阶段的，所以这里像原版甜菜
 * （{@code BeetrootsBlock}）一样换成 {@code Properties.AGE_3}，并覆写
 * {@link #getAgeProperty()}、{@link #getMaxAge()} 与 {@link #appendProperties}。
 *
 * <p>子类只需要两样东西：自己的 {@code CODEC} 和 {@link #getSeedsItem()}（种子物品）。
 * 参考 {@link TomatoCropBlock} / {@link OnionCropBlock}。
 * 两格高的玉米（{@link CornCropBlock}）和丛生反复收获的蓝莓（{@link BlueberryBushBlock}）
 * 结构不同，没有继承这个类。
 */
public abstract class SimpleCropBlock extends CropBlock {
    /** 成熟阶段（age 0~3）。 */
    public static final int MAX_AGE = 3;
    public static final IntProperty AGE = Properties.AGE_3;
    private static final VoxelShape[] SHAPES_BY_AGE =
            Block.createShapeArray(MAX_AGE, age -> Block.createColumnShape(16.0, 0.0, 2 + age * 5));

    protected SimpleCropBlock(AbstractBlock.Settings settings) {
        super(settings);
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
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES_BY_AGE[this.getAge(state)];
    }
}
