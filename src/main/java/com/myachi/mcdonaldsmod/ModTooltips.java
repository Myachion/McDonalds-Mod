package com.myachi.mcdonaldsmod;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;

public class ModTooltips {
    public static void itemTooltipInitializer(){
        ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipType, list) -> {
            if (!itemStack.isOf(ModItems.STEEL_INGOT)) {
                return;
            }
            list.add(Text.translatable("item.mcdonalds-mod.steel_ingot.tooltip"));
        });
    }
}
