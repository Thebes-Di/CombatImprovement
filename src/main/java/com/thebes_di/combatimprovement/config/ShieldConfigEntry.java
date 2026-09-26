package com.thebes_di.combatimprovement.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public class ShieldConfigEntry {
    public final String itemId;
    public final float resistance;
    public final float angle;
    public final int delay;

    public ShieldConfigEntry(String itemId, float resistance, float angle, int delay) {
        this.itemId = itemId;
        this.resistance = Math.max(0, resistance);
        this.angle = Math.max(0, angle);
        this.delay = Math.max(0, delay);
    }

    // 格式: "itemId;resistance;angle;delay"
    public static ShieldConfigEntry fromString(String str) {
        String[] parts = str.split(";");
        if (parts.length != 4) return null;
        try {
            return new ShieldConfigEntry(
                    parts[0].trim(),
                    Float.parseFloat(parts[1].trim()),
                    Float.parseFloat(parts[2].trim()),
                    Integer.parseInt(parts[3].trim())
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return itemId + ";" + resistance + ";" + angle + ";" + delay;
    }

    public static ShieldConfigEntry getShieldConfigFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return ConfigHandler.shieldConfigMap.getOrDefault(id, null);
    }

    // getter 方法保持不变
    public String getItemId() { return itemId; }
    public float getResistance() { return resistance; }
    public float getAngle() { return angle; }
    public int getDelay() { return delay; }
}
