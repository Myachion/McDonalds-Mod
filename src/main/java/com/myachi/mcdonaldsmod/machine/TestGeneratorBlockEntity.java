package com.myachi.mcdonaldsmod.machine;

import com.myachi.mcdonaldsmod.ModBlockEntities;
import com.myachi.mcdonaldsmod.ModScreenHandlers;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * 测试发电机的数据。已经迁到 {@link AbstractMachineBlockEntity} 基类上：
 * 缓冲区、额定输出、电网登记、存档、界面属性表都由基类管，这里只写"怎么发电"和按钮逻辑。
 *
 * <p>四个可调参数（发电电压/电流、输出电压/电流）加一个 100 kJ 的缓存池。
 * 发电机按"发电功率"往缓存里充电，对外输出时从缓存取电（见 {@link #tickServer} 和
 * {@link #extractEnergy(long)}），这符合"缓冲区模式"：发出来的电先存进缓冲区，输出再从缓冲区扣。
 *
 * <p>界面字段 = {@link MachineProperties} 标准 15 个 + 自己的 2 个（发电电压/电流），
 * 索引见下面两个 {@code INDEX_*} 常量。
 */
public class TestGeneratorBlockEntity extends AbstractMachineBlockEntity {
    /** 缓存容量：100 kJ，换算成毫焦。 */
    public static final long CAPACITY = 100_000_000L;

    /** 自己的界面字段（相对额外字段的编号）。 */
    public static final int EXTRA_GENERATION_VOLTAGE = 0;
    public static final int EXTRA_GENERATION_CURRENT = 1;
    private static final int EXTRA_COUNT = 2;
    /** 自己的界面字段（属性表里的绝对索引），界面读数据用这两个。 */
    public static final int INDEX_GENERATION_VOLTAGE = MachineProperties.STANDARD_COUNT + EXTRA_GENERATION_VOLTAGE;
    public static final int INDEX_GENERATION_CURRENT = MachineProperties.STANDARD_COUNT + EXTRA_GENERATION_CURRENT;
    /** 属性字段总数：标准 15 + 自己的 2。 */
    public static final int PROPERTY_COUNT = MachineProperties.STANDARD_COUNT + EXTRA_COUNT;

    /** 初始输出档位 128 V / 0 A（和以前一致）。 */
    private static final int DEFAULT_OUTPUT_VOLTAGE = 128;

    private int generationVoltage = 128;
    private int generationCurrent = 0;

    private final PropertyDelegate properties = standardProperties();

    public TestGeneratorBlockEntity(BlockPos pos, BlockState state) {
        // 额定输入 0：发电机不接受外部充电；额定输出（可调）就是"对外输出的电压/电流"
        super(ModBlockEntities.TEST_GENERATOR, pos, state, CAPACITY,
                0, 0, DEFAULT_OUTPUT_VOLTAGE, 0);
    }

    public PropertyDelegate getProperties() {
        return this.properties;
    }

    /** 设定的输出功率（W）= 输出电压 × 输出电流。 */
    public long getOutputPower() {
        return (long) this.getRatedOutputVoltage() * this.getRatedOutputCurrent() / 1000L;
    }

    /** 设定的发电功率（W）= 发电电压 × 发电电流。 */
    public long getGenerationPower() {
        return (long) this.generationVoltage * this.generationCurrent;
    }

    @Override
    protected int extraPropertyCount() {
        return EXTRA_COUNT;
    }

    @Override
    protected int extraProperty(int index) {
        return switch (index) {
            case EXTRA_GENERATION_VOLTAGE -> this.generationVoltage;
            case EXTRA_GENERATION_CURRENT -> this.generationCurrent;
            default -> 0;
        };
    }

    /**
     * 服务端每 tick 一次：先把缓存充满（按发电功率）。
     * 对外输出发生在 {@link #extractEnergy(long)}，由电网结算调用。
     * （电网登记已经由基类的 tickMachine 做掉了。）
     */
    @Override
    protected void tickServer(ServerWorld world) {
        long generationPowerMilliWatts = this.getGenerationPower() * 1000L;
        if (generationPowerMilliWatts <= 0) {
            return;
        }
        long room = this.getCapacityMilliJoules() - this.getStoredEnergyMilliJoules();
        if (room <= 0) {
            return;
        }
        // 1 W 持续一个 tick 相当于 50 mJ；至少要塞进去 1 mJ，免得极小功率永远不动
        long generated = Math.max(1L, milliJoulesPerTick(generationPowerMilliWatts));
        this.addEnergy(Math.min(room, generated));
    }

    /** 把电压在等级表里往前/往后挪一格（超出两端就绕回去）。 */
    public void cycleVoltage(boolean output, int delta) {
        if (output) {
            setRatedOutput(EnergyLevels.cycle(EnergyLevels.VOLTAGE, getRatedOutputVoltage(), delta),
                    getRatedOutputCurrent());
        } else {
            this.generationVoltage = EnergyLevels.cycle(EnergyLevels.VOLTAGE, this.generationVoltage, delta);
            this.markDirty();
        }
    }

    /**
     * 电流在 {@link EnergyLevels#CURRENT} 档位表里上/下挪一格，和电压一样是循环的：
     * 50 A 再往上回到 0 A，0 A 再往下绕到 50 A。
     */
    public void cycleCurrent(boolean output, int delta) {
        if (output) {
            setRatedOutput(getRatedOutputVoltage(),
                    EnergyLevels.cycle(EnergyLevels.CURRENT, getRatedOutputCurrent() / 1000, delta) * 1000);
        } else {
            this.generationCurrent = EnergyLevels.cycle(EnergyLevels.CURRENT, this.generationCurrent, delta);
            this.markDirty();
        }
    }

    /** 发电机不接受外部充电，缓存只由自己的发电填。 */
    @Override
    public long insertEnergy(long millijoules) {
        return 0;
    }

    /** 发电机一律不能输入电流：六个面都只出不进。 */
    @Override
    public boolean canReceiveEnergyOn(Direction side) {
        return false;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.generationVoltage = EnergyLevels.snap(EnergyLevels.VOLTAGE, view.getInt("generation_voltage", 128));
        this.generationCurrent = EnergyLevels.snap(EnergyLevels.CURRENT, view.getInt("generation_current", 0));
        // 迁移到基类之前的存档：输出档位存在 output_voltage / output_current 里，电流单位是 A
        int legacyVoltage = view.getInt("output_voltage", -1);
        if (legacyVoltage > 0) {
            setRatedOutput(EnergyLevels.snap(EnergyLevels.VOLTAGE, legacyVoltage),
                    EnergyLevels.snap(EnergyLevels.CURRENT, view.getInt("output_current", 0)) * 1000);
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("generation_voltage", this.generationVoltage);
        view.putInt("generation_current", this.generationCurrent);
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new TestGeneratorScreenHandler(syncId, playerInventory, this.pos, this.properties);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(ModScreenHandlers.TEST_GENERATOR_TITLE_KEY);
    }
}
