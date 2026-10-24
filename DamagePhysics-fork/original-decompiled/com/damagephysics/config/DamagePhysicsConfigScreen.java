package com.damagephysics.config;

import com.damagephysics.render.DamageNumberStyle;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DamagePhysicsConfigScreen extends Screen {
   private final Screen parentScreen;
   private DamagePhysicsConfigScreen.ColorTarget activeColor = DamagePhysicsConfigScreen.ColorTarget.CRIT;
   private int critR;
   private int critG;
   private int critB;
   private int normR;
   private int normG;
   private int normB;
   private int healR;
   private int healG;
   private int healB;
   private double gravityVal;
   private double airDragVal;
   private double spawnImpulseVal;
   private double critMultiplierVal;
   private int lifetimeVal;
   private int maxNumbersVal;
   private boolean collisionsVal;
   private boolean sparksVal;
   private boolean healsVal;
   private double minScaleVal;
   private double maxScaleVal;
   private double refDamageVal;
   private DamagePhysicsConfigScreen.RGBSlider sliderR;
   private DamagePhysicsConfigScreen.RGBSlider sliderG;
   private DamagePhysicsConfigScreen.RGBSlider sliderB;

   public DamagePhysicsConfigScreen(Screen parentScreen) {
      super(Component.translatable("damagephysics.configuration.title"));
      this.parentScreen = parentScreen;
      this.loadValues();
   }

   private void loadValues() {
      this.gravityVal = (Double)DamagePhysicsConfig.GRAVITY.get();
      this.airDragVal = (Double)DamagePhysicsConfig.AIR_DRAG.get();
      this.spawnImpulseVal = (Double)DamagePhysicsConfig.SPAWN_IMPULSE_BASE.get();
      this.critMultiplierVal = (Double)DamagePhysicsConfig.CRIT_IMPULSE_MULTIPLIER.get();
      this.lifetimeVal = (Integer)DamagePhysicsConfig.LIFETIME_TICKS.get();
      this.maxNumbersVal = (Integer)DamagePhysicsConfig.MAX_CONCURRENT_NUMBERS.get();
      this.collisionsVal = (Boolean)DamagePhysicsConfig.ENABLE_COLLISIONS.get();
      this.sparksVal = (Boolean)DamagePhysicsConfig.ENABLE_IMPACT_SPARKS.get();
      this.healsVal = (Boolean)DamagePhysicsConfig.ENABLE_HEALING_NUMBERS.get();
      this.minScaleVal = (Double)DamagePhysicsConfig.MIN_FONT_SCALE.get();
      this.maxScaleVal = (Double)DamagePhysicsConfig.MAX_FONT_SCALE.get();
      this.refDamageVal = (Double)DamagePhysicsConfig.SCALE_REFERENCE_DAMAGE.get();
      int c = DamageNumberStyle.parseHexColor((String)DamagePhysicsConfig.CRIT_COLOR.get(), -22016);
      this.critR = c >> 16 & 0xFF;
      this.critG = c >> 8 & 0xFF;
      this.critB = c & 0xFF;
      int n = DamageNumberStyle.parseHexColor((String)DamagePhysicsConfig.NORMAL_COLOR.get(), -1);
      this.normR = n >> 16 & 0xFF;
      this.normG = n >> 8 & 0xFF;
      this.normB = n & 0xFF;
      int h = DamageNumberStyle.parseHexColor((String)DamagePhysicsConfig.HEAL_COLOR.get(), -11141291);
      this.healR = h >> 16 & 0xFF;
      this.healG = h >> 8 & 0xFF;
      this.healB = h & 0xFF;
   }

   protected void init() {
      super.init();
      int cx = this.width / 2;
      int leftX = cx - 155;
      int rightX = cx + 5;
      int sliderW = 150;
      int sliderH = 20;
      int y = 30;
      int gap = 24;
      this.addRenderableWidget(
         new DamagePhysicsConfigScreen.ConfigSlider(leftX, y, sliderW, sliderH, "Gravity", this.gravityVal, 0.0, 1.0, v -> this.gravityVal = v)
      );
      this.addRenderableWidget(
         new DamagePhysicsConfigScreen.ConfigSlider(rightX, y, sliderW, sliderH, "Air Drag", this.airDragVal, 0.8, 1.0, v -> this.airDragVal = v)
      );
      y += gap;
      this.addRenderableWidget(
         new DamagePhysicsConfigScreen.ConfigSlider(leftX, y, sliderW, sliderH, "Impulse", this.spawnImpulseVal, 1.0, 10.0, v -> this.spawnImpulseVal = v)
      );
      this.addRenderableWidget(
         new DamagePhysicsConfigScreen.ConfigSlider(rightX, y, sliderW, sliderH, "Crit Mult", this.critMultiplierVal, 1.0, 3.0, v -> this.critMultiplierVal = v)
      );
      y += gap;
      this.addRenderableWidget(
         new DamagePhysicsConfigScreen.ConfigSlider(
            leftX, y, sliderW, sliderH, "Lifetime", (double)this.lifetimeVal, 15.0, 100.0, v -> this.lifetimeVal = (int)Math.round(v)
         )
      );
      this.addRenderableWidget(
         new DamagePhysicsConfigScreen.ConfigSlider(
            rightX, y, sliderW, sliderH, "Max Numbers", (double)this.maxNumbersVal, 10.0, 100.0, v -> this.maxNumbersVal = (int)Math.round(v)
         )
      );
      y += gap;
      this.addRenderableWidget(
         new DamagePhysicsConfigScreen.ConfigSlider(leftX, y, sliderW, sliderH, "Min Scale", this.minScaleVal, 0.4, 1.5, v -> this.minScaleVal = v)
      );
      this.addRenderableWidget(
         new DamagePhysicsConfigScreen.ConfigSlider(rightX, y, sliderW, sliderH, "Max Scale", this.maxScaleVal, 1.0, 3.5, v -> this.maxScaleVal = v)
      );
      y += gap;
      this.addRenderableWidget(
         new DamagePhysicsConfigScreen.ConfigSlider(leftX, y, sliderW, sliderH, "Ref Damage", this.refDamageVal, 5.0, 100.0, v -> this.refDamageVal = v)
      );
      this.addRenderableWidget(
         Checkbox.builder(Component.literal("Collisions"), this.font)
            .pos(rightX, y + 1)
            .selected(this.collisionsVal)
            .onValueChange((cb, v) -> this.collisionsVal = v)
            .build()
      );
      y += gap;
      this.addRenderableWidget(
         Checkbox.builder(Component.literal("Impact Sparks"), this.font)
            .pos(leftX, y + 1)
            .selected(this.sparksVal)
            .onValueChange((cb, v) -> this.sparksVal = v)
            .build()
      );
      this.addRenderableWidget(
         Checkbox.builder(Component.literal("Healing Numbers"), this.font)
            .pos(rightX, y + 1)
            .selected(this.healsVal)
            .onValueChange((cb, v) -> this.healsVal = v)
            .build()
      );
      y += gap + 8;
      int tabW = 80;
      this.addRenderableWidget(
         Button.builder(Component.literal("§6Crit Color"), btn -> this.switchColor(DamagePhysicsConfigScreen.ColorTarget.CRIT))
            .bounds(leftX, y, tabW, sliderH)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Normal Color"), btn -> this.switchColor(DamagePhysicsConfigScreen.ColorTarget.NORMAL))
            .bounds(leftX + tabW + 5, y, tabW + 10, sliderH)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("§aHeal Color"), btn -> this.switchColor(DamagePhysicsConfigScreen.ColorTarget.HEAL))
            .bounds(leftX + 2 * tabW + 20, y, tabW, sliderH)
            .build()
      );
      int colorY = y + gap;
      int rgbW = 100;
      this.sliderR = new DamagePhysicsConfigScreen.RGBSlider(leftX, colorY, rgbW, sliderH, "R", this.getActiveR(), v -> this.setActiveR(v));
      this.sliderG = new DamagePhysicsConfigScreen.RGBSlider(leftX + rgbW + 5, colorY, rgbW, sliderH, "G", this.getActiveG(), v -> this.setActiveG(v));
      this.sliderB = new DamagePhysicsConfigScreen.RGBSlider(leftX + 2 * (rgbW + 5), colorY, rgbW, sliderH, "B", this.getActiveB(), v -> this.setActiveB(v));
      this.addRenderableWidget(this.sliderR);
      this.addRenderableWidget(this.sliderG);
      this.addRenderableWidget(this.sliderB);
      int bottomY = this.height - 26;
      this.addRenderableWidget(Button.builder(Component.literal("Reset Defaults"), btn -> {
         this.resetDefaults();
         this.repositionElements();
      }).bounds(cx - 160, bottomY, 100, sliderH).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> this.saveAndClose()).bounds(cx - 50, bottomY, 100, sliderH).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose()).bounds(cx + 60, bottomY, 100, sliderH).build());
   }

   private void switchColor(DamagePhysicsConfigScreen.ColorTarget target) {
      this.activeColor = target;
      if (this.sliderR != null) {
         this.sliderR.setVal(this.getActiveR());
      }

      if (this.sliderG != null) {
         this.sliderG.setVal(this.getActiveG());
      }

      if (this.sliderB != null) {
         this.sliderB.setVal(this.getActiveB());
      }
   }

   private int getActiveR() {
      return switch (this.activeColor) {
         case CRIT -> this.critR;
         case NORMAL -> this.normR;
         case HEAL -> this.healR;
      };
   }

   private int getActiveG() {
      return switch (this.activeColor) {
         case CRIT -> this.critG;
         case NORMAL -> this.normG;
         case HEAL -> this.healG;
      };
   }

   private int getActiveB() {
      return switch (this.activeColor) {
         case CRIT -> this.critB;
         case NORMAL -> this.normB;
         case HEAL -> this.healB;
      };
   }

   private void setActiveR(int v) {
      switch (this.activeColor) {
         case CRIT:
            this.critR = v;
            break;
         case NORMAL:
            this.normR = v;
            break;
         case HEAL:
            this.healR = v;
      }
   }

   private void setActiveG(int v) {
      switch (this.activeColor) {
         case CRIT:
            this.critG = v;
            break;
         case NORMAL:
            this.normG = v;
            break;
         case HEAL:
            this.healG = v;
      }
   }

   private void setActiveB(int v) {
      switch (this.activeColor) {
         case CRIT:
            this.critB = v;
            break;
         case NORMAL:
            this.normB = v;
            break;
         case HEAL:
            this.healB = v;
      }
   }

   private int getActiveArgb() {
      return 0xFF000000 | this.getActiveR() << 16 | this.getActiveG() << 8 | this.getActiveB();
   }

   private String toHex(int r, int g, int b) {
      return String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
      super.render(g, mouseX, mouseY, pt);
      g.drawCenteredString(this.font, this.title, this.width / 2, 10, 16777215);
      int cx = this.width / 2;
      int previewX = cx + 5 + 150 + 10;
      int previewY = 206;
      String hex = this.toHex(this.getActiveR(), this.getActiveG(), this.getActiveB());
      g.drawString(this.font, this.activeColor.label + " " + hex, previewX, previewY - 12, 13421772);
      g.fill(previewX, previewY, previewX + 50, previewY + 22, -15658735);
      g.fill(previewX + 1, previewY + 1, previewX + 49, previewY + 21, -14540254);
      int color = this.getActiveArgb();

      String sample = switch (this.activeColor) {
         case CRIT -> "24.5";
         case NORMAL -> "12";
         case HEAL -> "+8";
      };
      int tw = this.font.width(sample);
      g.drawString(this.font, sample, previewX + (50 - tw) / 2, previewY + 6, color, true);
   }

   private void resetDefaults() {
      this.gravityVal = 0.15;
      this.airDragVal = 0.98;
      this.spawnImpulseVal = 3.0;
      this.critMultiplierVal = 1.5;
      this.lifetimeVal = 40;
      this.maxNumbersVal = 40;
      this.collisionsVal = true;
      this.sparksVal = true;
      this.healsVal = true;
      this.minScaleVal = 0.8;
      this.maxScaleVal = 2.0;
      this.refDamageVal = 20.0;
      this.critR = 255;
      this.critG = 170;
      this.critB = 0;
      this.normR = 255;
      this.normG = 255;
      this.normB = 255;
      this.healR = 85;
      this.healG = 255;
      this.healB = 85;
   }

   private void saveAndClose() {
      DamagePhysicsConfig.GRAVITY.set(this.gravityVal);
      DamagePhysicsConfig.AIR_DRAG.set(this.airDragVal);
      DamagePhysicsConfig.SPAWN_IMPULSE_BASE.set(this.spawnImpulseVal);
      DamagePhysicsConfig.CRIT_IMPULSE_MULTIPLIER.set(this.critMultiplierVal);
      DamagePhysicsConfig.LIFETIME_TICKS.set(this.lifetimeVal);
      DamagePhysicsConfig.MAX_CONCURRENT_NUMBERS.set(this.maxNumbersVal);
      DamagePhysicsConfig.ENABLE_COLLISIONS.set(this.collisionsVal);
      DamagePhysicsConfig.ENABLE_IMPACT_SPARKS.set(this.sparksVal);
      DamagePhysicsConfig.ENABLE_HEALING_NUMBERS.set(this.healsVal);
      DamagePhysicsConfig.MIN_FONT_SCALE.set(this.minScaleVal);
      DamagePhysicsConfig.MAX_FONT_SCALE.set(this.maxScaleVal);
      DamagePhysicsConfig.SCALE_REFERENCE_DAMAGE.set(this.refDamageVal);
      DamagePhysicsConfig.CRIT_COLOR.set(this.toHex(this.critR, this.critG, this.critB));
      DamagePhysicsConfig.NORMAL_COLOR.set(this.toHex(this.normR, this.normG, this.normB));
      DamagePhysicsConfig.HEAL_COLOR.set(this.toHex(this.healR, this.healG, this.healB));
      DamagePhysicsConfig.SPEC.save();
      this.onClose();
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parentScreen);
      }
   }

   private static enum ColorTarget {
      CRIT("Crit Hit"),
      NORMAL("Normal Hit"),
      HEAL("Healing");

      final String label;

      private ColorTarget(String label) {
         this.label = label;
      }
   }

   private static class ConfigSlider extends AbstractSliderButton {
      private final double min;
      private final double max;
      private final String prefix;
      private final Consumer<Double> onApply;

      public ConfigSlider(int x, int y, int w, int h, String prefix, double current, double min, double max, Consumer<Double> onApply) {
         super(x, y, w, h, Component.empty(), Math.max(0.0, Math.min(1.0, (current - min) / (max - min))));
         this.min = min;
         this.max = max;
         this.prefix = prefix;
         this.onApply = onApply;
         this.updateMessage();
      }

      protected void updateMessage() {
         double v = this.getVal();
         this.setMessage(
            Component.literal(
               Math.abs(v - (double)Math.round(v)) < 0.01
                  ? this.prefix + ": " + (int)Math.round(v)
                  : this.prefix + ": " + String.format(Locale.ROOT, "%.2f", v)
            )
         );
      }

      protected void applyValue() {
         this.onApply.accept(this.getVal());
      }

      double getVal() {
         return this.min + this.value * (this.max - this.min);
      }
   }

   private static class RGBSlider extends AbstractSliderButton {
      private final String label;
      private final Consumer<Integer> onApply;

      public RGBSlider(int x, int y, int w, int h, String label, int current, Consumer<Integer> onApply) {
         super(x, y, w, h, Component.empty(), (double)current / 255.0);
         this.label = label;
         this.onApply = onApply;
         this.updateMessage();
      }

      public void setVal(int v) {
         this.value = Math.max(0.0, Math.min(1.0, (double)v / 255.0));
         this.updateMessage();
      }

      protected void updateMessage() {
         this.setMessage(Component.literal(this.label + ": " + Math.round(this.value * 255.0)));
      }

      protected void applyValue() {
         this.onApply.accept((int)Math.round(this.value * 255.0));
      }
   }
}
