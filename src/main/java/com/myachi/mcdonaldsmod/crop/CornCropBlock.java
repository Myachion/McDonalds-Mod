package com.myachi.mcdonaldsmod.crop;

import com.mojang.serialization.MapCodec;
import com.myachi.mcdonaldsmod.ModItems;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

/**
 * 玉米作物：4 个生长阶段（age 0~3），从 age 2 起长成两格高，成熟后一次性收获。
 *
 * <p>实现方式照搬原版唯一的双格高作物 {@code PitcherCropBlock}：同一个方块用
 * {@link TallPlantBlock#HALF} 区分上下半，两半的联动破坏交给 {@link TallPlantBlock} 处理。
 * 与小麦等普通作物的两点差异：
 * <ul>
 *   <li>生长速度 ×{@link #GROWTH_MULTIPLIER}（原版每 tick 的成功概率是
 *       {@code 1 / (25 / 湿度 + 1)}，把湿度放大 1.3 倍即快约 30%）。</li>
 *   <li>长到 {@link #DOUBLE_HEIGHT_AGE} 时需要在正上方放下上半格，因此随机 tick 与骨粉
 *       都统一走 {@link #tryGrow}。</li>
 * </ul>
 */
public class CornCropBlock extends TallPlantBlock implements Fertilizable {
    public static final MapCodec<CornCropBlock> CODEC = createCodec(CornCropBlock::new);
    public static final int MAX_AGE = 3;
    /** 从这个阶段开始变成两格高。 */
    public static final int DOUBLE_HEIGHT_AGE = 2;
    /** 生长速度倍率，1.3 表示比普通作物快约 30%。 */
    public static final float GROWTH_MULTIPLIER = 1.3F;
    public static final IntProperty AGE = Properties.AGE_3;
    public static final EnumProperty<DoubleBlockHalf> HALF = TallPlantBlock.HALF;

    private static final VoxelShape[] LOWER_SHAPES_BY_AGE =
            Block.createShapeArray(MAX_AGE, age -> Block.createColumnShape(16.0, 0.0, 2 + age * 5));

    public CornCropBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(HALF, DoubleBlockHalf.LOWER).with(AGE, 0));
    }

    @Override
    public MapCodec<CornCropBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
        super.appendProperties(builder);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            return LOWER_SHAPES_BY_AGE[state.get(AGE)];
        }
        return state.get(AGE) >= MAX_AGE ? VoxelShapes.fullCube() : Block.createColumnShape(16.0, 0.0, 8.0);
    }

    /** 玉米种下去时只有下半格，上半格由生长过程补上（原版双格高植物默认会一次放两格）。 */
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState();
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        // 故意留空：不要像 TallPlantBlock 那样直接把上半格也放出来。
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(Blocks.FARMLAND);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            // 与 CropBlock 一致：需要足够的光照，否则作物会自己消失。
            return world.getBaseLightLevel(pos, 0) >= 8 && super.canPlaceAt(state, world, pos);
        }
        return super.canPlaceAt(state, world, pos);
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER && state.get(AGE) < MAX_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.getBaseLightLevel(pos, 0) < 9) {
            return;
        }

        // 原版每个随机 tick 的成功概率是 1 / (25 / 湿度 + 1)。这里直接对概率整体乘 1.3，
        // 比"把湿度乘 1.3"更稳：后者因为 (int) 截断，在高湿度下会退化成没有加速。
        float moisture = CropBlock.getAvailableMoisture(this, world, pos);
        float chance = Math.min(1.0F, GROWTH_MULTIPLIER / ((int) (25.0F / moisture) + 1));
        if (random.nextFloat() < chance) {
            this.tryGrow(world, state, pos, 1);
        }
    }

    private void tryGrow(ServerWorld world, BlockState state, BlockPos pos, int amount) {
        int age = Math.min(state.get(AGE) + amount, MAX_AGE);
        if (!this.canGrow(world, pos, state, age)) {
            return;
        }

        BlockState grown = state.with(AGE, age);
        world.setBlockState(pos, grown, Block.NOTIFY_LISTENERS);
        if (age >= DOUBLE_HEIGHT_AGE) {
            world.setBlockState(pos.up(), grown.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
        }
    }

    private boolean canGrow(WorldView world, BlockPos pos, BlockState state, int age) {
        if (state.get(AGE) >= MAX_AGE) {
            return false;
        }
        if (world.getBaseLightLevel(pos, 0) < 8) {
            return false;
        }
        if (age < DOUBLE_HEIGHT_AGE) {
            return true;
        }

        // 需要正上方能放下上半格：空气，或已经属于自己的上半格。
        BlockState above = world.getBlockState(pos.up());
        return above.isAir() || above.isOf(this);
    }

    @Nullable
    private LowerHalfContext getLowerHalfContext(WorldView world, BlockPos pos, BlockState state) {
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            return new LowerHalfContext(pos, state);
        }

        BlockPos below = pos.down();
        BlockState belowState = world.getBlockState(below);
        return belowState.isOf(this) && belowState.get(HALF) == DoubleBlockHalf.LOWER
                ? new LowerHalfContext(below, belowState)
                : null;
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        LowerHalfContext lower = this.getLowerHalfContext(world, pos, state);
        return lower != null && this.canGrow(world, lower.pos(), lower.state(), lower.state().get(AGE) + 1);
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        LowerHalfContext lower = this.getLowerHalfContext(world, pos, state);
        if (lower != null) {
            this.tryGrow(world, lower.state(), lower.pos(), 1);
        }
    }

    @Override
    protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(ModItems.CORN_SEEDS);
    }

    private record LowerHalfContext(BlockPos pos, BlockState state) {
    }
}
