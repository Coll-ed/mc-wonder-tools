package com.damagephysics.event;

import com.damagephysics.config.DamagePhysicsConfig;
import com.damagephysics.particle.ImpactSparkManager;
import com.damagephysics.physics.DamagePhysicsWorld;
import com.damagephysics.physics.Vec2;
import com.damagephysics.render.DamageNumberRenderer;
import com.damagephysics.render.DamageNumberStyle;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@OnlyIn(Dist.CLIENT)
public class DamageEventListener {
   private static final DamageEventListener INSTANCE = new DamageEventListener();
   private final Map<LivingEntity, Float> healthCache = new WeakHashMap<>();
   private final Map<LivingEntity, Long> lastCritHitTime = new WeakHashMap<>();
   private final Vec2 tempScreenPos = new Vec2();

   public static DamageEventListener getInstance() {
      return INSTANCE;
   }

   private DamageEventListener() {
   }

   @SubscribeEvent
   public void onPlayerAttack(AttackEntityEvent event) {
      if (event.getEntity() instanceof LocalPlayer player && event.getTarget() instanceof LivingEntity target) {
         boolean isCrit = this.isPlayerCriticalHit(player);
         if (isCrit) {
            this.lastCritHitTime.put(target, System.currentTimeMillis());
         }
      }
   }

   private boolean isPlayerCriticalHit(LocalPlayer player) {
      return player.fallDistance > 0.0F
         && !player.onGround()
         && !player.onClimbable()
         && !player.isInWater()
         && !player.hasEffect(MobEffects.BLINDNESS)
         && !player.isPassenger();
   }

   public void scanEntityHealth(ClientLevel level) {
      if (level != null) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;

         for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living) {
               this.processEntity(living);
            }
         }

         if (player != null) {
            this.processEntity(player);
         }
      }
   }

   private void processEntity(LivingEntity entity) {
      float currentHealth = entity.getHealth();
      Float previousHealth = this.healthCache.get(entity);
      if (previousHealth != null) {
         float delta = previousHealth - currentHealth;
         if (delta > 0.05F) {
            boolean isCrit = this.checkAndConsumeCrit(entity);
            this.spawnDamageIndicator(entity, delta, isCrit, false);
         } else if (delta < -0.05F && (Boolean)DamagePhysicsConfig.ENABLE_HEALING_NUMBERS.get()) {
            float healAmount = -delta;
            this.spawnDamageIndicator(entity, healAmount, false, true);
         }
      }

      this.healthCache.put(entity, currentHealth);
   }

   private boolean checkAndConsumeCrit(LivingEntity entity) {
      Long critTime = this.lastCritHitTime.get(entity);
      if (critTime != null) {
         long elapsed = System.currentTimeMillis() - critTime;
         if (elapsed <= 350L) {
            this.lastCritHitTime.remove(entity);
            return true;
         }
      }

      return false;
   }

   private void spawnDamageIndicator(LivingEntity entity, float amount, boolean isCrit, boolean isHeal) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.font != null) {
         double offsetX = (Math.random() - 0.5) * 0.3;
         double offsetY = (double)entity.getBbHeight() * 0.75 + (Math.random() - 0.5) * 0.2;
         double offsetZ = (Math.random() - 0.5) * 0.3;
         Vec3 worldPos = entity.position().add(offsetX, offsetY, offsetZ);
         Vec2 screenPos = DamageNumberRenderer.getInstance().projectToScreen(worldPos, this.tempScreenPos);
         if (screenPos != null) {
            String formattedText = DamageNumberStyle.formatDamageText(amount, isHeal);
            int color = DamageNumberStyle.getBaseColor(isCrit, isHeal);
            float scale = DamageNumberStyle.computeFontScale(amount, isCrit);
            int textWidth = mc.font.width(formattedText);
            int textHeight = 9;
            DamagePhysicsWorld.getInstance()
               .spawnScreen(screenPos.x, screenPos.y, amount, isCrit, isHeal, formattedText, color, scale, (float)textWidth, (float)textHeight);
            if (isCrit) {
               ImpactSparkManager.getInstance().spawnScreenCritBurst(screenPos.x, screenPos.y, color);
            }
         }
      }
   }

   public void clear() {
      this.healthCache.clear();
      this.lastCritHitTime.clear();
   }
}
