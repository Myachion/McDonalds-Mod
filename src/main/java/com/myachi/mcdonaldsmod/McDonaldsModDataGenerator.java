package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.datagen.ModBlockTagsProvider;
import com.myachi.mcdonaldsmod.datagen.ModModelsProvider;
import com.myachi.mcdonaldsmod.datagen.MaterialAssetsProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class McDonaldsModDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(ModBlockTagsProvider::new);
        pack.addProvider(ModModelsProvider::new);
        pack.addProvider(MaterialAssetsProvider::new);
	}

}
