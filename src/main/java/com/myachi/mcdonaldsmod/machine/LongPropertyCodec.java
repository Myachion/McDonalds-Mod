package com.myachi.mcdonaldsmod.machine;

/**
 * 把 long 拆成几个小字段，借原版 {@code PropertyDelegate} 同步到界面上。
 *
 * <p>原版 {@link net.minecraft.screen.PropertyDelegate} 的同步包是用 {@code writeShort}
 * 发出去的，也就是说每个字段其实只有 16 位——100000 这种值会被截断成负数。
 * 所以这里把一个 long 拆成 {@link #FIELDS} 个 15 位字段，最多能表示 2^45 焦
 * （约 35 万亿焦），足够"无限储能"用很久了。
 */
public final class LongPropertyCodec {
    /** 占用的属性个数。 */
    public static final int FIELDS = 3;
    private static final int BITS = 15;
    private static final int MASK = (1 << BITS) - 1;

    private LongPropertyCodec() {
    }

    /** 取第 index 段的值。 */
    public static int field(long value, int index) {
        return (int) ((value >>> (BITS * index)) & MASK);
    }

    /** 把三段拼回 long。 */
    public static long combine(int field0, int field1, int field2) {
        return (field0 & MASK)
                | ((long) (field1 & MASK) << BITS)
                | ((long) (field2 & MASK) << (BITS * 2));
    }
}
