package shieldimprovements.mixin;


import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.injection.Redirect;
import shieldimprovements.event.EntityDamageSourceIgnoreShield;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseMixin {

    @Inject(method = "canBlockDamageSource", at = @At("RETURN"), cancellable = true)
    private void canBlockDamageSourceMixin(DamageSource damageSourceIn, CallbackInfoReturnable<Boolean> cir) {
        if (damageSourceIn instanceof EntityDamageSourceIgnoreShield) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = "attackEntityFrom",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;knockBack(Lnet/minecraft/entity/Entity;FDD)V"
            )
    )
    private void redirectKnockback(EntityLivingBase instance, Entity attacker, float strength, double ratioX, double ratioZ, DamageSource source, float amount) {
        if (source instanceof EntityDamageSourceIgnoreShield &&
                ((EntityDamageSourceIgnoreShield) source).isReducedDamage()) {
            // 不执行击退
            return;
        }

        // 正常击退
        instance.knockBack(attacker, strength, ratioX, ratioZ);
    }
}
