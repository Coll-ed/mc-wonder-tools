package com.damagephysics.fork;

/**
 * 改版共用的小工具（单一来源：颜色解析只在这里实现一次）。
 */
public final class ForkStyle {

   /** 6 位 {@code #RRGGBB} 的 alpha 前缀（完全不透明）。 */
   private static final int FULL_ALPHA = 0xFF000000;

   private ForkStyle() {
   }

   /**
    * 解析 {@code #RRGGBB} / {@code #AARRGGBB} 颜色；解析失败返回 {@code fallback}。
    * 配置写错时回退而不是崩溃——渲染路径上抛异常会直接崩客户端。
    */
   public static int parseHexOrDefault(String hex, int fallback) {
      if (hex == null) {
         return fallback;
      }
      String value = hex.startsWith("#") ? hex.substring(1) : hex;
      try {
         long parsed = Long.parseLong(value, 16);
         if (value.length() == 6) {
            return FULL_ALPHA | (int)parsed;
         }
         if (value.length() == 8) {
            return (int)parsed;
         }
      } catch (NumberFormatException ignored) {
         // 颜色配置写错：回退到调用方给的默认色
      }
      return fallback;
   }

   /** 把颜色按比例压向黑色（{@code amount}=0 保持原色，1 = 纯黑），保留 alpha。 */
   public static int darken(int argb, float amount) {
      float keep = 1.0F - Math.max(0.0F, Math.min(1.0F, amount));
      int a = argb >>> 24 & 0xFF;
      int r = (int)((float)(argb >>> 16 & 0xFF) * keep);
      int g = (int)((float)(argb >>> 8 & 0xFF) * keep);
      int b = (int)((float)(argb & 0xFF) * keep);
      return a << 24 | r << 16 | g << 8 | b;
   }

   /**
    * 按 DamageType id 从 {@code typeColorMap} 里找颜色（子串匹配，先长后短由配置顺序决定）。
    * 找不到返回 0，由调用方决定回退。
    */
   public static int colorForTypeId(String typeId) {
      if (typeId == null || typeId.isEmpty()) {
         return 0;
      }
      String id = typeId.toLowerCase(java.util.Locale.ROOT);
      var entries = (java.util.List<? extends String>)com.damagephysics.config.DamagePhysicsConfig.TYPE_COLOR_MAP.get();
      for (String entry : entries) {
         int eq = entry.indexOf('=');
         if (eq <= 0) {
            continue;
         }
         String needle = entry.substring(0, eq).trim().toLowerCase(java.util.Locale.ROOT);
         String hex = entry.substring(eq + 1).trim();
         if (!needle.isEmpty() && id.contains(needle)) {
            int parsed = parseHexOrDefault(hex, 0);
            if (parsed != 0) {
               return parsed;
            }
         }
      }
      return 0;
   }

   /** 减益统一回退色。 */
   public static String debuffHexFor(DamageCategory category) {
      return com.damagephysics.config.DamagePhysicsConfig.DEBUFF_FALLBACK_COLOR.get();
   }

   /** 按分类取**描边**颜色（与数字颜色分开，用户要求两套独立）。 */
   public static String outlineHexFor(DamageCategory category) {
      return switch (category) {
         case PHYSICAL -> com.damagephysics.config.DamagePhysicsConfig.OUTLINE_COLOR_PHYSICAL.get();
         case MAGIC -> com.damagephysics.config.DamagePhysicsConfig.OUTLINE_COLOR_MAGIC.get();
         case FIRE -> com.damagephysics.config.DamagePhysicsConfig.OUTLINE_COLOR_FIRE.get();
         case FROST -> com.damagephysics.config.DamagePhysicsConfig.OUTLINE_COLOR_FROST.get();
         case LIGHTNING -> com.damagephysics.config.DamagePhysicsConfig.OUTLINE_COLOR_LIGHTNING.get();
         case ENCHANTED -> com.damagephysics.config.DamagePhysicsConfig.OUTLINE_COLOR_ENCHANTED.get();
         case HEAL -> com.damagephysics.config.DamagePhysicsConfig.OUTLINE_COLOR_PHYSICAL.get();
      };
   }

   /** 按分类取颜色字符串（全部来自配置，代码里没有写死的色值）。 */
   public static String hexFor(DamageCategory category) {
      return switch (category) {
         case PHYSICAL -> com.damagephysics.config.DamagePhysicsConfig.COLOR_PHYSICAL.get();
         case MAGIC -> com.damagephysics.config.DamagePhysicsConfig.COLOR_MAGIC.get();
         case FIRE -> com.damagephysics.config.DamagePhysicsConfig.COLOR_FIRE.get();
         case FROST -> com.damagephysics.config.DamagePhysicsConfig.COLOR_FROST.get();
         case LIGHTNING -> com.damagephysics.config.DamagePhysicsConfig.COLOR_LIGHTNING.get();
         case ENCHANTED -> com.damagephysics.config.DamagePhysicsConfig.COLOR_ENCHANTED.get();
         case HEAL -> com.damagephysics.config.DamagePhysicsConfig.HEAL_COLOR.get();
      };
   }
}
