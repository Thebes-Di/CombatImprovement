package shieldimprovements.event;

import net.minecraft.util.DamageSource;

public class DamageSourceStorage {
    public static final ThreadLocal<DamageSource> CURRENT_DAMAGE_SOURCE = new ThreadLocal<>();
}
