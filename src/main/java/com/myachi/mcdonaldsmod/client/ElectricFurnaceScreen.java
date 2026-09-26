package com.myachi.mcdonaldsmod.client;

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
 * 电炉界面：布局和原版熔炉一模一样，只把原版那个"火苗"换成 {@link EnergyBoltIcon 缓冲区闪电图标}。
 *
 * <p>这是"机器界面复用原版布局"的范例：继承对应的原版界面、只重画要改的部分。
 * 以后新机器如果要完全自定义界面，参考 {@code TestGeneratorScreen} 那套（{@code MachineScreen} 基类）。
 */
@Environment(EnvType.CLIENT)
public class ElectricFurnaceScreen extends FurnaceScreen {
    /** 原版熔炉底板（和界面布局完全一致）。 */
    private static final Identifier BACKGROUND = Identifier.ofVanilla("textures/gui/container/furnace.png");
    /** 原版烧制进度箭头。 */
    private static final Identifier BURN_PROGRESS = Identifier.ofVanilla("container/furnace/burn_progress");

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

    private int iconX() {
        return this.x + FLAME_X + (FLAME_SIZE - EnergyBoltIcon.WIDTH) / 2;
    }

    private int iconY() {
        return this.y + FLAME_Y + (FLAME_SIZE - EnergyBoltIcon.HEIGHT) / 2;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.x, this.y, 0.0F, 0.0F,
                this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 盖掉底板上原来那三簇火苗
        context.fill(this.x + FLAME_X, this.y + FLAME_Y,
                this.x + FLAME_X + FLAME_SIZE, this.y + FLAME_Y + FLAME_SIZE, PANEL_COLOR);

        // 缓冲区电量：灰色闪电打底 + 红色按比例从下往上填
        EnergyBoltIcon.draw(context, iconX(), iconY(), this.handler.getFuelProgress());

        // 烧制进度箭头：和原版一样
        int progress = MathHelper.ceil(this.handler.getCookProgress() * ARROW_WIDTH);
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS, ARROW_WIDTH, ARROW_HEIGHT, 0, 0,
                this.x + ARROW_X, this.y + ARROW_Y, progress, ARROW_HEIGHT);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);
        // 注意 isPointWithinBounds 的 x/y 是相对界面左上角的
        if (this.isPointWithinBounds(iconX() - this.x - EnergyBoltIcon.HOVER_MARGIN,
                iconY() - this.y - EnergyBoltIcon.HOVER_MARGIN,
                EnergyBoltIcon.WIDTH + EnergyBoltIcon.HOVER_MARGIN * 2,
                EnergyBoltIcon.HEIGHT + EnergyBoltIcon.HOVER_MARGIN * 2, mouseX, mouseY)) {
            int storedJoules = Math.round(this.handler.getFuelProgress() * ElectricFurnaceBlockEntity.CAPACITY_JOULES);
            EnergyBoltIcon.drawTooltip(context, this.textRenderer,
                    storedJoules, ElectricFurnaceBlockEntity.CAPACITY_JOULES, mouseX, mouseY);
        }
    }
}
