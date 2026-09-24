package com.myachi.mcdonaldsmod.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * 测试电池盒：右键打开界面，显示储电量和输入/输出的电压、电流、功率。
 * 六面都能接电缆（走数据包标签 {@code mcdonalds-mod:cable_connectable}）。
 */
public class TestBatteryBoxBlock extends Block implements BlockEntityProvider {
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

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof TestBatteryBoxBlockEntity battery) {
            player.openHandledScreen(battery);
        }
        return ActionResult.SUCCESS;
    }
}
