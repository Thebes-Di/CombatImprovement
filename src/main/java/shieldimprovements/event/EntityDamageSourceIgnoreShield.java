package shieldimprovements.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;

import javax.annotation.Nullable;

public class EntityDamageSourceIgnoreShield extends DamageSource {
    @Nullable
    private final Entity damageSourceEntity;

    private boolean isReducedDamage;

    public EntityDamageSourceIgnoreShield(String damageTypeIn, @Nullable Entity damageSourceEntityIn) {
        super(damageTypeIn);
        this.damageSourceEntity = damageSourceEntityIn;
    }

    @Nullable
    public Entity getTrueSource()
    {
        return this.damageSourceEntity;
    }

    public ITextComponent getDeathMessage(EntityLivingBase entityLivingBaseIn)
    {
        ItemStack itemstack = this.damageSourceEntity instanceof EntityLivingBase ? ((EntityLivingBase)this.damageSourceEntity).getHeldItemMainhand() : ItemStack.EMPTY;

        String baseDamageType = this.damageType.endsWith("IgnoreShield")
                ? this.damageType.substring(0, this.damageType.length() - "IgnoreShield".length())
                : this.damageType;

        String s = "death.attack." + baseDamageType;
        String s1 = s + ".item";
        return !itemstack.isEmpty() && itemstack.hasDisplayName() && I18n.canTranslate(s1) ? new TextComponentTranslation(s1, new Object[] {entityLivingBaseIn.getDisplayName(), this.damageSourceEntity.getDisplayName(), itemstack.getTextComponent()}) : new TextComponentTranslation(s, new Object[] {entityLivingBaseIn.getDisplayName(), this.damageSourceEntity.getDisplayName()});
    }

    @Nullable
    public Vec3d getDamageLocation()
    {
        return new Vec3d(this.damageSourceEntity.posX, this.damageSourceEntity.posY, this.damageSourceEntity.posZ);
    }

    public boolean isReducedDamage()
    {
        return this.isReducedDamage;
    }

    public DamageSource setIsReducedDamage()
    {
        this.isReducedDamage = true;
        return this;
    }
}
