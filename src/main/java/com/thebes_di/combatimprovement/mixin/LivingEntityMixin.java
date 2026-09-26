package com.thebes_di.combatimprovement.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.thebes_di.combatimprovement.CombatImprovement;
import com.thebes_di.combatimprovement.config.ConfigHandler;
import com.thebes_di.combatimprovement.config.ShieldConfigEntry;
import com.thebes_di.combatimprovement.event.EventHandler;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "blockedByShield", at = @At("HEAD"), cancellable = true)
    private void combatimprovement$fixShieldKnockback(LivingEntity defender, CallbackInfo ci) {
        /*ItemStack item = defender.getUseItem();
        if (!item.canPerformAction(ItemAbilities.SHIELD_BLOCK)) return;
        ShieldConfigEntry config = ShieldConfigEntry.getShieldConfigFor(useItem);
        if (config == null) return;*/

        // 手动执行正确的击退逻辑
        // 'this' 是被Mixin注入的当前实体，即攻击者
        LivingEntity attacker = (LivingEntity) (Object) this;
        attacker.knockback(0.5D, defender.getX() - attacker.getX(), defender.getZ() - attacker.getZ());

        // 取消原版的错误逻辑
        ci.cancel();
    }

    @WrapOperation(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V"))
    private void combatimprovement$handleShieldCooldown(LivingEntity entity, float shieldDamage, Operation<Void> original, @Local(argsOnly = true) DamageSource source, @Local(argsOnly = true) float f) {
        // 先执行原版盾牌耐久处理
        original.call(entity, shieldDamage);
        // 之后才处理 Cooldown
        if (!ConfigHandler.newShieldMechanics || !(entity instanceof Player player)) {
            return;
        }
        if (ConfigHandler.resistanceMode != ConfigHandler.ResistanceMode.Cooldown) {
            return;
        }
        ItemStack shield = entity.getUseItem();
        ShieldConfigEntry config = ShieldConfigEntry.getShieldConfigFor(shield);

        if (config == null) {
            return;
        }

        Entity attacker = source.getEntity();
        float resistance = EventHandler.calculateFinalResistance(attacker,source,config);
        float originalDamage = f;

        if (originalDamage <= resistance) {
            // 没有达到触发 Cooldown 的阈值
            return;
        }

        int cooldown = EventHandler.calculateShieldCooldown(originalDamage,resistance);

        // 注意：这里已经在 hurtCurrentlyUsedShield() 之后
        player.getCooldowns().addCooldown(shield.getItem(), cooldown);
        entity.stopUsingItem();

        // Cooldown 模式下的盾牌破防表现
        entity.level().broadcastEntityEvent(entity, (byte)30);
    }

    /*@Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void combatimprovement$customShieldDelay(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack item = entity.getUseItem();
        if (item.canPerformAction(ItemAbilities.SHIELD_BLOCK)) return;
        ShieldConfigEntry config = ShieldConfigEntry.getShieldConfigFor(item);
        if (config == null) return;
        if (!entity.isUsingItem()) return;

        int delay = Math.max(0, config.delay);
        if (entity.getTicksUsingItem() < delay) {
            cir.setReturnValue(false);  // 延迟未到，不算格挡
        } else {
            cir.setReturnValue(true);   // 延迟已到，格挡生效
        }
    }*/

    /*@ModifyConstant(method = "isBlocking", constant = @Constant(intValue = 5))
    private int combatimprovement$customShieldDelay(int constant) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack item = entity.getUseItem();
        ShieldConfigEntry config = ShieldConfigEntry.getShieldConfigFor(item);
        if (config == null) return constant;
        return Math.max(0, config.delay);
    }*/
}
