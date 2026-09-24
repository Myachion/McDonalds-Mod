package com.myachi.mcdonaldsmod.client;

import com.myachi.mcdonaldsmod.machine.MachineNumbers;
import com.myachi.mcdonaldsmod.machine.TestBatteryBoxBlockEntity;
import com.myachi.mcdonaldsmod.machine.TestBatteryBoxScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** 测试电池盒界面：储电量（无上限）+ 可调的额定输入/输出参数 + 当前实测读数。 */
@Environment(EnvType.CLIENT)
public class TestBatteryBoxScreen extends MachineScreen<TestBatteryBoxScreenHandler> {
    private static final int PANEL_WIDTH = 210;
    private static final int PANEL_HEIGHT = 152;
    /** 四行额定参数的起始 y，每行间隔 18。 */
    private static final int ROW_Y = 48;
    private static final int ROW_STEP = 18;
    private static final int MINUS_X = 152;
    private static final int PLUS_X = 174;
    private static final int BUTTON_WIDTH = 16;
    private static final int BUTTON_HEIGHT = 16;

    public TestBatteryBoxScreen(TestBatteryBoxScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = PANEL_WIDTH;
        this.backgroundHeight = PANEL_HEIGHT;

        for (int row = 0; row < 4; row++) {
            int y = ROW_Y + row * ROW_STEP - 4;
            addButton(MINUS_X, y, BUTTON_WIDTH, BUTTON_HEIGHT, row * 2);
            addButton(PLUS_X, y, BUTTON_WIDTH, BUTTON_HEIGHT, row * 2 + 1);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
        MachineScreenStyle.panel(context, this.x, this.y, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        TestBatteryBoxScreenHandler handler = this.handler;

        MachineScreenStyle.centeredTitle(context, this.textRenderer, this.title, PANEL_WIDTH / 2, 10);
        MachineScreenStyle.divider(context, 8, 24, PANEL_WIDTH - 16);

        long energy = handler.getStoredEnergy();
        // 没有容量上限，所以不画进度条，只报数值
        MachineScreenStyle.label(context, this.textRenderer, "gui.mcdonalds-mod.stored_energy",
                MachineNumbers.energy(energy), 12, 28);

        // 四行额定参数
        MachineScreenStyle.label(context, this.textRenderer, "gui.mcdonalds-mod.rated_input_voltage",
                handler.get(TestBatteryBoxBlockEntity.INDEX_RATED_INPUT_VOLTAGE) + " V", 12, ROW_Y);
        MachineScreenStyle.label(context, this.textRenderer, "gui.mcdonalds-mod.rated_input_current",
                handler.get(TestBatteryBoxBlockEntity.INDEX_RATED_INPUT_CURRENT) + " A", 12, ROW_Y + ROW_STEP);
        MachineScreenStyle.label(context, this.textRenderer, "gui.mcdonalds-mod.rated_output_voltage",
                handler.get(TestBatteryBoxBlockEntity.INDEX_RATED_OUTPUT_VOLTAGE) + " V", 12, ROW_Y + ROW_STEP * 2);
        MachineScreenStyle.label(context, this.textRenderer, "gui.mcdonalds-mod.rated_output_current",
                handler.get(TestBatteryBoxBlockEntity.INDEX_RATED_OUTPUT_CURRENT) + " A", 12, ROW_Y + ROW_STEP * 3);

        drawButtons(context, mouseX, mouseY);

        // 底部：当前实测读数（等电网接进来才会有值，先用灰色小字）
        MachineScreenStyle.divider(context, 8, 118, PANEL_WIDTH - 16);
        String input = Text.translatable("gui.mcdonalds-mod.input").getString() + ":  "
                + handler.get(TestBatteryBoxBlockEntity.INDEX_INPUT_VOLTAGE) + " V   "
                + handler.get(TestBatteryBoxBlockEntity.INDEX_INPUT_CURRENT) + " A   "
                + MachineNumbers.power(handler.getInputPower());
        String output = Text.translatable("gui.mcdonalds-mod.output").getString() + ":  "
                + handler.get(TestBatteryBoxBlockEntity.INDEX_OUTPUT_VOLTAGE) + " V   "
                + handler.get(TestBatteryBoxBlockEntity.INDEX_OUTPUT_CURRENT) + " A   "
                + MachineNumbers.power(handler.getOutputPower());
        context.drawText(this.textRenderer, input, 12, 126, MachineScreenStyle.TEXT_DIM, false);
        context.drawText(this.textRenderer, output, 12, 138, MachineScreenStyle.TEXT_DIM, false);
    }
}
