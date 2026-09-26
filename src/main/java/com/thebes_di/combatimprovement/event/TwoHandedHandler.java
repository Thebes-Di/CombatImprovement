package com.thebes_di.combatimprovement.event;

import com.thebes_di.combatimprovement.CombatImprovement;
import com.thebes_di.combatimprovement.config.ConfigHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CombatImprovement.MODID)
public class TwoHandedHandler {

    /*@SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Entity attackerEntity = event.getSource().getEntity();

        // 确保攻击者是LivingEntity
        if (!(attackerEntity instanceof LivingEntity attacker)) {
            return;
        }

        // 应用双手武器惩罚
        applyTwoHandedPenalty(attacker, event);
    }

    private static void applyTwoHandedPenalty(LivingEntity attacker, LivingIncomingDamageEvent event) {
        if (shouldPenalize(attacker)) {
            // 动态修改伤害
            float originalDamage = event.getAmount();
            float penaltyMultiplier = 0.5f; // 示例：降低20%伤害
            event.setAmount(originalDamage * penaltyMultiplier);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            managePlayerAttackSpeedPenalty(player);
        }
    }*/

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        EquipmentSlot slot = event.getSlot();
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) return;

        updatePenalty(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            updatePenalty(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player newPlayer = event.getEntity();
        if (!newPlayer.level().isClientSide()) {
            updatePenalty(newPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        int type = getWeaponType(player);  // 0 = 无，1 = 单双手，2 = 双手
        if (type == 0 || player.getOffhandItem().isEmpty()) {
            // 不满足条件，移除惩罚
            removeModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_PENALTY_ID);
            removeModifier(player, Attributes.ATTACK_SPEED, SPEED_PENALTY_ID);
            return;
        }

        double damagePenalty = (type == 1)
                ? ConfigHandler.twoHandedDamagePenalty1
                : ConfigHandler.twoHandedDamagePenalty2;
        double speedPenalty = (type == 1)
                ? ConfigHandler.twoHandedAttackSpeedPenalty1
                : ConfigHandler.twoHandedAttackSpeedPenalty2;

        setModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_PENALTY_ID, -damagePenalty);
        setModifier(player, Attributes.ATTACK_SPEED, SPEED_PENALTY_ID, -speedPenalty);
    }

    private static void updatePenalty(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // 判断武器类型：0 = 不是，1 = 单双手，2 = 双手
        int type = getWeaponType(player);

        // 只有副手有物品时才触发惩罚
        boolean shouldPenalize = type != 0 && !offHand.isEmpty();

        if (!shouldPenalize) {
            removeModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_PENALTY_ID);
            removeModifier(player, Attributes.ATTACK_SPEED, SPEED_PENALTY_ID);
            return;
        }

        // 按类型选惩罚系数
        double damagePenalty = (type == 1)
                ? ConfigHandler.twoHandedDamagePenalty1
                : ConfigHandler.twoHandedDamagePenalty2;
        double speedPenalty = (type == 1)
                ? ConfigHandler.twoHandedAttackSpeedPenalty1
                : ConfigHandler.twoHandedAttackSpeedPenalty2;

        setModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_PENALTY_ID, -damagePenalty);
        setModifier(player, Attributes.ATTACK_SPEED, SPEED_PENALTY_ID, -speedPenalty);
    }

    private static int getWeaponType(LivingEntity living) {
        String id = BuiltInRegistries.ITEM.getKey(living.getMainHandItem().getItem()).toString();
        return ConfigHandler.twoHandedWeapons.getOrDefault(id, 0);
    }

    private static boolean shouldPenalize(LivingEntity Entity) {
        ItemStack mainHand = Entity.getMainHandItem();
        ItemStack offHand = Entity.getOffhandItem();
        String mainHandId = BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString();
        return ConfigHandler.twoHandedWeapons.containsKey(mainHandId) && !offHand.isEmpty();
    }

    /*private static void managePlayerAttackSpeedPenalty(Player player) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) return;

        boolean shouldPenalize = shouldPenalize(player);
        boolean hasModifier = attackSpeed.hasModifier(SPEED_PENALTY_ID);

        if (shouldPenalize && !hasModifier) {
            AttributeModifier speedMod = new AttributeModifier(
                    SPEED_PENALTY_ID,
                    -0.2,  // 从配置读
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            attackSpeed.addTransientModifier(speedMod);
        } else if (!shouldPenalize && hasModifier) {
            attackSpeed.removeModifier(SPEED_PENALTY_ID);
        }
    }*/

    private static void setModifier(Player player, Holder<Attribute> attr, ResourceLocation id, double value) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - value) < 1e-6) return;
        if (existing != null) inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(
                id, value, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeModifier(Player player, Holder<Attribute> attr, ResourceLocation id) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null && inst.hasModifier(id)) inst.removeModifier(id);
    }

    public static final ResourceLocation DAMAGE_PENALTY_ID =
            ResourceLocation.fromNamespaceAndPath(CombatImprovement.MODID, "two_handed_damage_penalty");
    public static final ResourceLocation SPEED_PENALTY_ID =
            ResourceLocation.fromNamespaceAndPath(CombatImprovement.MODID, "two_handed_speed_penalty");
}
