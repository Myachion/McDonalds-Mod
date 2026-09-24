package com.myachi.mcdonaldsmod.datagen;
import com.jcraft.jorbis.Block;
import com.myachi.mcdonaldsmod.McDonaldsMod;
import com.myachi.mcdonaldsmod.ModBlockTags;
import com.myachi.mcdonaldsmod.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;


import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends FabricTagProvider.BlockTagProvider {


    public ModBlockTagsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        valueLookupBuilder(ModBlockTags.MACHINE)
                .add(ModBlocks.MACHINE_SHELL)
                .add(ModBlocks.TEST_GENERATOR)
                .add(ModBlocks.TEST_BATTERY_BOX)
                .setReplace(true);


    }
}
