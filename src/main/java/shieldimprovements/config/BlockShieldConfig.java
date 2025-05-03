package shieldimprovements.config;

import net.minecraft.item.ItemStack;

import static shieldimprovements.config.ConfigHandler.blockShieldConfigs;

public class BlockShieldConfig {
    public final String itemId;
    public final float maxBlockDamage;
    public final float blockAngle;
    public  int shieldRaiseTickDelay;

    public BlockShieldConfig(String itemId, float maxBlockDamage, float blockAngle, int RaiseTickDelay) {
        this.itemId = itemId;
        this.maxBlockDamage = maxBlockDamage;
        this.blockAngle = blockAngle;
        this.shieldRaiseTickDelay = RaiseTickDelay;
    }

    public static BlockShieldConfig fromString(String str) {
        String[] parts = str.split(";");
        if (parts.length != 4) return null;

        try {
            String id = parts[0].trim();
            float count = Float.parseFloat(parts[1].trim());
            float angle = Float.parseFloat(parts[2].trim());
            int TickDelay = Integer.parseInt(parts[3].trim());
            return new BlockShieldConfig(id, count, angle, TickDelay);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return itemId + ";" + maxBlockDamage + ";" + blockAngle + ";" + shieldRaiseTickDelay;
    }

    public static BlockShieldConfig getShieldConfigFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        String itemId = stack.getItem().getRegistryName().toString();

        for (BlockShieldConfig config : blockShieldConfigs) {
            if (config.itemId.equals(itemId)) {
                return config;
            }
        }

        return null;
    }

    public String getItemId() {
        return itemId;
    }

    public float getMaxBlockDamage() {
        return maxBlockDamage;
    }

    public float getBlockAngle() {
        return blockAngle;
    }

    public int getShieldRaiseTickDelay() {
        return shieldRaiseTickDelay;
    }
}
