package shieldimprovements.event;

import shieldimprovements.config.BlockShieldConfig;
import shieldimprovements.config.ConfigHandler;
import shieldimprovements.ShieldImprovements;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;

public class EventHandler {


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingAttack(LivingAttackEvent event) {
        // 跳过客户端、无效或非弹射物的自伤攻击跳过
        if (event.getEntityLiving().world.isRemote || event.isCanceled() || event.getAmount() <= 0.0F) return;
        if (event.getSource() instanceof EntityDamageSourceIgnoreShield) return;
        if (event.getEntityLiving() == event.getSource().getTrueSource() && !event.getSource().isProjectile()) return;

        EntityLivingBase defender = event.getEntityLiving();
        World world = defender.world;
        DamageSource source = event.getSource();
        float damage = event.getAmount();

        // 检查是否使用盾牌格挡
        ItemStack shield = defender.isHandActive() ? defender.getActiveItemStack() : ItemStack.EMPTY;
        if (!(shield.getItem() instanceof ItemShield)) return;

        // 检查是否为盾牌单独设置了格挡属性，若未设置则使用默认属性
        BlockShieldConfig config = BlockShieldConfig.getShieldConfigFor(shield);
        float blockAngle;
        float maxBlockDamage;
        int raiseTickDelay;
        if(config == null){
            blockAngle = 90F;
            maxBlockDamage = (float) ((shield.getMaxDamage() > 0) ? shield.getMaxDamage() : 500);
            raiseTickDelay = 5;
        }else{
            blockAngle = config.getBlockAngle();
            maxBlockDamage = config.getMaxBlockDamage();
            raiseTickDelay = config.getShieldRaiseTickDelay();
        }
        // 判断攻击是否来自于格挡角度内
        if (!canBlockDamageSource(defender, source, raiseTickDelay)) return;
        event.setCanceled(true);
        if (inCanBlockAngle(defender, source, blockAngle)){
            // 处理不同格挡模式
            switch (ConfigHandler.shieldModeEnum) {
                case BLOCK_ALL_DAMAGE:
                    fullBlockMode(defender, shield, source, damage, maxBlockDamage, world);
                    break;

                case FIXED_DAMAGE_REDUCTION:
                    fixedReductionMode(defender, shield, source, damage, maxBlockDamage, world);
                    break;
            }
        }else{
            DamageSource newSource = new EntityDamageSourceIgnoreShield(source.getDamageType(), source.getTrueSource());
            defender.attackEntityFrom(newSource, damage);
        }

    }


    private void fullBlockMode(EntityLivingBase defender, ItemStack shield, DamageSource source, float damage, float maxBlockDamage, World world) {
        if (!(defender instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) defender;

        if (damageShield(player, shield, damage)) return;

        // 检查是否被可破盾武器攻击
        if (tryShieldBypass(player, shield, source, world)) return;

        // 根据伤害设置冷却
        if (damage > maxBlockDamage && !source.isProjectile()) {
            int cooldown = (int) Math.max(ConfigHandler.cooldownTicksMinimum,
                    Math.min(ConfigHandler.cooldownTicksMaximum, (damage - maxBlockDamage) * ConfigHandler.cooldownTicksScaling));
            player.getCooldownTracker().setCooldown(shield.getItem(), cooldown);
            player.resetActiveHand();
            world.setEntityState(player, (byte) 30);
        } else {
            world.setEntityState(player, (byte) 29);
        }
        if (source.getTrueSource() instanceof EntityLivingBase){
            blockUsingShield((EntityLivingBase) source.getTrueSource(),player);
        }
    }

    private void fixedReductionMode(EntityLivingBase defender, ItemStack shield, DamageSource source, float damage, float maxBlockDamage, World world) {

        if (source.isProjectile()){ world.setEntityState(defender, (byte) 29); return;}

        if (ConfigHandler.onlyPlayer && !(defender instanceof EntityPlayer)) return;

        if (defender instanceof EntityPlayer) damageShield((EntityPlayer) defender,shield,damage);

        float reducedDamage = Math.max(damage - maxBlockDamage, 0.0F);

        if (source.getTrueSource() instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();
            ItemStack attackItem = attacker.getHeldItem(attacker.getActiveHand());

            if (!attackItem.isEmpty() && attackItem.getItem().canDisableShield(attackItem, shield, defender, attacker)) {
                reducedDamage = Math.max(damage - maxBlockDamage * ConfigHandler.breakShieldWeaponsDamageScaling, 0.0F);
                world.setEntityState(defender, (byte) 30);

                if (defender instanceof EntityPlayer && ConfigHandler.breakShieldWeaponsDisableShield) {
                    EntityPlayer player = (EntityPlayer) defender;
                    float chance = ConfigHandler.shieldBypassChance +
                            EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, attackItem) * 0.05F;
                    if (world.rand.nextFloat() < chance) {
                        player.getCooldownTracker().setCooldown(shield.getItem(), ConfigHandler.shieldBypassCooldown);
                        player.resetActiveHand();
                        return;
                    }
                }
            }else{
                world.setEntityState(defender, (byte) 29);
            }
            blockUsingShield(attacker,defender);
        }
        DamageSource newSource = new EntityDamageSourceIgnoreShield(source.getDamageType(), source.getTrueSource()).setIsReducedDamage();
        defender.attackEntityFrom(newSource, reducedDamage);
    }

