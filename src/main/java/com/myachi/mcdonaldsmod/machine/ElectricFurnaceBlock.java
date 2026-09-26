package com.myachi.mcdonaldsmod.machine;

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
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * 电炉。
 *
 * <p>正面（贴图上有炉门和加热丝的那一面）朝放置者，和熔炉一致；
 * 烧制时 {@link #LIT} 变 true，正面换成激活材质。
 * 右键打开界面（暂时是原版熔炉界面）。
 */
public class ElectricFurnaceBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final MapCodec<ElectricFurnaceBlock> CODEC = createCodec(ElectricFurnaceBlock::new);
    /** 正在烧制 = 正面用激活材质。 */
    public static final BooleanProperty LIT = Properties.LIT;

    public ElectricFurnaceBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(LIT, false));
    }

    @Override
    public MapCodec<ElectricFurnaceBlock> getCodec() {
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
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (type != com.myachi.mcdonaldsmod.ModBlockEntities.ELECTRIC_FURNACE) {
            return null;
        }
        return (world1, pos, state1, blockEntity) -> ((ElectricFurnaceBlockEntity) blockEntity).tick();
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof ElectricFurnaceBlockEntity furnace) {
            player.openHandledScreen(furnace);
        }
        return ActionResult.SUCCESS;
    }
}
