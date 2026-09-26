package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.energy.CableConnections;
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
import net.minecraft.screen.NamedScreenHandlerFactory;
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
 * 用电机器的公共方块底座。新机器继承它就能白拿这些东西：
 *
 * <ul>
 *     <li>{@code facing}：正面（贴图上的操作面）朝向放置者，和熔炉一致；</li>
 *     <li>{@code lit}：正在工作时为 true，方块状态里换成激活贴图（见 {@link #setActive}）；</li>
 *     <li>六面都能接电缆：构造时自动登记 {@link CableConnections#always};</li>
 *     <li>右键开界面：方块实体只要实现 {@link NamedScreenHandlerFactory} 就会被打开；</li>
 *     <li>每 tick 转发：只转发给 {@link AbstractMachineBlockEntity} 的子类。</li>
 * </ul>
 *
 * <p>子类通常只需要写自己的 {@code CODEC} 和 {@code createBlockEntity}，
 * 例如：
 * <pre>{@code
 * public class MyMachineBlock extends MachineBlock {
 *     public static final MapCodec<MyMachineBlock> CODEC = createCodec(MyMachineBlock::new);
 *     public MyMachineBlock(AbstractBlock.Settings settings) { super(settings); }
 *     @Override public MapCodec<MyMachineBlock> getCodec() { return CODEC; }
 *     @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new MyMachineBlockEntity(pos, state); }
 * }
 * }</pre>
 */
public abstract class MachineBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    /** 正在工作（正面用激活贴图）。 */
    public static final BooleanProperty LIT = Properties.LIT;

    protected MachineBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(LIT, false));
        // 用电器默认六面都能接电缆；要限制端口就在 ModBlocks 里再用 CableConnections.custom 覆盖
        CableConnections.always(this);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof AbstractMachineBlockEntity machine) {
                machine.tickMachine();
            }
        };
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof NamedScreenHandlerFactory factory) {
            player.openHandledScreen(factory);
        }
        return ActionResult.SUCCESS;
    }

    /** 切换"正在工作"状态，正面贴图跟着变。方块实体里直接调 {@code setActive(...)} 即可。 */
    public static void setActive(World world, BlockPos pos, boolean active) {
        if (world == null) {
            return;
        }
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof MachineBlock && state.get(LIT) != active) {
            world.setBlockState(pos, state.with(LIT, active), Block.NOTIFY_LISTENERS);
        }
    }
}
