package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * 测试发电机。
 *
 * <p>目前只是"能朝向的方块"：正面（贴图上有面板的那一面）朝放置者，和熔炉一致。
 * 右键打开界面，可以调发电/输出的电压和电流。能量逻辑还没有接——等能量层做好后，
 * 它会作为电网里的电源节点接入。六面都能接电缆（登记见 {@code ModBlocks.initializeModBlocks()}）。
 */
public class TestGeneratorBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final MapCodec<TestGeneratorBlock> CODEC = createCodec(TestGeneratorBlock::new);

    public TestGeneratorBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<TestGeneratorBlock> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // 正面朝向玩家。
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TestGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.TEST_GENERATOR) {
            return null;
        }
        return (world1, pos, state1, blockEntity) -> ((TestGeneratorBlockEntity) blockEntity).tick();
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof TestGeneratorBlockEntity generator) {
            player.openHandledScreen(generator);
        }
        return ActionResult.SUCCESS;
    }
}
