package com.thebes_di.combatimprovement.config;

import com.thebes_di.combatimprovement.CombatImprovement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.*;

@EventBusSubscriber(modid = CombatImprovement.MODID)
public class ConfigHandler {

    public static final ModConfigSpec SPEC;

    //盾牌机制相关配置定义
    private static final ModConfigSpec.ConfigValue<List<? extends String>> Entries;
    private static final ModConfigSpec.BooleanValue New_Shield_Mechanics;
    private static final ModConfigSpec.BooleanValue Only_Player;
    private static final ModConfigSpec.EnumValue<ResistanceMode> Resistance_Mode;
    private static final ModConfigSpec.IntValue Shield_Cooldown_Min;
    private static final ModConfigSpec.IntValue Shield_Cooldown_Max;
    private static final ModConfigSpec.DoubleValue Shield_Cooldown_Scaling;
    private static final ModConfigSpec.BooleanValue Block_All_Projectile_Damage;
    private static final ModConfigSpec.DoubleValue Projectile_Damage_Block_Factor;
    private static final ModConfigSpec.BooleanValue Axe_Disable_Shield;
    private static final ModConfigSpec.DoubleValue Break_Shield_Resistance_Factor;
    //近战相关配置定义
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TWO_HANDED_WEAPONS;
    private static final ModConfigSpec.DoubleValue TWO_HANDED_DAMAGE_PENALTY_1;
    private static final ModConfigSpec.DoubleValue TWO_HANDED_SPEED_PENALTY_1;
    private static final ModConfigSpec.DoubleValue TWO_HANDED_DAMAGE_PENALTY_2;
    private static final ModConfigSpec.DoubleValue TWO_HANDED_SPEED_PENALTY_2;

    private static final ModConfigSpec.BooleanValue OFFHAND_COMBAT_ENABLED;
    private static final ModConfigSpec.DoubleValue OFFHAND_DAMAGE_MULTIPLIER;
    //药水相关配置定义
    private static final ModConfigSpec.IntValue Max_Stack_Size_Normal;
    private static final ModConfigSpec.IntValue Max_Stack_Size_Splash;
    private static final ModConfigSpec.IntValue Max_Stack_Size_Lingering;
    private static final ModConfigSpec.IntValue Cooldown_Throwable_Potion;
    //药水效果相关配置定义
    private static final ModConfigSpec.EnumValue<StrengthMode> Strength_Mode;
    private static final ModConfigSpec.DoubleValue Strength_Per_Level;
    private static final ModConfigSpec.EnumValue<WeaknessMode> Weakness_Mode;
    private static final ModConfigSpec.BooleanValue Weakness_Exponential;
    private static final ModConfigSpec.DoubleValue Weakness_Per_Level;


    //盾牌机制相关静态字段
    public static final Map<String, ShieldConfigEntry> shieldConfigMap = new HashMap<>();
    public static boolean newShieldMechanics;
    public static boolean onlyPlayer;
    public static ResistanceMode resistanceMode;
    public static Integer shieldCooldownMin;
    public static Integer shieldCooldownMax;
    public static Double shieldCooldownScaling;
    public static boolean blockAllProjectileDamage;
    public static Double projectileDamageBlockFactor;
    public static boolean axeDisableShield;
    public static Double breakShieldResistanceFactor;
    //近战相关静态字段
    public static final Map<String, Integer> twoHandedWeapons = new HashMap<>();
    public static double twoHandedDamagePenalty1;
    public static double twoHandedAttackSpeedPenalty1;
    public static double twoHandedDamagePenalty2;
    public static double twoHandedAttackSpeedPenalty2;

    public static boolean offhandCombatEnabled;
    public static double offhandDamageMultiplier;
    //药水相关静态字段
    public static int maxStackSizeNormal;
    public static int maxStackSizeSplash;
    public static int maxStackSizeLingering;
    public static int cooldownThrowablePotion;
    //药水效果相关静态字段
    public static StrengthMode strengthMode;
    public static Double strengthPerLevel;
    public static WeaknessMode weaknessMode;
    public static boolean weaknessExponential;
    public static Double weaknessPerLevel;

    public enum ResistanceMode {
        Subtraction,
        Percent,
        Cooldown
    }

