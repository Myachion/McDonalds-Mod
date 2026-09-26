package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.energy.CableConnections;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * 不带朝向、不带激活贴图的机器方块基类 —— 对称机器用（测试电池盒这类）。
 *
 * <p>白拿的东西和 {@link MachineBlock} 一样，只是没有 {@code facing} / {@code lit} 两个属性：
 * <ul>
 *     <li>六面接电缆：构造时自动登记 {@link CableConnections#always};</li>
 *     <li>每 tick 转发给 {@link AbstractMachineBlockEntity}；</li>
 *     <li>右键开界面（方块实体实现 {@code NamedScreenHandlerFactory}）。</li>
 * </ul>
 *
 * <p>需要"正面朝向放置者 + 工作时有第二种正面贴图"的机器，请继承 {@link MachineBlock}。
 *
 * <pre>{@code
 * public class MyBoxBlock extends AbstractMachineBlock {
 *     public static final MapCodec<MyBoxBlock> CODEC = createCodec(MyBoxBlock::new);
 *     public MyBoxBlock(AbstractBlock.Settings settings) { super(settings); }
 *     @Override public MapCodec<MyBoxBlock> getCodec() { return CODEC; }
 *     @Nullable @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new MyBoxBlockEntity(pos, state); }
 * }
 * }</pre>
 */
public abstract class AbstractMachineBlock extends Block implements BlockEntityProvider {
    protected AbstractMachineBlock(AbstractBlock.Settings settings) {
        super(settings);
        // 用电器默认六面都能接电缆；要限制端口就在 ModBlocks 里用 CableConnections 覆盖
        CableConnections.always(this);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return MachineBlocks.ticker();
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        return MachineBlocks.openScreen(world, pos, player);
    }
}
