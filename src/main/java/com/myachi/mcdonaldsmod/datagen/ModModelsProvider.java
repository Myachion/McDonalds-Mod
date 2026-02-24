package com.myachi.mcdonaldsmod.datagen;

import com.myachi.mcdonaldsmod.ModBlocks;
import com.myachi.mcdonaldsmod.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.BlockStateModelGenerator;
import net.minecraft.client.data.ItemModelGenerator;
import net.minecraft.client.data.Models;

public class ModModelsProvider extends FabricModelProvider {

    public ModModelsProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.MACHINE_SHELL);
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.CHUM, Models.GENERATED);
        itemModelGenerator.register(ModItems.CHUM_ON_STICK, Models.GENERATED);
        itemModelGenerator.register(ModItems.STEEL_MORTAR, Models.GENERATED);
        itemModelGenerator.register(ModItems.STONE_MORTAR, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_MORTAR, Models.GENERATED);
    }

}
