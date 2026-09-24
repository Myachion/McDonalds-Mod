package com.myachi.mcdonaldsmod.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * 机器界面基类：提供一组成对的 +/- 按钮。
 *
 * <p>按钮 id 的约定是"偶数减、奇数加"，所以第 n 组按钮用 {@code 2n} 和 {@code 2n + 1}，
 * 和各个 ScreenHandler 里定义的常量一一对应。
 *
 * <p>注意命中判定必须放在 {@code super.mouseClicked} 之前：原版
 * {@link HandledScreen#mouseClicked} 只要点在 GUI 范围内几乎总会返回 true，会把点击吞掉。
 */
@Environment(EnvType.CLIENT)
public abstract class MachineScreen<T extends ScreenHandler> extends HandledScreen<T> {
    public record ButtonArea(int x, int y, int width, int height, int id) {
    }

    private final List<ButtonArea> buttons = new ArrayList<>();
    private int pressedButton = -1;

    protected MachineScreen(T handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    /** 登记一个按钮，坐标是面板内的局部坐标。 */
    protected void addButton(int x, int y, int width, int height, int id) {
        this.buttons.add(new ButtonArea(x, y, width, height, id));
    }

    /** 画全部按钮（局部坐标，放在 drawForeground 里调用）。 */
    protected void drawButtons(DrawContext context, int mouseX, int mouseY) {
        for (ButtonArea button : this.buttons) {
            boolean plus = button.id() % 2 == 1;
            MachineScreenStyle.button(context, this.textRenderer, button.x(), button.y(), button.width(), button.height(),
                    plus ? "+" : "-", mouseX, mouseY, button.id() == this.pressedButton);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            double localX = click.x() - this.x;
            double localY = click.y() - this.y;
            for (ButtonArea button : this.buttons) {
                if (localX >= button.x() && localX < button.x() + button.width()
                        && localY >= button.y() && localY < button.y() + button.height()) {
                    this.pressedButton = button.id();
                    if (this.client != null && this.client.interactionManager != null) {
                        this.client.interactionManager.clickButton(this.handler.syncId, button.id());
                        this.client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        this.pressedButton = -1;
        return super.mouseReleased(click);
    }
}
