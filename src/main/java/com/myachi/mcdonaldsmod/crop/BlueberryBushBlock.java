package com.myachi.mcdonaldsmod.crop;

import com.mojang.serialization.MapCodec;
import com.myachi.mcdonaldsmod.McDonaldsMod;
import com.myachi.mcdonaldsmod.ModItems;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;

/**
 * 蓝莓丛。逻辑照原版甜浆果（{@code SweetBerryBushBlock}）：
 *
 * <ul>
 *   <li>种在 {@code #minecraft:dirt}（泥土/草方块/灰化土/菌丝/泥巴等）上，<b>不需要耕地</b>——
 *       由 {@link PlantBlock#canPlantOnTop} 决定。</li>
 *   <li>age 2 起可以右键采摘，采完退回 age 1，不破坏植株，可以反复收。</li>
 *   <li>穿过只会减速、不会受伤（狐狸和蜜蜂连减速都没有）。</li>
 *   <li>精准采集掉落蓝莓丛物品，空手破坏按阶段给蓝莓。</li>
 * </ul>
 */
public class BlueberryBushBlock extends PlantBlock implements Fertilizable {
    public static final MapCodec<BlueberryBushBlock> CODEC = createCodec(BlueberryBushBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntProperty AGE = Properties.AGE_3;
    /** 右键采摘走单独的掉落表，与原版甜浆果一致。 */
    public static final RegistryKey<LootTable> HARVEST_LOOT_TABLE = RegistryKey.of(
            RegistryKeys.LOOT_TABLE, Identifier.of(McDonaldsMod.MOD_ID, "harvest/blueberry_bush"));
    private static final VoxelShape SMALL_SHAPE = Block.createColumnShape(10.0, 0.0, 8.0);
    private static final VoxelShape LARGE_SHAPE = Block.createColumnShape(14.0, 0.0, 16.0);

    public BlueberryBushBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AGE, 0));
    }

    @Override
    public MapCodec<BlueberryBushBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        // 中键选取给"能种下去的那个物品"，和原版甜浆果给甜浆果一致。
        return new ItemStack(ModItems.BLUEBERRY);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(AGE)) {
            case 0 -> SMALL_SHAPE;
            case 3 -> VoxelShapes.fullCube();
            default -> LARGE_SHAPE;
        };
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return state.get(AGE) < MAX_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int age = state.get(AGE);
        if (age < MAX_AGE && random.nextInt(5) == 0 && world.getBaseLightLevel(pos.up(), 0) >= 9) {
            BlockState grown = state.with(AGE, age + 1);
            world.setBlockState(pos, grown, Block.NOTIFY_LISTENERS);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(grown));
        }
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity,
                                     EntityCollisionHandler handler, boolean bl) {
        if (entity instanceof LivingEntity && entity.getType() != EntityType.FOX && entity.getType() != EntityType.BEE) {
            entity.slowMovement(state, new Vec3d(0.8F, 0.75, 0.8F));
        }
    }

    /** 未成熟时把骨粉让给原版骨粉逻辑处理（和甜浆果一致）。 */
    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        boolean mature = state.get(AGE) == MAX_AGE;
        return !mature && stack.isOf(Items.BONE_MEAL)
                ? ActionResult.PASS
                : super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (state.get(AGE) <= 1) {
            return super.onUse(state, world, pos, player, hit);
        }

        if (world instanceof ServerWorld serverWorld) {
            Block.generateBlockInteractLoot(serverWorld, HARVEST_LOOT_TABLE, state, world.getBlockEntity(pos),
                    null, player, (worldx, stack) -> Block.dropStack(worldx, pos, stack));
            serverWorld.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES,
                    SoundCategory.BLOCKS, 1.0F, 0.8F + serverWorld.random.nextFloat() * 0.4F);
            BlockState picked = state.with(AGE, 1);
            serverWorld.setBlockState(pos, picked, Block.NOTIFY_LISTENERS);
            serverWorld.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, picked));
        }

        return ActionResult.SUCCESS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return state.get(AGE) < MAX_AGE;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state.with(AGE, Math.min(MAX_AGE, state.get(AGE) + 1)), Block.NOTIFY_LISTENERS);
    }
}
