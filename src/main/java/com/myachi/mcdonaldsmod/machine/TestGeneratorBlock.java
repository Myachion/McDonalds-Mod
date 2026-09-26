package com.myachi.mcdonaldsmod.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * 测试发电机。
 *
 * <p>正面（贴图上有面板的那一面）朝放置者 —— 朝向、接电缆、右键开界面、每 tick 转发
 * 全部来自 {@link MachineBlock}。它没有"激活贴图"，所以方块状态 JSON 里
 * {@code lit=false} 和 {@code lit=true} 指向同一个模型，状态本身一直是 false。
 */
public class TestGeneratorBlock extends MachineBlock {
    public static final MapCodec<TestGeneratorBlock> CODEC = createCodec(TestGeneratorBlock::new);

    public TestGeneratorBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<TestGeneratorBlock> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TestGeneratorBlockEntity(pos, state);
    }
}
