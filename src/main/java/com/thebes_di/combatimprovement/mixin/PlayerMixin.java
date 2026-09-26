package com.thebes_di.combatimprovement.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thebes_di.combatimprovement.config.ConfigHandler;
import com.thebes_di.combatimprovement.config.ShieldConfigEntry;
import com.thebes_di.combatimprovement.event.EventHandler;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Player.class)
public class PlayerMixin {

    @ModifyExpressionValue(method = "blockUsingShield", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;canDisableShield()Z"))
    private boolean combatimprovement$configurableDisableShield(boolean original) {
        if(!ConfigHandler.newShieldMechanics) return original;
        return original && ConfigHandler.axeDisableShield && ConfigHandler.resistanceMode != ConfigHandler.ResistanceMode.Cooldown;
    }

    /*@WrapOperation(method = "blockUsingShield", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;disableShield()V"))
    private void combatimprovement$customDisableShield(Player self, Operation<Void> original) {
        if (!ConfigHandler.newShieldMechanics || !EventHandler.active) {
            original.call(self);
            return;
        }

        EventHandler.active = false;

        int cooldown = EventHandler.shieldCooldown;

        if (cooldown <= 0) {
            original.call(self);
            return;
        }

        self.getCooldowns().addCooldown(self.getUseItem().getItem(), cooldown);
        self.stopUsingItem();
        EventHandler.shieldCooldown = 0;
        self.level().broadcastEntityEvent(self, (byte)30);
    }*/

    @ModifyArg(method = "disableShield", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V"), index = 1)
    private int combatimprovement$customDisableTime(int original) {
        // 关闭模组机制或没有上下文（不是从我们的格挡流程来的），走原版 100
        if (!ConfigHandler.newShieldMechanics || !EventHandler.active) return original;

        EventHandler.active = false;  // 消费掉，避免影响后续

        // 和 Cooldown 模式同样的公式
        int cooldown = EventHandler.shieldCooldown;
        EventHandler.shieldCooldown = 0;

        // 如果算出来 <= 0，走原版（避免 0 tick 冷却导致盾牌永不禁用）
        return cooldown > 0 ? cooldown : original;
    }

}
