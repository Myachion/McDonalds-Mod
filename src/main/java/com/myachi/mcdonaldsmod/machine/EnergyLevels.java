package com.myachi.mcdonaldsmod.machine;

/**
 * 电压 / 电流的档位表，发电机和电池盒共用。
 *
 * <p>按钮只负责"往前/往后挪一格"，两端都是循环的：到顶再点回到最低档，到底再点绕到最高档。
 */
public final class EnergyLevels {
    /** 电压等级（V）：锡 32 / 铜·钢 128 / 金 512 / 铁 2048 / 玻璃纤维 8192。 */
    public static final int[] VOLTAGE = {32, 128, 512, 2048, 8192};
    /** 电流档位（A）：0 起步，50 A 是上限。 */
    public static final int[] CURRENT = {0, 1, 2, 3, 5, 10, 15, 20, 50};
    /** 电流上限（A）。 */
    public static final int MAX_CURRENT = CURRENT[CURRENT.length - 1];

    private EnergyLevels() {
    }

    /** 在档位表里挪 delta 格，超出两端就绕回去。 */
    public static int cycle(int[] levels, int value, int delta) {
        return levels[Math.floorMod(indexOf(levels, value) + delta, levels.length)];
    }

    /** 找到 value 在档位表里的位置；找不到就取第一个不小于它的档位（读旧存档时用）。 */
    public static int indexOf(int[] levels, int value) {
        for (int i = 0; i < levels.length; i++) {
            if (levels[i] >= value) {
                return i;
            }
        }
        return levels.length - 1;
    }

    /** 把任意值吸附到最近的档位。 */
    public static int snap(int[] levels, int value) {
        return levels[indexOf(levels, value)];
    }
}
