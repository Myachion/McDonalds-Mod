package com.myachi.mcdonaldsmod.machine;

import java.util.Locale;

/** 机器界面上显示数值用的格式化工具。 */
public final class MachineNumbers {
    private MachineNumbers() {
    }

    /** 能量，输入毫焦，统一用 kJ 显示。 */
    public static String energy(long millijoules) {
        if (millijoules % 1_000_000L == 0) {
            return (millijoules / 1_000_000L) + " kJ";
        }
        return trim(millijoules / 1_000_000.0) + " kJ";
    }

    /** 电流，输入毫安，最多显示两位小数。 */
    public static String current(int milliamps) {
        if (milliamps % 1000 == 0) {
            return (milliamps / 1000) + " A";
        }
        return trim(milliamps / 1000.0) + " A";
    }

    /** 电阻，输入毫欧，最多显示三位小数。 */
    public static String ohms(int milliohms) {
        if (milliohms == 0) {
            return "0";
        }
        if (milliohms % 1000 == 0) {
            return String.valueOf(milliohms / 1000);
        }
        if (milliohms % 10 == 0) {
            return String.format(Locale.ROOT, "%.2f", milliohms / 1000.0);
        }
        return String.format(Locale.ROOT, "%.3f", milliohms / 1000.0);
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
