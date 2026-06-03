package shieldimprovements.mixin;


import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shieldimprovements.event.DamageSourceStorage;
import shieldimprovements.event.EntityDamageSourceIgnoreShield;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityLivingBase.class, priority = 1400)
public abstract class EntityLivingBaseMixin {

    @Inject(method = "canBlockDamageSource", at = @At("RETURN"), cancellable = true)
    private void canBlockDamageSourceMixin(DamageSource damageSourceIn, CallbackInfoReturnable<Boolean> cir) {
        if (damageSourceIn instanceof EntityDamageSourceIgnoreShield) {
            cir.setReturnValue(false);
        }
    }

    /*@Redirect(
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
    }*/

    @Inject(method = "attackEntityFrom", at = @At("HEAD"))
    private void onAttackEntityFrom(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        DamageSourceStorage.CURRENT_DAMAGE_SOURCE.set(source);
    }

    @Inject(method = "attackEntityFrom", at = @At("RETURN"))
    private void onAttackEntityFromEnd(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        DamageSourceStorage.CURRENT_DAMAGE_SOURCE.remove();
    }

    @Inject(
            method = "knockBack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void knockBackMixin(Entity attacker, float strength, double ratioX, double ratioZ, CallbackInfo ci) {
        DamageSource source = DamageSourceStorage.CURRENT_DAMAGE_SOURCE.get();
        if (source instanceof EntityDamageSourceIgnoreShield &&
                ((EntityDamageSourceIgnoreShield) source).isReducedDamage()) {
            // 阻止击退
            ci.cancel();
        }
    }
}
