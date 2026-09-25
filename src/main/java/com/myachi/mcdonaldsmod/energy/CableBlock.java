package com.myachi.mcdonaldsmod.energy;

import com.myachi.mcdonaldsmod.ModBlockTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 电缆方块：一根 4×4 像素粗细的小方块，六个方向可以自动连接。
 *
 * <p>连接规则（见 {@link #canConnectTo}）：
 * <ol>
 *     <li>邻居是电缆（{@code mcdonalds-mod:cable} 标签）→ 连接；</li>
 *     <li>邻居实现了 {@link CableConnectable} → 由机器自己决定这一面能不能出线；</li>
 *     <li>邻居在 {@code mcdonalds-mod:cable_connectable} 标签里 → 六面都连接。</li>
 * </ol>
 *
 * <p>方块状态本身只记录"哪一面连着"（6 个布尔属性、64 种状态），渲染交给 multipart 模型，
 * 形状是"中心块 + 已连接方向的臂"的并集。
 */
public class CableBlock extends Block {
    /** 普通缆的横截面宽度（像素）。 */
    public static final int THIN = 4;
    /** 加粗缆（钢、铁）的横截面宽度（像素）。 */
    public static final int THICK = 6;

    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;

    public static final Map<Direction, BooleanProperty> CONNECTION_PROPERTIES = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST,
            Direction.UP, UP,
            Direction.DOWN, DOWN
    );

    private final MapCodec<CableBlock> codec;
    /** 额定电压（V）。 */
    private final int voltage;
    /** 额定电流（mA），超过它会开始过热。 */
    private final int ratedCurrentMilliAmps;
    /** 每格电阻（毫欧），用来算线路压降和损耗。 */
    private final int resistanceMilliOhms;
    /** 横截面宽度（像素）：中心块和各条臂都用这个尺寸。 */
    private final int thickness;
    /** 中心块。 */
    private final VoxelShape coreShape;
    /** 六个方向的臂：与中心块同粗，从中心一直伸到方块边界。 */
    private final Map<Direction, VoxelShape> armShapes;
    /** 64 种状态的外形预计算一次，避免每次视线检测 / 碰撞检测都做形状并集。 */
    private final Map<BlockState, VoxelShape> outlineShapes = new HashMap<>();

    public CableBlock(int voltage, AbstractBlock.Settings settings) {
        this(voltage, THIN, 16, 50, settings);
    }

    public CableBlock(int voltage, int thickness, AbstractBlock.Settings settings) {
        this(voltage, thickness, 16, 50, settings);
    }

    public CableBlock(int voltage, int thickness, int ratedCurrentAmps, int resistanceMilliOhms, AbstractBlock.Settings settings) {
        super(settings);
        this.voltage = voltage;
        this.thickness = thickness;
        this.ratedCurrentMilliAmps = ratedCurrentAmps * 1000;
        this.resistanceMilliOhms = resistanceMilliOhms;
        this.codec = createCodec(settings1 -> new CableBlock(voltage, thickness, ratedCurrentAmps, resistanceMilliOhms, settings1));

        double min = (16.0 - thickness) / 2.0;
        double max = 16.0 - min;
        this.coreShape = Block.createCuboidShape(min, min, min, max, max, max);
        this.armShapes = Map.of(
                Direction.NORTH, Block.createCuboidShape(min, min, 0.0, max, max, min),
                Direction.SOUTH, Block.createCuboidShape(min, min, max, max, max, 16.0),
                Direction.WEST, Block.createCuboidShape(0.0, min, min, min, max, max),
                Direction.EAST, Block.createCuboidShape(max, min, min, 16.0, max, max),
                Direction.UP, Block.createCuboidShape(min, max, min, max, 16.0, max),
                Direction.DOWN, Block.createCuboidShape(min, 0.0, min, max, min, max)
        );

        BlockState defaultState = this.stateManager.getDefaultState();
        for (BooleanProperty property : CONNECTION_PROPERTIES.values()) {
            defaultState = defaultState.with(property, false);
        }
        this.setDefaultState(defaultState);

        for (BlockState state : this.stateManager.getStates()) {
            this.outlineShapes.put(state, buildOutlineShape(state));
        }
    }

    public int getVoltage() {
        return this.voltage;
    }

    public int getThickness() {
        return this.thickness;
    }

    /** 额定电流（mA）。 */
    public int getRatedCurrentMilliAmps() {
        return this.ratedCurrentMilliAmps;
    }

    /** 每格电阻（毫欧）。 */
    public int getResistanceMilliOhms() {
        return this.resistanceMilliOhms;
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return this.codec;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    // ------------------------------------------------------------------
    // 形状
    // ------------------------------------------------------------------

    private VoxelShape buildOutlineShape(BlockState state) {
        VoxelShape shape = this.coreShape;
        for (Direction direction : Direction.values()) {
            if (state.get(CONNECTION_PROPERTIES.get(direction))) {
                shape = VoxelShapes.union(shape, this.armShapes.get(direction));
            }
        }
        return shape;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return this.outlineShapes.get(state);
    }

    /**
     * 碰撞箱和外形完全一致：中心 4×4×4 像素的块，加上每个已连接方向伸出的 4×4 像素臂。
     * 电缆因此可以被踩上去，也会挡住想穿过它的实体——和大多数科技模组的电线一样。
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return this.outlineShapes.get(state);
    }

    /** 电缆不挡光、不遮住相邻方块的面。 */
    @Override
    protected boolean isTransparent(BlockState state) {
        return true;
    }

    // ------------------------------------------------------------------
    // 连接
    // ------------------------------------------------------------------

    /**
     * 判断电缆 {@code pos} 的 {@code direction} 一侧要不要伸出一条臂。
     *
     * @param direction 从电缆指向邻居的方向；传给机器时会取反成"机器的哪一面"
     */
    public static boolean canConnectTo(BlockState neighborState, BlockView world, BlockPos neighborPos, Direction direction) {
        if (neighborState.isIn(ModBlockTags.CABLE)) {
            return true;
        }

        Direction side = direction.getOpposite();
        // 登记表优先；方块类自己实现接口也算一种登记方式。
        CableConnectable provider = CableConnections.get(neighborState.getBlock());
        if (provider == null && neighborState.getBlock() instanceof CableConnectable connectable) {
            provider = connectable;
        }
        if (provider != null) {
            return provider.canCableConnect(neighborState, world, neighborPos, side);
        }

        // 数据包兜底：标签里的方块六面都能接。
        return neighborState.isIn(ModBlockTags.CABLE_CONNECTABLE);
    }

    /** 按周围方块重新算一遍六个方向，返回带连接属性的状态。 */
    public BlockState withConnections(BlockState state, BlockView world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BooleanProperty property = CONNECTION_PROPERTIES.get(direction);
            BlockPos neighborPos = pos.offset(direction);
            boolean connect = canConnectTo(world.getBlockState(neighborPos), world, neighborPos, direction);
            if (state.get(property) != connect) {
                state = state.with(property, connect);
            }
        }
        return state;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.withConnections(this.getDefaultState(), ctx.getWorld(), ctx.getBlockPos());
    }

    /**
     * 邻居变化时重算连接。原版藤蔓、栅栏也是这样在 {@code getStateForNeighborUpdate} 里
     * 更新自己的连接状态的（引擎拿到不同的返回值就会写回世界）。
     */
    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos,
                                                   Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        return this.withConnections(state, world, pos);
    }

    /** 电缆放下时告诉电网：网络结构要重算。 */
    @Override
    protected void onBlockAdded(BlockState state, net.minecraft.world.World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (world instanceof ServerWorld serverWorld) {
            EnergyNetworks.get(serverWorld).markCable(pos, true);
        }
    }

    /** 电缆被拆掉时同理。 */
    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        EnergyNetworks.get(world).markCable(pos, false);
        super.onStateReplaced(state, world, pos, moved);
    }
}
