package com.myachi.mcdonaldsmod;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    public static final SoundEvent VILLAGER_MOAN = registerSound("villager_moan");


    private static SoundEvent registerSound(String id) {
        Identifier identifier = Identifier.of(McDonaldsMod.MOD_ID, id);
        return Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
    }

    public static void initializeModSounds() {
        McDonaldsMod.LOGGER.info("Registry ModSounds!");
    }
}
