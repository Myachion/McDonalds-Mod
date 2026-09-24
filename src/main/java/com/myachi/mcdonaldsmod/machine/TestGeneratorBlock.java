package com.myachi.mcdonaldsmod.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

/**
 * 测试发电机。
 *
 * <p>目前只是"能朝向的方块"：正面（贴图上有面板的那一面）朝放置者，和熔炉一致。
 * 还没有任何交互，也没有能量逻辑——等电缆方块做好后，它会作为电网里的电源节点接入
 * （模型正面即出线面）。
 */
public class TestGeneratorBlock extends HorizontalFacingBlock {
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
}
