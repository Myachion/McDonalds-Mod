package com.myachi.mcdonaldsmod.specialItem;

import net.minecraft.item.Item;

/**
 * A tool that is used up as a recipe ingredient.
 *
 * <p>Each craft it takes part in swaps it for a copy with one more point of damage,
 * and once the last point is spent it is consumed entirely. The behaviour itself
 * lives in {@link AbstractDurabilityItem}; this class only exists so the tool can be
 * registered like any other item. The mod's mortars work the same way.
 */
public class DurableCraftingTool extends Item implements AbstractDurabilityItem {
    public DurableCraftingTool(Item.Settings settings) {
        super(settings);
    }
}
