package com.myachi.mcdonaldsmod.client;

import com.myachi.mcdonaldsmod.machine.MachineNumbers;
import com.myachi.mcdonaldsmod.machine.TestGeneratorBlockEntity;
import com.myachi.mcdonaldsmod.machine.TestGeneratorScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** 测试发电机界面：缓存能量条 + 四组可调参数 + 总输出功率。 */
@Environment(EnvType.CLIENT)
public class TestGeneratorScreen extends MachineScreen<TestGeneratorScreenHandler> {
    /** 面板加宽到 250：底部"实际输出"那行要放下 "7.75 kW (512 V 15.1 A)" 这样的长文本。 */
    private static final int PANEL_WIDTH = 250;
    private static final int PANEL_HEIGHT = 170;
    private static final int BAR_X = 12;
    private static final int BAR_Y = 36;
    private static final int BAR_WIDTH = 226;
    private static final int BAR_HEIGHT = 8;
    /** 四行参数的起始 y，每行间隔 20。 */
    private static final int ROW_Y = 54;
    private static final int ROW_STEP = 20;
    private static final int MINUS_X = 192;
    private static final int PLUS_X = 214;
    private static final int BUTTON_WIDTH = 16;
    private static final int BUTTON_HEIGHT = 16;

    public TestGeneratorScreen(TestGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
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
        TestGeneratorScreenHandler handler = this.handler;

        MachineScreenStyle.centeredTitle(context, this.textRenderer, this.title, PANEL_WIDTH / 2, 10);
        MachineScreenStyle.divider(context, 8, 24, PANEL_WIDTH - 16);

        // 缓存能量
        long energy = handler.storedEnergyMilliJoules();
        String energyText = MachineNumbers.energy(energy) + " / " + MachineNumbers.energy(TestGeneratorBlockEntity.CAPACITY);
        MachineScreenStyle.label(context, this.textRenderer, "gui.mcdonalds-mod.energy_buffer", energyText, 12, 26);
        MachineScreenStyle.bar(context, BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT,
                MachineNumbers.ratio(energy, TestGeneratorBlockEntity.CAPACITY));

        MachineScreenStyle.label(context, this.textRenderer, "gui.mcdonalds-mod.generation_voltage",
                handler.generationVoltage() + " V", 12, ROW_Y);
        MachineScreenStyle.label(context, this.textRenderer, "gui.mcdonalds-mod.generation_current",
                handler.generationCurrent() + " A", 12, ROW_Y + ROW_STEP);
        MachineScreenStyle.label(context, this.textRenderer, "gui.mcdonalds-mod.output_voltage",
                handler.ratedOutputVoltage() + " V", 12, ROW_Y + ROW_STEP * 2);
        MachineScreenStyle.label(context, this.textRenderer, "gui.mcdonalds-mod.output_current",
                handler.ratedOutputCurrentAmps() + " A", 12, ROW_Y + ROW_STEP * 3);

        // 按钮
        drawButtons(context, mouseX, mouseY);

        MachineScreenStyle.divider(context, 8, 132, PANEL_WIDTH - 16);
        // 设定值和实际值分开显示，避免"设了 2.56 kW 就以为真的在输出 2.56 kW"
        context.drawText(this.textRenderer,
                Text.translatable("gui.mcdonalds-mod.rated_output_power").getString() + ":", 12, 140,
                MachineScreenStyle.TEXT, false);
        context.drawText(this.textRenderer, MachineNumbers.power(handler.ratedOutputPowerWatts()), 110, 140,
                MachineScreenStyle.TEXT_DIM, false);

        String actual = MachineNumbers.power(handler.outputPowerMilliWatts() / 1000L)
                + "  (" + handler.outputVoltage() + " V "
                + MachineNumbers.current(handler.outputCurrentMilliAmps()) + ")";
        context.drawText(this.textRenderer,
                Text.translatable("gui.mcdonalds-mod.actual_output_power").getString() + ":", 12, 154,
                MachineScreenStyle.TEXT, false);
        context.drawText(this.textRenderer, actual, 110, 154, MachineScreenStyle.TEXT_VALUE, false);
    }

}
