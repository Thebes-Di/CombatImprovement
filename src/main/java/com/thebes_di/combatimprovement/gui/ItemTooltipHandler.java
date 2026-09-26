package com.thebes_di.combatimprovement.gui;


import com.thebes_di.combatimprovement.CombatImprovement;

import com.thebes_di.combatimprovement.config.ConfigHandler;
import com.thebes_di.combatimprovement.config.ShieldConfigEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = CombatImprovement.MODID, value = Dist.CLIENT)
public class ItemTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if(stack.canPerformAction(ItemAbilities.SHIELD_BLOCK) && ConfigHandler.newShieldMechanics) {
            ShieldConfigEntry config = ShieldConfigEntry.getShieldConfigFor(stack);
            if (config == null) return;

            event.getToolTip().add(Component.empty());
            String value;
            if (ConfigHandler.resistanceMode == ConfigHandler.ResistanceMode.Percent) {
                value = String.format("%.1f%%", config.resistance);
            } else {
                value = String.format("%.1f", config.resistance);
            }
            event.getToolTip().add(Component.translatable("combatimprovement.tooltip.shield.resistance",
                            value)
                    .withStyle(ChatFormatting.GREEN));
            event.getToolTip().add(Component.translatable("combatimprovement.tooltip.shield.angle",
                            String.format("%.0f", config.angle))
                    .withStyle(ChatFormatting.GREEN));
            event.getToolTip().add(Component.translatable("combatimprovement.tooltip.shield.delay",
                            config.delay)
                    .withStyle(ChatFormatting.RED));
        }
        if (ConfigHandler.twoHandedWeapons.containsKey(itemId)) {
            int type = ConfigHandler.twoHandedWeapons.get(itemId);
            event.getToolTip().add(Component.empty());
            String key = (type == 1)
                    ? "combatimprovement.tooltip.weapon.semiTwoHanded"
                    : "combatimprovement.tooltip.weapon.twoHanded";
            event.getToolTip().add(Component.translatable(key)
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}