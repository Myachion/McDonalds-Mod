package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.energy.CableBlock;
import com.myachi.mcdonaldsmod.machine.MachineNumbers;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.BlockItem;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ModTooltips {
    public static void itemTooltipInitializer(){
        ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipType, list) -> {
            if (itemStack.isOf(ModItems.STEEL_INGOT)) {
                list.add(Text.translatable("item.mcdonalds-mod.steel_ingot.tooltip"));
            }
            // 电缆：把承载能力和线损写在提示里，方便挑线
            if (itemStack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof CableBlock cable) {
                list.add(Text.translatable("item.mcdonalds-mod.cable.voltage", cable.getVoltage() + " V")
                        .formatted(Formatting.GRAY));
                list.add(Text.translatable("item.mcdonalds-mod.cable.current",
                        MachineNumbers.current(cable.getRatedCurrentMilliAmps())).formatted(Formatting.GRAY));
                list.add(Text.translatable("item.mcdonalds-mod.cable.resistance",
                        MachineNumbers.ohms(cable.getResistanceMilliOhms())).formatted(Formatting.GRAY));
            }
            if (itemStack.isOf(ModItems.METER)) {
                list.add(Text.translatable("item.mcdonalds-mod.meter.tooltip").formatted(Formatting.GRAY));
            }
        });
    }
}
