package com.myachi.mcdonaldsmod;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;

public class ModItemGroups {
    public static final ItemGroup MCDONALD_GROUP = FabricItemGroup.builder()
            .icon(()->new ItemStack(ModItems.STEEL_INGOT))
            .displayName(Text.translatable("itemGroup.mcdonalds-mod.mcdonald_group"))
            .entries((context,entries)-> {
                entries.add(ModItems.STEEL_INGOT);
                entries.add(ModItems.IRIDIUM);
                entries.add(ModItems.IRIDIUM_SHARD);
            })
            .build();
    public static void initializeModItemGroups () {
        Registry.register(Registries.ITEM_GROUP, Identifier.of(McDonaldsMod.MOD_ID,"mcdonald_group"),MCDONALD_GROUP);
    }
}
