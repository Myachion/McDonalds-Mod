package com.myachi.mcdonaldsmod;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;

public class ModFoodComponent {
    public static final FoodComponent CHEESE = new FoodComponent.Builder().nutrition(4).saturationModifier(0.6F).build();
    public static final FoodComponent TOMATO = new FoodComponent.Builder().nutrition(3).saturationModifier(0.4F).build();
    public static final FoodComponent ONION = new FoodComponent.Builder().nutrition(3).saturationModifier(0.3F).build();
    public static final FoodComponent CHEESE_HAMBURGER = new FoodComponent.Builder().nutrition(10).saturationModifier(0.8F).build();
}
