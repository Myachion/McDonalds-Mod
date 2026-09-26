package com.myachi.mcdonaldsmod.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * 电炉：额定 32 V / 4 A，缓冲区 2.5 kJ，烧制时按 96 W 从缓冲区取电，速度和高炉一致。
 *
 * <p>朝向、正面激活贴图、六面接电缆、右键开界面、每 tick 转发这些通用部分都在
 * {@link MachineBlock} 里，这里只需要挂上自己的方块实体。
 */
public class ElectricFurnaceBlock extends MachineBlock {
    public static final MapCodec<ElectricFurnaceBlock> CODEC = createCodec(ElectricFurnaceBlock::new);

    public ElectricFurnaceBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<ElectricFurnaceBlock> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state);
    }
}
