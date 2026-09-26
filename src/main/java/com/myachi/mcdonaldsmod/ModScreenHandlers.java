package com.myachi.mcdonaldsmod;

import com.myachi.mcdonaldsmod.machine.TestBatteryBoxScreenHandler;
import com.myachi.mcdonaldsmod.machine.TestGeneratorScreenHandler;
import com.myachi.mcdonaldsmod.machine.ElectricFurnaceScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final String TEST_GENERATOR_TITLE_KEY = "gui.mcdonalds-mod.test_generator.title";
    public static final String TEST_BATTERY_BOX_TITLE_KEY = "gui.mcdonalds-mod.test_battery_box.title";

    public static final ScreenHandlerType<TestGeneratorScreenHandler> TEST_GENERATOR = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(McDonaldsMod.MOD_ID, "test_generator"),
            new ScreenHandlerType<>(TestGeneratorScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<TestBatteryBoxScreenHandler> TEST_BATTERY_BOX = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(McDonaldsMod.MOD_ID, "test_battery_box"),
            new ScreenHandlerType<>(TestBatteryBoxScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    /** 电炉界面暂时直接复用原版熔炉那套（布局 + 贴图），只多接一个取出产物时的经验结算。 */
    public static final ScreenHandlerType<ElectricFurnaceScreenHandler> ELECTRIC_FURNACE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(McDonaldsMod.MOD_ID, "electric_furnace"),
            new ScreenHandlerType<>(ElectricFurnaceScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static void initializeModScreenHandlers() {
        McDonaldsMod.LOGGER.info("Registry ModScreenHandlers!");
    }
}
