package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.datagen.ModBlockTagsProvider;
import com.myachi.mcdonaldsmod.datagen.ModModelsProvider;
import com.myachi.mcdonaldsmod.datagen.MaterialAssetsProvider;
import com.myachi.mcdonaldsmod.datagen.SmallDustAssetsProvider;
import com.myachi.mcdonaldsmod.datagen.CrushedOreAssetsProvider;
import com.myachi.mcdonaldsmod.datagen.CasingAssetsProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class McDonaldsModDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(ModBlockTagsProvider::new);
        pack.addProvider(ModModelsProvider::new);
        pack.addProvider(MaterialAssetsProvider::new);
        // 粉：大粉的物品模型 + 小撮粉的定义/模型/9↔1 配方
        pack.addProvider(SmallDustAssetsProvider::new);
        // 粉碎矿：定义/模型 + 粗矿+研钵 → 粉碎矿 配方
        pack.addProvider(CrushedOreAssetsProvider::new);
        // 外壳：定义/模型（贴图由用户提供）
        pack.addProvider(CasingAssetsProvider::new);
	}

}
