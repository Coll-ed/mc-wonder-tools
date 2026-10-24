package com.hxzhitang.tongdaway.config;

import com.hxzhitang.tongdaway.common.XpRoundingMode;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class VitalXpConfig {
    public static final ModConfigSpec SERVER_SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<String> ROUNDING_MODE;
    public static final ModConfigSpec.BooleanValue PLAYER_KILL_ONLY;
    public static final ModConfigSpec.BooleanValue ALLOW_ZERO_BASE_XP_BONUS;
    public static final ModConfigSpec.IntValue ZERO_BASE_XP_VALUE;

    public static final ModConfigSpec.BooleanValue ENABLE_MULTIPLIER_CAP;
    public static final ModConfigSpec.DoubleValue MAX_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ENABLE_FINAL_XP_CAP;
    public static final ModConfigSpec.IntValue MAX_FINAL_XP;

    public static final ModConfigSpec.ConfigValue<String> FILTER_MODE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WHITELIST;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        ENABLED = builder.comment("Enable or disable the XP bonus system.")
                .define("enabled", true);
        HEALTH_MULTIPLIER = builder.comment("XP bonus per max health point. 0.02 means 2% per health point.")
                .defineInRange("healthMultiplier", 0.02D, 0.0D, 100.0D);
        ROUNDING_MODE = builder.comment("Rounding mode: FLOOR, ROUND, CEIL.")
                .define("roundingMode", "ROUND");
        PLAYER_KILL_ONLY = builder.comment("Only apply bonus when the entity was killed by a player.")
                .define("playerKillOnly", true);
        ALLOW_ZERO_BASE_XP_BONUS = builder.comment("If true, entities with 0 base XP can receive bonus based on zeroBaseXpValue.")
                .define("allowZeroBaseXpBonus", true);
        ZERO_BASE_XP_VALUE = builder.comment("Base XP used when allowZeroBaseXpBonus is true and original XP is 0.")
                .defineInRange("zeroBaseXpValue", 1, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("limits");
        ENABLE_MULTIPLIER_CAP = builder.comment("Enable maximum XP multiplier cap.")
                .define("enableMultiplierCap", true);
        MAX_MULTIPLIER = builder.comment("Maximum multiplier applied to base XP.")
                .defineInRange("maxMultiplier", 200.0D, 1.0D, 100000.0D);
        ENABLE_FINAL_XP_CAP = builder.comment("Enable maximum final XP cap.")
                .define("enableFinalXpCap", false);
        MAX_FINAL_XP = builder.comment("Maximum final XP after calculation.")
                .defineInRange("maxFinalXp", 10000, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("filters");
        FILTER_MODE = builder.comment("Filter mode: BLACKLIST or WHITELIST.")
                .define("filterMode", "BLACKLIST");
        BLACKLIST = builder.comment("Entity blacklist (e.g. minecraft:player).")
                .defineListAllowEmpty("blacklist", List.of("minecraft:player"), () -> "", obj -> obj instanceof String);
        WHITELIST = builder.comment("Entity whitelist.")
                .defineListAllowEmpty("whitelist", List.of(), () -> "", obj -> obj instanceof String);
        builder.pop();

        SERVER_SPEC = builder.build();
    }

    private VitalXpConfig() {}

    public static XpRoundingMode getRoundingMode() {
        try {
            return XpRoundingMode.valueOf(ROUNDING_MODE.get().toUpperCase());
        } catch (IllegalArgumentException e) {
            return XpRoundingMode.ROUND;
        }
    }
}
