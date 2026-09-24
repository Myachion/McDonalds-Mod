package com.myachi.mcdonaldsmod.machine;

import java.util.Locale;

/** 机器界面上显示数值用的格式化工具。 */
public final class MachineNumbers {
    private MachineNumbers() {
    }

    /** 能量，统一用 kJ 显示。 */
    public static String energy(long joules) {
        return trim(joules / 1000.0) + " kJ";
    }

    /** 能量占容量的百分比，用来画进度条。 */
    public static double ratio(long value, long capacity) {
        if (capacity <= 0) {
            return 0.0;
        }
        return Math.clamp(value / (double) capacity, 0.0, 1.0);
    }

    /** 功率：W / kW / MW。 */
    public static String power(long watts) {
        if (watts < 1_000) {
            return watts + " W";
        }
        if (watts < 1_000_000) {
            return trim(watts / 1000.0) + " kW";
        }
        return trim(watts / 1_000_000.0) + " MW";
    }

    /** 去掉多余的小数位并加千分位分隔：12.50 -> 12.5，5000000 -> 5,000,000。 */
    private static String trim(double value) {
        if (value >= 100) {
            return String.format(Locale.ROOT, "%,.0f", value);
        }
        if (value >= 10) {
            return String.format(Locale.ROOT, "%,.1f", value);
        }
        return String.format(Locale.ROOT, "%,.2f", value);
    }
}