    public enum StrengthMode {
        Addition,
        Multiplied_Base,
        Multiplied_Total
    }
    public enum WeaknessMode {
        Addition,
        Multiplied_Base,
        Multiplied_Total
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Shield Mechanics Configuration").push("shieldMechanics");{
            Entries = builder
                    .comment("Format: itemId;resistance;angle;delay",
                            "Example: minecraft:shield;5.0;90.0;5")
                    .defineListAllowEmpty(
                            "shieldConfigs",
                            List.of("minecraft:shield;5.0;90.0;5"),
                            () -> "minecraft:shield;5.0;90.0;5",
                            obj -> obj instanceof String
                    );
            New_Shield_Mechanics = builder
                    .define("newShieldMechanics", true);
            Only_Player = builder
                    .comment("Only player use new shield mechanics")
                    .define("onlyPlayer", false);
            Resistance_Mode = builder
                    .comment("Subtraction: ",
                            "Percent: ",
                            "Cooldown: ")
                    .defineEnum("resistanceMode", ResistanceMode.Subtraction);
            Shield_Cooldown_Min = builder
                    .comment("")
                    .defineInRange("shieldCooldownMin", 10, 0, Short.MAX_VALUE);
            Shield_Cooldown_Max = builder
                    .comment("")
                    .defineInRange("shieldCooldownMax", 200, 0, Short.MAX_VALUE);
            Shield_Cooldown_Scaling = builder
                    .comment("")
                    .defineInRange("shieldCooldownScaling", 10.0F, 0, Short.MAX_VALUE);
            Block_All_Projectile_Damage = builder
                    .comment("A magic number")
                    .define("blockAllProjectileDamage", false);
            Projectile_Damage_Block_Factor = builder
                    .comment("A magic number")
                    .defineInRange("projectileDamageBlockFactor", 2.0, 0, Float.MAX_VALUE);
            Axe_Disable_Shield = builder
                    .comment("A magic number")
                    .define("AxeDisableShield", true);
            Break_Shield_Resistance_Factor = builder
                    .comment("A magic number")
                    .defineInRange("breakShieldResistanceFactor",0.5,0,1.0);
        }builder.pop();

        builder.comment("Melee Configuration").push("meleeMechanics");{
            TWO_HANDED_WEAPONS = builder
                    .comment("List of items considered as semi-two-handed or two-handed weapons.",
                            "Format: itemId;type",
                            "type 1 = semi-two-handed (lighter penalty)",
                            "type 2 = two-handed (heavier penalty)",
                            "Example: minecraft:iron_sword;1")
                    .defineListAllowEmpty(
                            "twoHandedWeapons",
                            List.of("minecraft:iron_sword;1", "minecraft:diamond_sword;2"), // 默认值
                            () -> "minecraft:iron_sword;1",
                            obj -> obj instanceof String
                    );
            TWO_HANDED_DAMAGE_PENALTY_1 = builder
                    .comment("Damage penalty for type 1 (semi-two-handed), 0.0~1.0")
                    .defineInRange("twoHandedDamagePenalty1", 0.15, 0.0, 1.0);
            TWO_HANDED_SPEED_PENALTY_1 = builder
                    .comment("Attack speed penalty for type 1, 0.0~1.0")
                    .defineInRange("twoHandedSpeedPenalty1", 0.1, 0.0, 1.0);
            TWO_HANDED_DAMAGE_PENALTY_2 = builder
                    .comment("Damage penalty for type 2 (two-handed), 0.0~1.0")
                    .defineInRange("twoHandedDamagePenalty2", 0.45, 0.0, 1.0);
            TWO_HANDED_SPEED_PENALTY_2 = builder
                    .comment("Attack speed penalty for type 2, 0.0~1.0")
                    .defineInRange("twoHandedSpeedPenalty2", 0.3, 0.0, 1.0);
            OFFHAND_COMBAT_ENABLED = builder
                    .comment("Enable offhand combat: right-click with an offhand weapon to attack")
                    .define("offhandCombatEnabled", true);

            OFFHAND_DAMAGE_MULTIPLIER = builder
                    .comment("Damage multiplier for offhand attacks (1.0 = full damage)")
                    .defineInRange("offhandDamageMultiplier", 0.7, 0.0, 1.0);
        }builder.pop();

