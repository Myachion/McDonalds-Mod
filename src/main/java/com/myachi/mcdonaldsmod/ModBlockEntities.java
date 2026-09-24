package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.machine.TestBatteryBoxBlockEntity;
import com.myachi.mcdonaldsmod.machine.TestGeneratorBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static final BlockEntityType<TestGeneratorBlockEntity> TEST_GENERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(McDonaldsMod.MOD_ID, "test_generator"),
            FabricBlockEntityTypeBuilder.create(TestGeneratorBlockEntity::new, ModBlocks.TEST_GENERATOR).build()
    );

    public static final BlockEntityType<TestBatteryBoxBlockEntity> TEST_BATTERY_BOX = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(McDonaldsMod.MOD_ID, "test_battery_box"),
            FabricBlockEntityTypeBuilder.create(TestBatteryBoxBlockEntity::new, ModBlocks.TEST_BATTERY_BOX).build()
    );

    public static void initializeModBlockEntities() {
        McDonaldsMod.LOGGER.info("Registry ModBlockEntities!");
    }
}
