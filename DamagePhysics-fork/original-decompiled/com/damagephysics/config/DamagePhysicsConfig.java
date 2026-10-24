package com.damagephysics.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public class DamagePhysicsConfig {
   public static final ModConfigSpec SPEC = DamagePhysicsConfig.BUILDER.build();
   public static final Builder BUILDER = new Builder();
   public static final DoubleValue GRAVITY = BUILDER.comment("Downward gravity acceleration applied per tick.").defineInRange("gravity", 0.15, 0.0, 2.0);
   public static final DoubleValue AIR_DRAG = BUILDER.comment("Air resistance multiplier applied each tick.").defineInRange("airDrag", 0.98, 0.5, 1.0);
   public static final DoubleValue SPAWN_IMPULSE_BASE = BUILDER.comment("Base upward velocity impulse on spawn.")
      .defineInRange("spawnImpulseBase", 3.0, 0.5, 15.0);
   public static final DoubleValue CRIT_IMPULSE_MULTIPLIER = BUILDER.comment("Impulse velocity multiplier applied to critical hits.")
      .defineInRange("critImpulseMultiplier", 1.5, 1.0, 4.0);
   public static final IntValue LIFETIME_TICKS = BUILDER.comment("Lifetime of a damage number in ticks (20 ticks = 1 second).")
      .defineInRange("lifetimeTicks", 40, 10, 200);
   public static final IntValue MAX_CONCURRENT_NUMBERS = BUILDER.comment("Maximum simultaneous damage numbers rendered on screen.")
      .defineInRange("maxConcurrentNumbers", 40, 5, 200);
   public static final BooleanValue ENABLE_COLLISIONS = BUILDER.comment("Whether damage numbers collide with each other to prevent unreadable stacking.")
      .define("enableCollisions", true);
   public static final ConfigValue<String> CRIT_COLOR = BUILDER.comment("Hex color for critical hits (format: #RRGGBB).").define("critColor", "#FFAA00");
   public static final ConfigValue<String> NORMAL_COLOR = BUILDER.comment("Hex color for normal damage hits (format: #RRGGBB).")
      .define("normalColor", "#FFFFFF");
   public static final ConfigValue<String> HEAL_COLOR = BUILDER.comment("Hex color for healing numbers (format: #RRGGBB).").define("healColor", "#55FF55");
   public static final DoubleValue MIN_FONT_SCALE = BUILDER.comment("Minimum text scale factor for small hits.").defineInRange("minFontScale", 0.8, 0.2, 3.0);
   public static final DoubleValue MAX_FONT_SCALE = BUILDER.comment("Maximum text scale factor for massive hits.").defineInRange("maxFontScale", 2.0, 0.5, 5.0);
   public static final DoubleValue SCALE_REFERENCE_DAMAGE = BUILDER.comment("Damage amount that scales up to maximum font size.")
      .defineInRange("scaleReferenceDamage", 20.0, 1.0, 1000.0);
   public static final BooleanValue ENABLE_IMPACT_SPARKS = BUILDER.comment("Whether to render 2D spark particles on critical hits.")
      .define("enableImpactSparks", true);
   public static final BooleanValue ENABLE_HEALING_NUMBERS = BUILDER.comment("Whether to detect and render healing numbers (health increases).")
      .define("enableHealingNumbers", true);

   static {
      BUILDER.comment("DamagePhysics Mod Configuration").push("physics");
      BUILDER.pop().push("visuals");
      BUILDER.pop();
   }
}