        builder.comment("Potion Configuration").push("potionTweaks");{
            Max_Stack_Size_Normal = builder
                    .comment("A magic number")
                    .defineInRange("maxStackSizeNormalPotion", 16, 1, 64);
            Max_Stack_Size_Splash = builder
                    .comment("A magic number")
                    .defineInRange("maxStackSizeSplashPotion", 8, 1, 64);
            Max_Stack_Size_Lingering = builder
                    .comment("A magic number")
                    .defineInRange("maxStackSizeLingeringPotion", 8, 1, 64);
            Cooldown_Throwable_Potion = builder
                    .comment("A magic number")
                    .defineInRange("cooldownThrowablePotion", 20, 0, Short.MAX_VALUE);
            builder.comment("Effect Configuration").push("effectTweaks");{
                Strength_Mode = builder
                        .comment("Addition: ",
                                "Multiplied_Base: ",
                                "Multiplied_Total: ")
                        .defineEnum("strengthMode", StrengthMode.Multiplied_Base);
                Strength_Per_Level = builder
                        .comment("A magic number")
                        .defineInRange("strengthPerLevel", 0.3, 0, Double.MAX_VALUE);
                Weakness_Mode = builder
                        .comment("Addition: ",
                                "Multiplied_Base: ",
                                "Multiplied_Total: ")
                        .defineEnum("weaknessMode", WeaknessMode.Multiplied_Base);
                Weakness_Exponential = builder
                        .comment("A magic number")
                        .define("weaknessExponential", false);
                Weakness_Per_Level = builder
                        .comment("A magic number")
                        .defineInRange("weaknessPerLevel", 0.4, 0, Double.MAX_VALUE);
                builder.pop();
            }
        }builder.pop();
        SPEC = builder.build();
    }

    public static void reload() {
        newShieldMechanics = New_Shield_Mechanics.get();
        onlyPlayer = Only_Player.get();
        resistanceMode = Resistance_Mode.get();
        shieldCooldownMin = Shield_Cooldown_Min.get();
        shieldCooldownMax = Shield_Cooldown_Max.get();
        shieldCooldownScaling = Shield_Cooldown_Scaling.get();
        blockAllProjectileDamage = Block_All_Projectile_Damage.get();
        projectileDamageBlockFactor = Projectile_Damage_Block_Factor.get();
        axeDisableShield = Axe_Disable_Shield.get();
        breakShieldResistanceFactor = Break_Shield_Resistance_Factor.get();

        shieldConfigMap.clear();
        for (String str : Entries.get()) {
            ShieldConfigEntry entry = ShieldConfigEntry.fromString(str);
            if (entry != null) {
                shieldConfigMap.put(entry.itemId, entry);
            }
        }

        twoHandedWeapons.clear();
        for (String str : TWO_HANDED_WEAPONS.get()) {
            String[] parts = str.split(";");
            if (parts.length != 2) continue;
            try {
                String id = parts[0].trim();
                int type = Integer.parseInt(parts[1].trim());
                if (type == 1 || type == 2) {
                    twoHandedWeapons.put(id, type);
                }
            } catch (NumberFormatException ignored) {}
        }

        twoHandedDamagePenalty1 = TWO_HANDED_DAMAGE_PENALTY_1.get();
        twoHandedAttackSpeedPenalty1 = TWO_HANDED_SPEED_PENALTY_1.get();
        twoHandedDamagePenalty2 = TWO_HANDED_DAMAGE_PENALTY_2.get();
        twoHandedAttackSpeedPenalty2 = TWO_HANDED_SPEED_PENALTY_2.get();

        offhandCombatEnabled = OFFHAND_COMBAT_ENABLED.get();
        offhandDamageMultiplier = OFFHAND_DAMAGE_MULTIPLIER.get();

        maxStackSizeNormal = Max_Stack_Size_Normal.get();
        maxStackSizeSplash = Max_Stack_Size_Splash.get();
        maxStackSizeLingering = Max_Stack_Size_Lingering.get();
        cooldownThrowablePotion = Cooldown_Throwable_Potion.get();

        strengthMode = Strength_Mode.get();
        strengthPerLevel = Strength_Per_Level.get();
        weaknessMode = Weakness_Mode.get();
        weaknessExponential = Weakness_Exponential.get();
        weaknessPerLevel = Weakness_Per_Level.get();
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            reload();
        }
    }

}
