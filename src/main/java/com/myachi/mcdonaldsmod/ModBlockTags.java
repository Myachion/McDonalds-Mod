package com.myachi.mcdonaldsmod;

import net.fabricmc.fabric.api.tag.FabricTagKey;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModBlockTags {
    public static final TagKey<Block> MACHINE = of("machine");
    /** 所有电缆方块。电缆之间只看这个标签，互相自动连接。 */
    public static final TagKey<Block> CABLE = of("cable");
    /** 不需要区分面的机器：标签里的方块六面都能接电缆。 */
    public static final TagKey<Block> CABLE_CONNECTABLE = of("cable_connectable");


    private static TagKey<Block> of(String id) {
        return TagKey.of(RegistryKeys.BLOCK,Identifier.of(McDonaldsMod.MOD_ID,id));
    }

    public static void initializeModBlockTags() {
        McDonaldsMod.LOGGER.info("Registry ModBlocksTags!");
    }


}
