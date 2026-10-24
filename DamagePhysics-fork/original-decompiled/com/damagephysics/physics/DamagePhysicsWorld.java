package com.damagephysics.physics;

import com.damagephysics.config.DamagePhysicsConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DamagePhysicsWorld {
   private static final DamagePhysicsWorld INSTANCE = new DamagePhysicsWorld();
   public static final int SUB_STEPS = 6;
   private final List<DamageNumberBody> activeBodies = new ArrayList<>(64);
   private final List<DamageNumberBody> unmodifiableActiveBodies = Collections.unmodifiableList(this.activeBodies);
   private final ArrayDeque<DamageNumberBody> bodyPool = new ArrayDeque<>(64);

   public static DamagePhysicsWorld getInstance() {
      return INSTANCE;
   }

   private DamagePhysicsWorld() {
   }

   public DamageNumberBody spawnScreen(
      float screenX, float screenY, float damage, boolean isCrit, boolean isHeal, String text, int color, float baseScale, float textWidth, float textHeight
   ) {
      DamageNumberBody body = this.acquireBody();
      float baseImpulse = ((Double)DamagePhysicsConfig.SPAWN_IMPULSE_BASE.get()).floatValue();
      float critMultiplier = isCrit ? ((Double)DamagePhysicsConfig.CRIT_IMPULSE_MULTIPLIER.get()).floatValue() : 1.0F;
      int lifetime = (Integer)DamagePhysicsConfig.LIFETIME_TICKS.get();
      float upwardImpulse = -baseImpulse * critMultiplier;
      float horizontalSpread = ((float)Math.random() * 2.4F - 1.2F) * (isCrit ? 1.4F : 1.0F);
      body.initScreen(screenX, screenY, horizontalSpread, upwardImpulse, damage, text, isCrit, isHeal, color, baseScale, lifetime, textWidth, textHeight);
      this.activeBodies.add(body);
      this.enforceCapacity();
      return body;
   }

   public void tick() {
      if (!this.activeBodies.isEmpty()) {
         float gravity = ((Double)DamagePhysicsConfig.GRAVITY.get()).floatValue();
         float drag = ((Double)DamagePhysicsConfig.AIR_DRAG.get()).floatValue();
         boolean collisions = (Boolean)DamagePhysicsConfig.ENABLE_COLLISIONS.get();
         float dt = 0.008333334F;

         for (int step = 0; step < 6; step++) {
            for (int i = 0; i < this.activeBodies.size(); i++) {
               this.activeBodies.get(i).integrate(dt, gravity, drag);
            }

            if (collisions && this.activeBodies.size() > 1) {
               int size = this.activeBodies.size();

               for (int i = 0; i < size; i++) {
                  DamageNumberBody a = this.activeBodies.get(i);

                  for (int j = i + 1; j < size; j++) {
                     DamageNumberBody b = this.activeBodies.get(j);
                     DamageNumberContact.resolve(a, b);
                  }
               }
            }
         }

         for (int i = this.activeBodies.size() - 1; i >= 0; i--) {
            DamageNumberBody body = this.activeBodies.get(i);
            body.age++;
            if (body.isExpired()) {
               this.activeBodies.remove(i);
               this.releaseBody(body);
            }
         }
      }
   }

   private void enforceCapacity() {
      int max = (Integer)DamagePhysicsConfig.MAX_CONCURRENT_NUMBERS.get();
      if (this.activeBodies.size() > max) {
         int excess = this.activeBodies.size() - max;

         for (int i = 0; i < excess && i < this.activeBodies.size(); i++) {
            DamageNumberBody oldest = this.activeBodies.get(i);
            if (oldest.age < oldest.maxLifetime - 4) {
               oldest.age = oldest.maxLifetime - 4;
            }
         }
      }
   }

   private DamageNumberBody acquireBody() {
      DamageNumberBody body = this.bodyPool.poll();
      if (body == null) {
         body = new DamageNumberBody();
      }

      return body;
   }

   private void releaseBody(DamageNumberBody body) {
      body.reset();
      if (this.bodyPool.size() < 128) {
         this.bodyPool.offer(body);
      }
   }

   public void clear() {
      for (DamageNumberBody body : this.activeBodies) {
         body.reset();
         if (this.bodyPool.size() < 128) {
            this.bodyPool.offer(body);
         }
      }

      this.activeBodies.clear();
   }

   public List<DamageNumberBody> getActiveBodies() {
      return this.unmodifiableActiveBodies;
   }

   public int getActiveCount() {
      return this.activeBodies.size();
   }
}
