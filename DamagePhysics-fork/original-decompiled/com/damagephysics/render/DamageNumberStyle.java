package com.damagephysics.render;

import com.damagephysics.config.DamagePhysicsConfig;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class DamageNumberStyle {
   private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.ROOT));

   public static int parseHexColor(String hex, int defaultColor) {
      if (hex != null && !hex.isEmpty()) {
         try {
            String clean = hex.trim();
            if (clean.startsWith("#")) {
               clean = clean.substring(1);
            } else if (clean.startsWith("0x") || clean.startsWith("0X")) {
               clean = clean.substring(2);
            }

            if (clean.length() == 6) {
               return 0xFF000000 | Integer.parseInt(clean, 16);
            }

            if (clean.length() == 8) {
               return (int)Long.parseLong(clean, 16);
            }
         } catch (Exception var3) {
         }

         return defaultColor;
      } else {
         return defaultColor;
      }
   }

   public static int getBaseColor(boolean isCrit, boolean isHeal) {
      if (isHeal) {
         return parseHexColor((String)DamagePhysicsConfig.HEAL_COLOR.get(), -11141291);
      } else {
         return isCrit
            ? parseHexColor((String)DamagePhysicsConfig.CRIT_COLOR.get(), -22016)
            : parseHexColor((String)DamagePhysicsConfig.NORMAL_COLOR.get(), -1);
      }
   }

   public static int applyAlpha(int argb, float alpha) {
      int a = Math.max(0, Math.min(255, (int)(alpha * 255.0F)));
      return a << 24 | argb & 16777215;
   }

   public static float computeFontScale(float damage, boolean isCrit) {
      float min = ((Double)DamagePhysicsConfig.MIN_FONT_SCALE.get()).floatValue();
      float max = ((Double)DamagePhysicsConfig.MAX_FONT_SCALE.get()).floatValue();
      float ref = ((Double)DamagePhysicsConfig.SCALE_REFERENCE_DAMAGE.get()).floatValue();
      float t = Math.max(0.0F, Math.min(1.0F, damage / Math.max(1.0F, ref)));
      float scale = min + (max - min) * (float)Math.sqrt((double)t);
      if (isCrit) {
         scale *= 1.2F;
      }

      return Math.max(0.2F, scale);
   }

   public static String formatDamageText(float damage, boolean isHeal) {
      String prefix = isHeal ? "+" : "";
      if (damage >= 1000000.0F) {
         return prefix + ONE_DECIMAL.format((double)(damage / 1000000.0F)) + "M";
      } else if (damage >= 1000.0F) {
         return prefix + ONE_DECIMAL.format((double)(damage / 1000.0F)) + "k";
      } else {
         return Math.abs(damage - (float)Math.round(damage)) < 0.05F ? prefix + Math.round(damage) : prefix + ONE_DECIMAL.format((double)damage);
      }
   }
}
