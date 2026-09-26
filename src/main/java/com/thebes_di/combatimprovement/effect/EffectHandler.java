package com.thebes_di.combatimprovement.effect;

import com.thebes_di.combatimprovement.CombatImprovement;
import com.thebes_di.combatimprovement.config.ConfigHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

public class EffectHandler {
    public static void applyPotionTweaks() {
        MobEffects.DAMAGE_BOOST.value().addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                ResourceLocation.withDefaultNamespace("effect.strength"),
                switch (ConfigHandler.strengthMode){
                    case Addition -> AttributeModifier.Operation.ADD_VALUE;
                    case Multiplied_Base -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    case Multiplied_Total -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                },
                amplifier -> ConfigHandler.strengthPerLevel * (amplifier + 1)
        );

        MobEffects.WEAKNESS.value().addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                ResourceLocation.withDefaultNamespace("effect.weakness"),
                switch (ConfigHandler.weaknessMode){
                    case Addition -> AttributeModifier.Operation.ADD_VALUE;
                    case Multiplied_Base -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    case Multiplied_Total -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                },
                amplifier -> {
                    double weakness = ConfigHandler.weaknessPerLevel;
                    if (ConfigHandler.weaknessExponential && ConfigHandler.weaknessMode != ConfigHandler.WeaknessMode.Addition) {
                        return Math.pow(1.0 - weakness, amplifier + 1) - 1.0;
                    }
                    return -weakness * (amplifier + 1);
                }
        );
    }
}
