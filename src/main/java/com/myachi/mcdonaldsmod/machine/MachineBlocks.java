package com.myachi.mcdonaldsmod.machine;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 机器方块的公共实现细节，给 {@link AbstractMachineBlock}（无朝向）和 {@link MachineBlock}
 * （有朝向、可选激活贴图）两个基类复用。包内可见，机器之外不用管。
 */
final class MachineBlocks {
    private MachineBlocks() {
    }

    /** 每 tick 转发给方块实体；只认 {@link AbstractMachineBlockEntity} 子类。 */
    static <T extends BlockEntity> BlockEntityTicker<T> ticker() {
        return (world, pos, state, blockEntity) -> {
            if (blockEntity instanceof AbstractMachineBlockEntity machine) {
                machine.tickMachine();
            }
        };
    }

    /** 右键开界面：方块实体实现 {@link NamedScreenHandlerFactory} 即可（基类已经实现了）。 */
    static ActionResult openScreen(World world, BlockPos pos, PlayerEntity player) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof NamedScreenHandlerFactory factory) {
            player.openHandledScreen(factory);
        }
        return ActionResult.SUCCESS;
    }

    /**
     * 切换"正在工作"状态（正面激活贴图）。<b>方块没有 {@code lit} 属性时自动忽略</b>，
     * 所以带界面的电力机器（电池盒、测试发电机）也能安全调用。
     */
    static void setActive(World world, BlockPos pos, boolean active) {
        if (world == null) {
            return;
        }
        BlockState state = world.getBlockState(pos);
        if (state.contains(MachineBlock.LIT) && state.get(MachineBlock.LIT) != active) {
            world.setBlockState(pos, state.with(MachineBlock.LIT, active), Block.NOTIFY_LISTENERS);
        }
    }
}
