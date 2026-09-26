package com.myachi.mcdonaldsmod.machine;

import net.minecraft.screen.PropertyDelegate;

/**
 * 机器界面的"标准属性表"：走 {@link AbstractMachineScreenHandler} 的机器共用同一套
 * {@link PropertyDelegate} 索引，界面代码不用再关心每台机器自己的编号。
 *
 * <h2>索引表（前 {@value #STANDARD_COUNT} 个固定，机器自己的字段接在后面）</h2>
 * <table>
 *   <tr><th>索引</th><th>含义</th><th>单位</th></tr>
 *   <tr><td>{@link #ENERGY_LOW} ~ {@link #ENERGY_HIGH}</td><td>缓冲区电量，拆 3 个 15 位字段</td><td>mJ</td></tr>
 *   <tr><td>{@link #INPUT_VOLTAGE}</td><td>实测输入电压</td><td>V</td></tr>
 *   <tr><td>{@link #INPUT_CURRENT_LOW} ~ {@link #INPUT_CURRENT_HIGH}</td><td>实测输入电流，拆 3 段</td><td>mA</td></tr>
 *   <tr><td>{@link #OUTPUT_VOLTAGE}</td><td>实测输出电压</td><td>V</td></tr>
 *   <tr><td>{@link #OUTPUT_CURRENT_LOW} ~ {@link #OUTPUT_CURRENT_HIGH}</td><td>实测输出电流，拆 3 段</td><td>mA</td></tr>
 *   <tr><td>{@link #RATED_INPUT_VOLTAGE} / {@link #RATED_INPUT_CURRENT}</td><td>额定输入（可调机器用）</td><td>V / A</td></tr>
 *   <tr><td>{@link #RATED_OUTPUT_VOLTAGE} / {@link #RATED_OUTPUT_CURRENT}</td><td>额定输出（可调机器用）</td><td>V / A</td></tr>
 * </table>
 *
 * <p>电流为什么两套单位：<b>额定</b>电流是玩家用 +/- 按钮调的档位，本来就是整数 A，直接按 A 同步；
 * <b>实测</b>电流要支持 mA 精度、还可能超过 16 位同步上限，所以用 3 段 15 位字段按 mA 同步。
 *
 * <p>子类想加自己的界面字段（例如发电机的"发电电压"）：在方块实体里覆写
 * {@link AbstractMachineBlockEntity#extraPropertyCount()} 和
 * {@link AbstractMachineBlockEntity#extraProperty(int)}，索引从 {@link #STANDARD_COUNT} 开始。
 */
public final class MachineProperties {
    /** 缓冲区电量低 15 位。 */
    public static final int ENERGY_LOW = 0;
    public static final int ENERGY_MID = 1;
    public static final int ENERGY_HIGH = 2;
    /** 实测输入电压（V）。 */
    public static final int INPUT_VOLTAGE = 3;
    public static final int INPUT_CURRENT_LOW = 4;
    public static final int INPUT_CURRENT_MID = 5;
    public static final int INPUT_CURRENT_HIGH = 6;
    /** 实测输出电压（V）。 */
    public static final int OUTPUT_VOLTAGE = 7;
    public static final int OUTPUT_CURRENT_LOW = 8;
    public static final int OUTPUT_CURRENT_MID = 9;
    public static final int OUTPUT_CURRENT_HIGH = 10;
    /** 额定输入电压（V）。 */
    public static final int RATED_INPUT_VOLTAGE = 11;
    /** 额定输入电流（A，整数档位）。 */
    public static final int RATED_INPUT_CURRENT = 12;
    /** 额定输出电压（V）。 */
    public static final int RATED_OUTPUT_VOLTAGE = 13;
    /** 额定输出电流（A，整数档位）。 */
    public static final int RATED_OUTPUT_CURRENT = 14;
    /** 标准字段个数，子类额外字段从这个索引往后排。 */
    public static final int STANDARD_COUNT = 15;

    private MachineProperties() {
    }

    /** 生成一台机器的完整属性表：标准字段 + 该机器的额外字段。 */
    public static PropertyDelegate delegate(AbstractMachineBlockEntity machine) {
        return new Delegate(machine);
    }

    private static final class Delegate implements PropertyDelegate {
        private final AbstractMachineBlockEntity machine;

        private Delegate(AbstractMachineBlockEntity machine) {
            this.machine = machine;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case ENERGY_LOW -> LongPropertyCodec.field(this.machine.getStoredEnergyMilliJoules(), 0);
                case ENERGY_MID -> LongPropertyCodec.field(this.machine.getStoredEnergyMilliJoules(), 1);
                case ENERGY_HIGH -> LongPropertyCodec.field(this.machine.getStoredEnergyMilliJoules(), 2);
                case INPUT_VOLTAGE -> this.machine.getMeasuredInputVoltage();
                case INPUT_CURRENT_LOW -> LongPropertyCodec.field(this.machine.getMeasuredInputCurrent(), 0);
                case INPUT_CURRENT_MID -> LongPropertyCodec.field(this.machine.getMeasuredInputCurrent(), 1);
                case INPUT_CURRENT_HIGH -> LongPropertyCodec.field(this.machine.getMeasuredInputCurrent(), 2);
                case OUTPUT_VOLTAGE -> this.machine.getMeasuredOutputVoltage();
                case OUTPUT_CURRENT_LOW -> LongPropertyCodec.field(this.machine.getMeasuredOutputCurrent(), 0);
                case OUTPUT_CURRENT_MID -> LongPropertyCodec.field(this.machine.getMeasuredOutputCurrent(), 1);
                case OUTPUT_CURRENT_HIGH -> LongPropertyCodec.field(this.machine.getMeasuredOutputCurrent(), 2);
                case RATED_INPUT_VOLTAGE -> this.machine.getRatedInputVoltage();
                case RATED_INPUT_CURRENT -> this.machine.getRatedInputCurrent() / 1000;
                case RATED_OUTPUT_VOLTAGE -> this.machine.getRatedOutputVoltage();
                case RATED_OUTPUT_CURRENT -> this.machine.getRatedOutputCurrent() / 1000;
                default -> this.machine.extraProperty(index - STANDARD_COUNT);
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端那边用 ArrayPropertyDelegate 接同步包，这个实现只在服务端被读，set 不会走到这里
        }

        @Override
        public int size() {
            return STANDARD_COUNT + this.machine.extraPropertyCount();
        }
    }
}
