package com.myachi.mcdonaldsmod.energy;

import net.minecraft.util.math.Direction;

/**
 * 能接入电网的机器：体内有一个能量缓冲区，按"额定输入/输出电压、电流"收发。
 *
 * <p>单位全部是整数，避免浮点误差：
 * <ul>
 *     <li>电压 V</li>
 *     <li>电流 mA</li>
 *     <li>能量 mJ（毫焦）</li>
 *     <li>功率 mW —— 由 {@code 电压(V) × 电流(mA)} 直接得出</li>
 * </ul>
 *
 * <p>一个 tick 是 1/20 秒，所以每 tick 传输的能量（mJ）= 功率（mW）/ 20。
 *
 * <p>机器设计成"缓冲区模式"：外部输电一律往缓冲区里灌，机器自己按耗电从缓冲区取。
 */
public interface EnergyStorage {
    /** 缓冲区里的能量（mJ）。 */
    long getStoredEnergyMilliJoules();

    /** 往缓冲区写，返回实际写入量（mJ）。 */
    long insertEnergy(long millijoules);

    /** 从缓冲区取，返回实际取出量（mJ）。 */
    long extractEnergy(long millijoules);

    /** 额定输入电压（V）。网络电压高于它时，实际按这个值算。 */
    int getRatedInputVoltage();

    /** 额定输入电流（mA）。 */
    int getRatedInputCurrent();

    /** 额定输出电压（V）。 */
    int getRatedOutputVoltage();

    /** 额定输出电流（mA）。 */
    int getRatedOutputCurrent();

    /**
     * 这台机器<b>这一 tick 需要</b>多少输入功率（mW）。
     *
     * <p>默认不限（返回 {@link Long#MAX_VALUE}），由网络按额定功率截断。
     * "缓冲区模式"的机器（{@code AbstractMachineBlockEntity}）覆写成"缓冲区这一 tick 空出来的空间"：
     * <ul>
     *     <li>缓冲区满且不在工作 → 0（网络就一点都不用给它，也不会白白丢掉能量）；</li>
     *     <li>缓冲区满但正在工作 → 正好等于它的耗电（例如 96 W：这一 tick 消耗掉多少，就空出多少）；</li>
     *     <li>缓冲区没满 → 额定功率（一边供自己用电，一边慢慢把缓冲区灌满）。</li>
     * </ul>
     */
    default long getRequestedInputMilliWatts() {
        return Long.MAX_VALUE;
    }

    /** 能不能当电源往外供电。 */
    default boolean canProvideEnergy() {
        return getRatedOutputCurrent() > 0;
    }

    /** 能不能当负载吸收能量。 */
    default boolean canReceiveEnergy() {
        return getRatedInputCurrent() > 0;
    }

    /**
     * 机器的 {@code side} 这一面能不能往外输电。
     *
     * <p>默认六面都能（一般机器都是这样），带专用端口的机器自己覆盖：
     * 电池盒只有贴图上画了圆形端口的那四面能输出，发电机则只有输出口。
     *
     * @param side 机器自己的哪个面（也就是"电缆贴在机器的哪一边"）
     */
    default boolean canProvideEnergyFrom(Direction side) {
        return canProvideEnergy();
    }

    /**
     * 机器的 {@code side} 这一面能不能被充电。
     *
     * <p>默认六面都能（用电器一般六个面都可以充电），电池盒只有上下两面能充。
     */
    default boolean canReceiveEnergyOn(Direction side) {
        return canReceiveEnergy();
    }

    /** 网络结算后回填"当前实测输入"，给界面显示用。 */
    default void setMeasuredInput(int voltage, int currentMilliAmps) {
    }

    /** 网络结算后回填"当前实测输出"，给界面显示用。 */
    default void setMeasuredOutput(int voltage, int currentMilliAmps) {
    }
}
