package com.myachi.mcdonaldsmod;

import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;

import java.util.List;

public class ModConsumableComponents {
    public static final ConsumableComponent FULL_FIJI_CUP = ConsumableComponents.food()
            .consumeEffect(
                    new ApplyEffectsConsumeEffect(
                            List.of(
                                    new StatusEffectInstance(StatusEffects.POISON, 600, 1),
                                    new StatusEffectInstance(StatusEffects.NAUSEA, 600, 0),
                                    new StatusEffectInstance(StatusEffects.BLINDNESS, 400, 0)
                                    //new StatusEffectInstance(StatusEffects.HASTE, 200, 0)
                            )
                    )
            )
            .build();
    public static final ConsumableComponent CHUM = ConsumableComponents.food()
            .consumeEffect(
                    new ApplyEffectsConsumeEffect(
                            List.of(
                                    new StatusEffectInstance(StatusEffects.POISON, 300, 3),
                                    new StatusEffectInstance(StatusEffects.NAUSEA, 800, 0)
                            )
                    )
            )
            .build();

}
