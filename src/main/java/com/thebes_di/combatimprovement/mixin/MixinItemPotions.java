package com.thebes_di.combatimprovement.mixin;

import com.thebes_di.combatimprovement.config.ConfigHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class MixinItemPotions {

    @Mixin(ThrowablePotionItem.class)
    public abstract static class MixinItemThrowablePotion {

        @Inject(method="use", at=@At("RETURN"))
        private void onUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
            InteractionResultHolder<ItemStack> result = cir.getReturnValue();
            if (result == null || result.getResult() == InteractionResult.FAIL) return;
            player.getCooldowns().addCooldown(Items.SPLASH_POTION, ConfigHandler.cooldownThrowablePotion);
            player.getCooldowns().addCooldown(Items.LINGERING_POTION, ConfigHandler.cooldownThrowablePotion);
        }
    }

    /*@Mixin(SplashPotionItem.class)
    public abstract static class MixinItemSplashPotion {

        @Inject(method="use", at=@At("RETURN"))
        private void onUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
            Item item = player.getItemInHand(hand).getItem();
            player.getCooldowns().addCooldown(item, ConfigHandler.cooldownSplashPotion);
        }
    }

    @Mixin(LingeringPotionItem.class)
    public abstract static class MixinLingeringPotionItem {

        @Inject(method = "use", at = @At("RETURN"))
        private void onUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
            Item item = player.getItemInHand(hand).getItem();
            player.getCooldowns().addCooldown(item, ConfigHandler.cooldownLingeringPotion);
        }
    }*/
}