    private boolean tryShieldBypass(EntityPlayer player, ItemStack shield, DamageSource source, World world) {
        if (!(source.getTrueSource() instanceof EntityLivingBase)) return false;
        EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();

        ItemStack attackItem = attacker.getHeldItem(attacker.getActiveHand());
        if (attackItem.isEmpty() || !attackItem.getItem().canDisableShield(attackItem, shield, player, attacker)) return false;

        float chance = ConfigHandler.shieldBypassChance +
                EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, attackItem) * 0.05F;
        if (world.rand.nextFloat() < chance) {
            player.getCooldownTracker().setCooldown(shield.getItem(), ConfigHandler.shieldBypassCooldown);
            player.resetActiveHand();
            world.setEntityState(player, (byte) 30);
            return true;
        }

        return false;
    }

    private boolean canBlockDamageSource(EntityLivingBase defender, DamageSource damagesource, int raiseTickDelay)
    {
        if(!damagesource.isUnblockable() && isActiveItemStackBlocking(defender, raiseTickDelay)) {
            Vec3d vec3d = damagesource.getDamageLocation();
            if(vec3d != null) {
                Vec3d vec3d1 = defender.getLook(1.0F);
                Vec3d vec3d2 = vec3d.subtractReverse(new Vec3d(defender.posX, defender.posY, defender.posZ)).normalize();
                vec3d2 = new Vec3d(vec3d2.x, 0.0D, vec3d2.z);

                if(vec3d2.dotProduct(vec3d1) < 0.0D) return true;
            }
        }
        return false;
    }

    private boolean inCanBlockAngle(EntityLivingBase defender, DamageSource damagesource, float blockAngleDegrees)
    {
        Vec3d vec3d = damagesource.getDamageLocation();
        if(vec3d != null) {
            Vec3d defenderLook = defender.getLook(1.0F).normalize();
            Vec3d attackDir = vec3d.subtract(new Vec3d(defender.posX, defender.posY, defender.posZ)).normalize();
            attackDir = new Vec3d(attackDir.x, 0.0D, attackDir.z).normalize();
            defenderLook = new Vec3d(defenderLook.x, 0.0D, defenderLook.z).normalize();

            // 计算夹角（单位：度）
            double dot = attackDir.dotProduct(defenderLook); // cos(theta)
            double angleDegrees = Math.toDegrees(Math.acos(dot));


            // 判断是否在允许的格挡角度范围内
            if(angleDegrees <= blockAngleDegrees / 2.0D) return true;
        }

        return false;
    }


    private boolean isActiveItemStackBlocking(EntityLivingBase defender, int raiseTickDelay)
    {
        if(defender.isHandActive() && !defender.getActiveItemStack().isEmpty()){
            Item item = defender.getActiveItemStack().getItem();

            if(item.getItemUseAction(defender.getActiveItemStack()) != EnumAction.BLOCK) return false;

            else return defender.getItemInUseMaxCount() >= raiseTickDelay;

        }
        else return false;

    }

    private boolean damageShield(EntityPlayer player, ItemStack shield, float damage)
    {
        ItemStack copy = shield.copy();
        int i = Math.max(1, (int)damage);
        shield.damageItem(i, player);

        if(player.getActiveItemStack().isEmpty()) {
            EnumHand hand = player.getActiveHand();
            ForgeEventFactory.onPlayerDestroyItem(player, copy, hand);

            player.setItemStackToSlot(hand.equals(EnumHand.MAIN_HAND) ? EntityEquipmentSlot.MAINHAND : EntityEquipmentSlot.OFFHAND, ItemStack.EMPTY);
            player.resetActiveHand();
            player.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + player.world.rand.nextFloat() * 0.4F);

            return true;
        }
        return false;
    }

    private void blockUsingShield(EntityLivingBase attacker, EntityLivingBase defender)
    {
        attacker.knockBack(defender, 0.5F, defender.posX - attacker.posX, defender.posZ - attacker.posZ);
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    @SideOnly(Side.CLIENT)
    public void onItemTooltip(ItemTooltipEvent event) {
        if(event.isCanceled()) return;

        ItemStack shield = event.getItemStack();
        BlockShieldConfig config = BlockShieldConfig.getShieldConfigFor(shield);
        if(config == null) return;

        float shieldBlockAngle = config.getBlockAngle();
        float shieldMaxBlockDamage = config.getMaxBlockDamage();
        int shieldRaiseTickDelay = config.getShieldRaiseTickDelay();
        double shieldResistance = ((double)((int)(shieldMaxBlockDamage*100)))/100D;
        int shieldSize = (int)shieldBlockAngle;

        event.getToolTip().add(TextFormatting.GREEN + I18n.format("tooltip.shieldimprovements.shieldresistance") + ":" + shieldResistance + TextFormatting.RESET);
        event.getToolTip().add(TextFormatting.GREEN + I18n.format("tooltip.shieldimprovements.shieldsize") + ":" + shieldSize + TextFormatting.RESET);
        event.getToolTip().add(TextFormatting.GREEN + I18n.format("tooltip.shieldimprovements.shieldRaiseDelay") + ":" + shieldRaiseTickDelay + TextFormatting.RESET);
    }
}
