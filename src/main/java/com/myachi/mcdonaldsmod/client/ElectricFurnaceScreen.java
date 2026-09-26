package com.myachi.mcdonaldsmod.client;

import com.myachi.mcdonaldsmod.McDonaldsMod;
import com.myachi.mcdonaldsmod.machine.ElectricFurnaceBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * 电炉界面：布局和原版熔炉一模一样，只把原版那个"火苗"换成闪电图标。
 *
 * <p>图标取自素材 {@code resource/texture/gui/common.png} 里的那一对（7×13 像素）：
 * 灰色是底图，红色的按缓冲区电量比例从下往上盖，所以一眼就能看出还剩多少电。
 */
@Environment(EnvType.CLIENT)
public class ElectricFurnaceScreen extends FurnaceScreen {
    /** 原版熔炉底板（和界面布局完全一致）。 */
    private static final Identifier BACKGROUND = Identifier.ofVanilla("textures/gui/container/furnace.png");
    /** 原版烧制进度箭头。 */
    private static final Identifier BURN_PROGRESS = Identifier.ofVanilla("container/furnace/burn_progress");
    private static final Identifier ENERGY_EMPTY =
            Identifier.of(McDonaldsMod.MOD_ID, "textures/gui/electric_furnace/energy_empty.png");
    private static final Identifier ENERGY_FILLED =
            Identifier.of(McDonaldsMod.MOD_ID, "textures/gui/electric_furnace/energy_filled.png");

    /** 图标原始尺寸（素材里那一小块）。 */
    private static final int ICON_WIDTH = 7;
    private static final int ICON_HEIGHT = 13;
    /** 原版火苗的位置与大小：相对界面左上角 (56, 36)，14×14。 */
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_SIZE = 14;
    /** 原版进度箭头的位置与大小。 */
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 16;
    /** 原版熔炉底板里"火苗"位置本来就画了三簇灰色火苗，用面板底色盖掉再画自己的图标。 */
    private static final int PANEL_COLOR = 0xFFC6C6C6;

    public ElectricFurnaceScreen(FurnaceScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.x, this.y, 0.0F, 0.0F,
                this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 盖掉底板上原来那三簇火苗
        context.fill(this.x + FLAME_X, this.y + FLAME_Y, this.x + FLAME_X + FLAME_SIZE, this.y + FLAME_Y + FLAME_SIZE,
                PANEL_COLOR);

        // 图标摆进原来火苗的位置（居中）
        int iconX = this.x + FLAME_X + (FLAME_SIZE - ICON_WIDTH) / 2;
        int iconY = this.y + FLAME_Y + (FLAME_SIZE - ICON_HEIGHT) / 2;
        // 底图：灰色闪电，电量为 0 时也能看到图标
        context.drawTexture(RenderPipelines.GUI_TEXTURED, ENERGY_EMPTY, iconX, iconY,
                0.0F, 0.0F, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);
        // 有电的部分：红色闪电从底部往上按比例裁切
        int filled = MathHelper.ceil(this.handler.getFuelProgress() * ICON_HEIGHT);
        if (filled > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, ENERGY_FILLED,
                    iconX, iconY + ICON_HEIGHT - filled,
                    0.0F, ICON_HEIGHT - filled, ICON_WIDTH, filled, ICON_WIDTH, ICON_HEIGHT);
        }

        // 烧制进度箭头：和原版一样
        int progress = MathHelper.ceil(this.handler.getCookProgress() * ARROW_WIDTH);
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS, ARROW_WIDTH, ARROW_HEIGHT, 0, 0,
                this.x + ARROW_X, this.y + ARROW_Y, progress, ARROW_HEIGHT);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);
        // 悬停闪电图标：显示缓冲区电量，例如 1356 J / 2500 J
        int iconX = this.x + FLAME_X + (FLAME_SIZE - ICON_WIDTH) / 2;
        int iconY = this.y + FLAME_Y + (FLAME_SIZE - ICON_HEIGHT) / 2;
        // 注意 isPointWithinBounds 的 x/y 是相对界面左上角的，不是屏幕坐标
        if (this.isPointWithinBounds(iconX - this.x - 2, iconY - this.y - 2, ICON_WIDTH + 4, ICON_HEIGHT + 4,
                mouseX, mouseY)) {
            int storedJoules = Math.round(this.handler.getFuelProgress() * ElectricFurnaceBlockEntity.CAPACITY_JOULES);
            context.drawTooltip(this.textRenderer,
                    Text.translatable("gui.mcdonalds-mod.electric_furnace.energy",
                            storedJoules, ElectricFurnaceBlockEntity.CAPACITY_JOULES),
                    mouseX, mouseY);
        }
    }
}
