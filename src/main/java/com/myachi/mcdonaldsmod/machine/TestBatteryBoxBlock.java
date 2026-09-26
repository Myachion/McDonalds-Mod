package com.myachi.mcdonaldsmod.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * 测试电池盒：一个对称的储能方块，右键打开界面。
 *
 * <p>它没有朝向、也没有激活贴图，所以继承的是 {@link AbstractMachineBlock}
 * （接电缆、tick 转发、右键开界面照样白拿）。
 */
public class TestBatteryBoxBlock extends AbstractMachineBlock {
    public static final MapCodec<TestBatteryBoxBlock> CODEC = createCodec(TestBatteryBoxBlock::new);

    public TestBatteryBoxBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<TestBatteryBoxBlock> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TestBatteryBoxBlockEntity(pos, state);
    }
}
