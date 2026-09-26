package com.myachi.mcdonaldsmod.client;

import com.myachi.mcdonaldsmod.McDonaldsMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * 机器界面里表示缓冲区电量的"闪电"图标。
 *
 * <p>图标素材来自 {@code resource/texture/gui/common.png} 里那一对（各 7x13 像素，
 * 轮廓完全一样，只差配色）：灰色的常驻打底，红色的按电量比例从下往上盖。
 * 一般机器界面直接用 {@link #draw} + {@link #isHovered} + {@link #drawTooltip} 三件套即可。
 *
 * <p>注意：{@code HandledScreen#isPointWithinBounds} 的 x/y 是<b>相对界面左上角</b>的坐标，
 * 所以那里要传 {@code iconX - this.x}、{@code iconY - this.y}，不要直接传屏幕坐标。
 */
@Environment(EnvType.CLIENT)
public final class EnergyBoltIcon {
    private static final Identifier TEXTURE_EMPTY =
            Identifier.of(McDonaldsMod.MOD_ID, "textures/gui/electric_furnace/energy_empty.png");
    private static final Identifier TEXTURE_FILLED =
            Identifier.of(McDonaldsMod.MOD_ID, "textures/gui/electric_furnace/energy_filled.png");

    public static final int WIDTH = 7;
    public static final int HEIGHT = 13;
    /** 判定悬停时四周放宽几个像素，免得小图标太难点中。 */
    public static final int HOVER_MARGIN = 2;

    private EnergyBoltIcon() {
    }

    /** 画图标：{@code ratio} 是 0~1 的填充比例（一般直接传界面的 fuel progress）。 */
    public static void draw(DrawContext context, int x, int y, float ratio) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE_EMPTY, x, y, 0.0F, 0.0F,
                WIDTH, HEIGHT, WIDTH, HEIGHT);
        int filled = MathHelper.ceil(MathHelper.clamp(ratio, 0.0F, 1.0F) * HEIGHT);
        if (filled > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE_FILLED,
                    x, y + HEIGHT - filled, 0.0F, HEIGHT - filled, WIDTH, filled, WIDTH, HEIGHT);
        }
    }

    /** 悬停判定（屏幕绝对坐标）。 */
    public static boolean isHovered(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x - HOVER_MARGIN && mouseX < x + WIDTH + HOVER_MARGIN
                && mouseY >= y - HOVER_MARGIN && mouseY < y + HEIGHT + HOVER_MARGIN;
    }

    /** 悬停提示，例如"缓冲区：1536 J / 2500 J"。 */
    public static void drawTooltip(DrawContext context, TextRenderer textRenderer,
                                   long storedJoules, long capacityJoules, int mouseX, int mouseY) {
        context.drawTooltip(textRenderer,
                Text.translatable("gui.mcdonalds-mod.machine.energy", storedJoules, capacityJoules),
                mouseX, mouseY);
    }
}
