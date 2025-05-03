package shieldimprovements.config;

import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Mod;
import shieldimprovements.ShieldImprovements;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber
public class ConfigHandler {
    public static Configuration config;

    public static void init(Configuration configuration) {
        if (config == null) {
            config = configuration;
            loadConfig();
            for (String category : categories) {
                setCategoryLanguageKey(category);
            }
        }
    }

    public static void setCategoryLanguageKey(String category) {
        String langKey = ShieldImprovements.MOD_ID + ".config." + category;
        config.setCategoryLanguageKey(category, langKey);
    }

    @SubscribeEvent
    public static void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if(event.getModID().equals(ShieldImprovements.MOD_ID))
        {
            loadConfig();
        }
    }

    public enum ShieldMode {
        BLOCK_ALL_DAMAGE,
        FIXED_DAMAGE_REDUCTION;

        public static ShieldMode fromString(String name) {
            try {
                return ShieldMode.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return BLOCK_ALL_DAMAGE; // 默认值，可按需修改
            }
        }
    }

    public static final String CAT_COMBAT = "combat";
    public static final String CAT_FullBlockMode = "fullBlockMode";
    public static final String CAT_FixedReductionMode = "fixedReductionMode";

    public static final String[] categories = new String[] {
            CAT_COMBAT,
            CAT_FullBlockMode,
            CAT_FixedReductionMode
    };

    public static List<BlockShieldConfig> blockShieldConfigs = new ArrayList<>();
    public static ShieldMode shieldModeEnum;

    public static float shieldBypassChance;
    public static int shieldBypassCooldown;
    public static boolean parryKnockBackAttacker;

    public static int cooldownTicksMinimum;
    public static int cooldownTicksMaximum;
    public static float cooldownTicksScaling;

    public static boolean breakShieldWeaponsDisableShield;
    public static boolean onlyPlayer;
    public static float breakShieldWeaponsDamageScaling;

    public static void loadConfig()
    {
        shieldBypassChance = config.getFloat("shieldBypassChance",CAT_COMBAT,0.25F,0,Float.MAX_VALUE,"");
        shieldBypassCooldown = config.getInt("shieldBypassCooldown",CAT_COMBAT,15,0,Short.MAX_VALUE,"");
        parryKnockBackAttacker = config.getBoolean("parryKnockBackAttacker",CAT_COMBAT,true,"");
        cooldownTicksMinimum = config.getInt("cooldownTicksMinimum",CAT_FullBlockMode,10,0,Short.MAX_VALUE,"");
        cooldownTicksMaximum = config.getInt("cooldownTicksMaximum",CAT_FullBlockMode,200,0,Short.MAX_VALUE,"");
        cooldownTicksScaling = config.getFloat("cooldownTicksScaling",CAT_FullBlockMode,10.0F,0,Float.MAX_VALUE,"");
        breakShieldWeaponsDisableShield = config.getBoolean("breakShieldWeaponsDisableShield",CAT_FixedReductionMode,true,"");
        onlyPlayer = config.getBoolean("onlyPlayer",CAT_FixedReductionMode,false,"");
        breakShieldWeaponsDamageScaling = config.getFloat("breakShieldWeaponsDamageScaling",CAT_FixedReductionMode,0.5F,0,1.0F,"");
        Property mode = config.get(CAT_COMBAT,"ShieldMode", "BLOCK_ALL_DAMAGE", "盾牌格挡逻辑模式,BLOCK_ALL_DAMAGE,FIXED_DAMAGE_REDUCTION");
        String[] validModes = Arrays.stream(ShieldMode.values())
                .map(Enum::name)
                .toArray(String[]::new);
        mode.setValidValues(validModes);
        shieldModeEnum = ShieldMode.fromString(mode.getString());

        String[] defaultList = new String[] {
                "minecraft:shield;5;90;5"
        };
        String[] list = config.getStringList(
                "blockShieldConfigs",
                CAT_COMBAT,
                defaultList,
                "List of shield-like items with block properties in the format: item_id;max_block_count;block_angle,raise_tick_delay"
        );

        blockShieldConfigs.clear(); // 清空旧数据
        for (String entry : list) {
            BlockShieldConfig parsed = BlockShieldConfig.fromString(entry);
            if (parsed != null) {
                blockShieldConfigs.add(parsed);
            }
        }

        if (config.hasChanged()) {
            config.save();
        }
    }
}
