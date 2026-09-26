package com.thebes_di.combatimprovement.event;

import com.thebes_di.combatimprovement.CombatImprovement;
import com.thebes_di.combatimprovement.config.ConfigHandler;
import com.thebes_di.combatimprovement.config.ShieldConfigEntry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.joml.Vector3d;

import java.util.Objects;


@EventBusSubscriber(modid = CombatImprovement.MODID)
public class EventHandler {

    public static boolean active = false;
    public static int shieldCooldown = 0;

    private static boolean isBlocking(LivingEntity entity) {
        if (entity.isUsingItem() && !entity.getUseItem().isEmpty()) {
            Item item = entity.getUseItem().getItem();
            int delay = 5;
            ShieldConfigEntry config = ShieldConfigEntry.getShieldConfigFor(entity.getUseItem());
            if (config != null){
                delay = config.delay;
            }
            return entity.getUseItem().canPerformAction(ItemAbilities.SHIELD_BLOCK) && item.getUseDuration(entity.getUseItem(), entity) - entity.getUseItemRemainingTicks() >= delay;
        } else {
            return false;
        }
    }

    private static boolean isDamageSourceBlocked(LivingEntity defender, DamageSource damageSource, float shieldAngle) {
        Entity entity = damageSource.getDirectEntity();
        boolean flag = false;
        if (entity instanceof AbstractArrow abstractarrow && abstractarrow.getPierceLevel() > 0) {
            flag = true;
        }

        if (!damageSource.is(DamageTypeTags.BYPASSES_SHIELD) && isBlocking(defender) && !flag){
            if (shieldAngle >= 360) return true;
            Vec3 vec32 = damageSource.getSourcePosition();
            if (vec32 != null) {
                Vec3 vec3 = defender.calculateViewVector(0.0F, defender.getYHeadRot());
                Vec3 vec31 = vec32.vectorTo(defender.position());
                vec31 = new Vec3(vec31.x, 0.0, vec31.z).normalize();

                double threshold = -Math.cos(Math.toRadians(shieldAngle / 2.0));
                return vec31.dot(vec3) < threshold;
            }
        }
        return false;
    }

    public static int calculateShieldCooldown(float damage, float resistance) {
        if (damage <= resistance) {
            return 0;
        }
        return (int) Math.max(ConfigHandler.shieldCooldownMin, Math.min(ConfigHandler.shieldCooldownMax, (damage - resistance) * ConfigHandler.shieldCooldownScaling));
    }

    public static float calculateFinalResistance(Entity attacker, DamageSource source, ShieldConfigEntry config) {
        float resistance = config.resistance;

        if (attacker instanceof LivingEntity living && living.canDisableShield()) {
            resistance *= ConfigHandler.breakShieldResistanceFactor;
        }

        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            resistance *= ConfigHandler.projectileDamageBlockFactor;
        }

        return resistance;
    }

    private static void playerBlockSound(LivingEntity entity, boolean fullyBlocked){
        if (fullyBlocked) return;
        entity.level().broadcastEntityEvent(entity, (byte)29);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingShieldBlock(LivingShieldBlockEvent event) {
        if(!ConfigHandler.newShieldMechanics) return;
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide()) return;
        if (ConfigHandler.onlyPlayer && !(entity instanceof Player)) return;
        DamageSource source = event.getDamageSource();
        if (!(entity.getUseItem().canPerformAction(ItemAbilities.SHIELD_BLOCK))) return;

        ItemStack shield = entity.getUseItem();
        ShieldConfigEntry config = ShieldConfigEntry.getShieldConfigFor(shield);
        if (config == null) return;

        //角度和穿盾判定
        if (!isDamageSourceBlocked(entity, source, config.angle)) {
            // 不在防御角度内或可穿透盾牌 → 取消格挡，让原版继续走普通受伤流程
            event.setBlocked(false);
            return;
        } else {
            event.setBlocked(true);
        }

        float original = event.getOriginalBlockedDamage();              // 原版准备挡下的伤害
        float finalDamage = 0;
        float finalBlockedDamage = original;
        Entity attacker = source.getEntity();
        float finalResistance = calculateFinalResistance(attacker,source,config);
        /*float finalResistance = config.resistance;
        if (attacker instanceof LivingEntity && ((LivingEntity) attacker).canDisableShield()){
            finalResistance *= ConfigHandler.breakShieldResistanceFactor;
        }

        if (source.is(DamageTypeTags.IS_PROJECTILE)){
            finalResistance *= ConfigHandler.projectileDamageBlockFactor;
        }*/

        switch (ConfigHandler.resistanceMode){
            case Subtraction -> {
                if (!source.is(DamageTypeTags.IS_PROJECTILE) || !ConfigHandler.blockAllProjectileDamage){
                    if (original > finalResistance){
                        finalDamage = Math.max(0, original - finalResistance);
                        finalBlockedDamage = finalResistance;
                    }
                }
            }
            case Percent -> {
                if (!source.is(DamageTypeTags.IS_PROJECTILE) || !ConfigHandler.blockAllProjectileDamage){
                    if (100.0 > finalResistance) {
                        float damageReduction = finalResistance/100;
                        finalDamage = Math.max(0, original * (1-damageReduction));
                        finalBlockedDamage = Math.max(0,original-(finalDamage));
                    }
                }
            }
            case Cooldown -> {
                if (original > finalResistance && entity instanceof Player){
                    int cooldown = calculateShieldCooldown(original, finalResistance);
                    shieldCooldown = cooldown;
                    //((Player) entity).getCooldowns().addCooldown(shield.getItem(), cooldown);
                    //entity.stopUsingItem();
                    entity.level().broadcastEntityEvent(entity, (byte)30);
                }
            }
        }

        active = true;

        // 设置被挡下的伤害 = min(原伤害, 抗击值)
        event.setBlockedDamage(Math.min(original, finalBlockedDamage));

        /* 3. 耐久消耗（可选：只按被挡下的部分扣）
        event.setShieldDamage(Math.min(original, finalBlockedDamage));*/

        //4. 音效
        playerBlockSound(entity, finalDamage <= 0);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modify(Items.POTION, builder -> builder
                .set(DataComponents.MAX_STACK_SIZE, ConfigHandler.maxStackSizeNormal));
        event.modify(Items.SPLASH_POTION, builder -> builder
                .set(DataComponents.MAX_STACK_SIZE, ConfigHandler.maxStackSizeSplash));
        event.modify(Items.LINGERING_POTION, builder -> builder
                .set(DataComponents.MAX_STACK_SIZE, ConfigHandler.maxStackSizeLingering));
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        float speed = event.getOriginalSpeed();

        if (player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            int amplifier = Objects.requireNonNull(player.getEffect(MobEffects.DIG_SLOWDOWN)).getAmplifier();
            if (amplifier == 2) {
                speed /= 0.0027F;
                speed *= 0.027F;
            } else if (amplifier >= 3) {
                speed /= 8.1E-4F;
                float scale = (float) Math.pow(0.3, amplifier + 1);
                speed *= scale;
            }
        }
        event.setNewSpeed(speed);
    }
}
