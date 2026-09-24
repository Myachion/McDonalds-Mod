package com.myachi.mcdonaldsmod.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * 机器界面共用的绘制风格：原版配色的浅灰面板 + 立体边框。
 * 全部用代码画，不依赖贴图。
 */
public final class MachineScreenStyle {
    public static final int PANEL = 0xFFC6C6C6;
    public static final int HIGHLIGHT = 0xFFFFFFFF;
    public static final int SHADOW = 0xFF555555;
    // 注意：1.21.11 的渲染管线会真的使用颜色里的 alpha，文本颜色必须带 0xFF 前缀，
    // 否则 alpha=0 会让文字完全透明（方块填充和文字不一样，fill 的 alpha 是显式给的）。
    public static final int TEXT = 0xFF404040;
    public static final int TEXT_VALUE = 0xFF1A3A6B;
    public static final int TEXT_DIM = 0xFF606060;
    public static final int BAR_TRACK = 0xFF8B8B8B;
    public static final int BAR_FILL = 0xFF3CB043;
    public static final int BUTTON = 0xFF9E9E9E;
    public static final int BUTTON_HOVER = 0xFFBDBDBD;
    public static final int BUTTON_PRESSED = 0xFF8A8A8A;
    public static final int BUTTON_TEXT = 0xFFFFFFFF;

    private MachineScreenStyle() {
    }

    /** 画面板：整块浅灰底 + 内凹边框。 */
    public static void panel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, PANEL);
        // 上、左边框高光
        context.fill(x, y, x + width, y + 1, HIGHLIGHT);
        context.fill(x, y, x + 1, y + height, HIGHLIGHT);
        // 下、右边框阴影
        context.fill(x, y + height - 1, x + width, y + height, SHADOW);
        context.fill(x + width - 1, y, x + width, y + height, SHADOW);
    }

    /** 标题下面的一条分隔线。 */
    public static void divider(DrawContext context, int x, int y, int width) {
        context.fill(x, y, x + width, y + 1, SHADOW);
        context.fill(x, y + 1, x + width, y + 2, HIGHLIGHT);
    }

    /** 进度条。ratio 是 0~1。 */
    public static void bar(DrawContext context, int x, int y, int width, int height, double ratio) {
        context.fill(x, y, x + width, y + height, SHADOW);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, BAR_TRACK);
        int filled = (int) Math.round((width - 2) * Math.clamp(ratio, 0.0, 1.0));
        if (filled > 0) {
            context.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, BAR_FILL);
        }
    }

    /** 小按钮，返回是否是悬停状态。pressed 时把边框反过来画，做出"按下去"的效果。 */
    public static boolean button(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer,
                                 int x, int y, int width, int height, String label, int mouseX, int mouseY, boolean pressed) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        int background = pressed ? BUTTON_PRESSED : (hovered ? BUTTON_HOVER : BUTTON);
        context.fill(x, y, x + width, y + height, background);
        int topLeft = pressed ? SHADOW : HIGHLIGHT;
        int bottomRight = pressed ? HIGHLIGHT : SHADOW;
        context.fill(x, y, x + width, y + 1, topLeft);
        context.fill(x, y, x + 1, y + height, topLeft);
        context.fill(x, y + height - 1, x + width, y + height, bottomRight);
        context.fill(x + width - 1, y, x + width, y + height, bottomRight);
        int textWidth = textRenderer.getWidth(label);
        context.drawText(textRenderer, label, x + (width - textWidth) / 2, y + (height - 8) / 2 + (pressed ? 1 : 0),
                BUTTON_TEXT, false);
        return hovered;
    }

    /** 一行 "标签: 值"。原版容器界面里的文字不加阴影，这里保持一致，看起来更清楚。 */
    public static void label(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer,
                             String labelKey, String value, int x, int y) {
        String name = Text.translatable(labelKey).getString();
        context.drawText(textRenderer, name + ":", x, y, TEXT, false);
        context.drawText(textRenderer, value, x + 76, y, TEXT_VALUE, false);
    }

    /** 居中标题。 */
    public static void centeredTitle(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer,
                                     net.minecraft.text.Text title, int centerX, int y) {
        int width = textRenderer.getWidth(title);
        context.drawText(textRenderer, title, centerX - width / 2, y, TEXT, false);
    }
}
