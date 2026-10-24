package com.damagephysics.particle;

import com.damagephysics.config.DamagePhysicsConfig;
import com.damagephysics.render.DamageNumberStyle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;

public class ImpactSparkManager {
   private static final ImpactSparkManager INSTANCE = new ImpactSparkManager();
   private static final Random RANDOM = new Random();
   private final List<ImpactSparkManager.Spark> activeSparks = new ArrayList<>(64);
   private final ArrayDeque<ImpactSparkManager.Spark> sparkPool = new ArrayDeque<>(64);

   public static ImpactSparkManager getInstance() {
      return INSTANCE;
   }

   private ImpactSparkManager() {
   }

   public void spawnScreenCritBurst(float screenX, float screenY, int baseColor) {
      if ((Boolean)DamagePhysicsConfig.ENABLE_IMPACT_SPARKS.get()) {
         int sparkCount = 6 + RANDOM.nextInt(5);

         for (int i = 0; i < sparkCount; i++) {
            ImpactSparkManager.Spark spark = this.acquireSpark();
            float angle = (float)((double)RANDOM.nextFloat() * Math.PI * 2.0);
            float speed = 1.5F + RANDOM.nextFloat() * 3.5F;
            spark.x = screenX + (RANDOM.nextFloat() * 8.0F - 4.0F);
            spark.y = screenY + (RANDOM.nextFloat() * 8.0F - 4.0F);
            spark.vx = (float)Math.cos((double)angle) * speed;
            spark.vy = (float)Math.sin((double)angle) * speed - 1.5F;
            spark.color = baseColor;
            spark.age = 0;
            spark.maxAge = 10 + RANDOM.nextInt(8);
            spark.size = 1.5F + RANDOM.nextFloat() * 1.5F;
            spark.active = true;
            this.activeSparks.add(spark);
         }
      }
   }

   public void tick() {
      if (!this.activeSparks.isEmpty()) {
         for (int i = this.activeSparks.size() - 1; i >= 0; i--) {
            ImpactSparkManager.Spark spark = this.activeSparks.get(i);
            spark.age++;
            spark.x = spark.x + spark.vx;
            spark.y = spark.y + spark.vy;
            spark.vy += 0.25F;
            spark.vx *= 0.92F;
            spark.vy *= 0.92F;
            if (spark.age >= spark.maxAge) {
               this.activeSparks.remove(i);
               this.releaseSpark(spark);
            }
         }
      }
   }

   public void renderScreen(GuiGraphics guiGraphics) {
      if (!this.activeSparks.isEmpty()) {
         for (ImpactSparkManager.Spark spark : this.activeSparks) {
            float alpha = 1.0F - (float)spark.age / (float)spark.maxAge;
            if (!(alpha <= 0.0F)) {
               int colorWithAlpha = DamageNumberStyle.applyAlpha(spark.color, alpha);
               int halfSize = Math.max(1, Math.round(spark.size * 0.5F));
               int ix = Math.round(spark.x);
               int iy = Math.round(spark.y);
               guiGraphics.fill(ix - halfSize, iy - halfSize, ix + halfSize, iy + halfSize, colorWithAlpha);
            }
         }
      }
   }

   private ImpactSparkManager.Spark acquireSpark() {
      ImpactSparkManager.Spark spark = this.sparkPool.poll();
      if (spark == null) {
         spark = new ImpactSparkManager.Spark();
      }

      return spark;
   }

   private void releaseSpark(ImpactSparkManager.Spark spark) {
      spark.reset();
      if (this.sparkPool.size() < 128) {
         this.sparkPool.offer(spark);
      }
   }

   public void clear() {
      for (ImpactSparkManager.Spark spark : this.activeSparks) {
         this.releaseSpark(spark);
      }

      this.activeSparks.clear();
   }

   public static class Spark {
      public float x;
      public float y;
      public float vx;
      public float vy;
      public int color;
      public int age;
      public int maxAge;
      public float size;
      public boolean active;

      public void reset() {
         this.x = this.y = 0.0F;
         this.vx = this.vy = 0.0F;
         this.color = 0;
         this.age = 0;
         this.maxAge = 0;
         this.size = 1.0F;
         this.active = false;
      }
   }
}
