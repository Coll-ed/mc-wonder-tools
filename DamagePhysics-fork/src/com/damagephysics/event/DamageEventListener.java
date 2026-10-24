package com.damagephysics.event;

import com.damagephysics.config.DamagePhysicsConfig;
import com.damagephysics.fork.DamageCategory;
import com.damagephysics.fork.DamageFamily;
import com.damagephysics.fork.ForkStyle;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * 伤害数字生成的监听器（改版）。
 *
 * <p>与上游 0.6.2 的唯一差异：{@link #spawnDamageIndicator} 里在算出字号后，
 * 再乘一个「相机到数字落点的世界距离」因子（{@link #computeDistanceScale}）。
 * 其余逻辑（血量差检测、暴击判定、治疗数字、暴击火花）与上游逐行一致。
 *
 * <p>为什么改在这里而不是渲染层：上游把数字在生成时就 {@code projectToScreen} 成屏幕像素，
 * body 里不再保留世界坐标；渲染时只有 {@code baseScale * popScale}，
 * 所以在生成处把距离因子乘进 {@code baseScale} 是等价且改动最小的做法。
 */
@OnlyIn(Dist.CLIENT)
public class DamageEventListener {
   private static final DamageEventListener INSTANCE = new DamageEventListener();

   /**
    * 取颜色：治疗走 healColor；开启类型着色时按分类取配置色，否则沿用上游逻辑。
    * 分类标记是一次性的——取走即清，避免下一次无来源的伤害沿用旧分类。
    */

   /**
    * 冲击比例 = 本次伤害 / 命中前生命，截断到 0..1。
    * 生命已为 0（击杀那一下）时按 1.0 处理——最后一击永远是最“有威胁”的。
    */
   private float computeImpactRatio(float damage, float healthBeforeHit) {
      if (healthBeforeHit <= 0.0F) {
         return 1.0F;
      }
      float ratio = damage / healthBeforeHit;
      return Math.max(0.0F, Math.min(1.0F, ratio));
   }

   /** 冲击比例 → 字号倍率（在配置的最小/最大值之间线性插值）。 */
   private float computeImpactScale(float impactRatio) {
      if (!(Boolean)DamagePhysicsConfig.ENABLE_IMPACT_SCALING.get()) {
         return 1.0F;
      }
      double lower = (Double)DamagePhysicsConfig.IMPACT_SCALE_MIN.get();
      double upper = (Double)DamagePhysicsConfig.IMPACT_SCALE_MAX.get();
      return (float)(lower + (upper - lower) * (double)impactRatio);
   }

   /**
    * 冲击比例 → 颜色：从 {@code impactColorLow}（几乎无威胁）插值到 {@code impactColorHigh}（致命）。
    * 关闭冲击着色时回退到“按伤害类型取色”。
    */
   private int resolveImpactColor(float impactRatio) {
      if (!(Boolean)DamagePhysicsConfig.ENABLE_IMPACT_COLORS.get()) {
         return DamageNumberStyle.getBaseColor(false, false);
      }
      int low = ForkStyle.parseHexOrDefault((String)DamagePhysicsConfig.IMPACT_COLOR_LOW.get(), 0xFFFFFFFF);
      int high = ForkStyle.parseHexOrDefault((String)DamagePhysicsConfig.IMPACT_COLOR_HIGH.get(), 0xFFFF2020);
      return lerpColor(low, high, impactRatio);
   }


   /** 单刀是否达到「最大生命的 X%」——达到则渲染成动态彩虹。 */
   private boolean isRainbowHit(float damage, LivingEntity entity) {
      double percent = (Double)DamagePhysicsConfig.RAINBOW_MAX_HEALTH_PERCENT.get();
      if (percent <= 0.0) {
         return false;
      }
      float maxHealth = entity.getMaxHealth();
      if (maxHealth <= 0.0F) {
         return false;
      }
      return (double)(damage / maxHealth) >= percent;
   }

   /** ARGB 逐通道线性插值（{@code t} 已在 0..1 范围内由调用方保证截断）。 */
   private static int lerpColor(int from, int to, float t) {
      int a = (int)Mth.lerp(t, (float)(from >>> 24 & 0xFF), (float)(to >>> 24 & 0xFF));
      int r = (int)Mth.lerp(t, (float)(from >>> 16 & 0xFF), (float)(to >>> 16 & 0xFF));
      int g = (int)Mth.lerp(t, (float)(from >>> 8 & 0xFF), (float)(to >>> 8 & 0xFF));
      int b = (int)Mth.lerp(t, (float)(from & 0xFF), (float)(to & 0xFF));
      return a << 24 | r << 16 | g << 8 | b;
   }

   /**
    * 距离为 0 时避免除零的保护阈值（方块）。
    * 取 1e-3：远小于任何有意义的相机距离，只是防止 {@code ref / 0} 得到 Infinity。
    */
   private static final double MIN_DISTANCE_EPSILON = 1.0E-3;

   private final Map<LivingEntity, Float> healthCache = new WeakHashMap<>();
   private final Map<LivingEntity, Long> lastCritHitTime = new WeakHashMap<>();
   /** 本次伤害的类型标记（由 {@link LivingDamageEvent.Post} 写入，生成数字时消费一次）。 */
   private final Map<LivingEntity, DamageCategory> pendingCategory = new WeakHashMap<>();
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

   /** 本类自己的日志器（不依赖 DamagePhysicsMod 的私有 LOGGER）。 */
   private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

   /**
    * 一次伤害的证据（按受伤者保存，避免连击/多目标时互相覆盖）。
    * 服务端线程写入、客户端线程读取，同一进程（单人/整合服务器）内有效。
    */
   private record DamageProbe(String typeId, boolean hasAttacker, boolean hasDirectEntity) {
   }

   /** 本次生成数字所用的 typeId（由 classifyFamily 从证据里取出）。 */
   private String currentTypeId = null;

   /** 受伤者 → 最近一次伤害的证据。 */
   private final Map<LivingEntity, DamageProbe> probes = new WeakHashMap<>();

   /** 最近一次写入的 typeId，仅用于调试日志（分类不依赖它）。 */
   private String lastProbeTypeId = null;

   /** 本次命中的伤害类型（供同一刀的后续判断复用：减益类要固定样式）。 */
   private DamageCategory lastCategory = null;

   /**
    * 客户端可用的分类判据。
    *
    * <p>为什么不用 {@code LivingDamageEvent}：那是**服务端**事件，客户端收不到，
    * 于是分类永远为空、颜色只能回退白色（实测就是这个现象）。
    *
    * <p>判据优先级（越靠前越具体）：
    * <ol>
    *   <li>减益由 {@link #looksLikeDebuff()} 自动判定（DamageType 关键字 / 无攻击者）</li>
    *   <li>手持物品来自 {@code irons_spellbooks} → MAGIC</li>
    *   <li>手持物品带附魔 → ENCHANTED</li>
    *   <li>受伤者着火 → FIRE；被冻结 → FROST</li>
    *   <li>都不满足 → PHYSICAL</li>
    * </ol>
    */
   private DamageCategory classifyFromLocalPlayer(LivingEntity victim) {
      Minecraft mc = Minecraft.getInstance();
      if (victim != null) {
         if (victim.isOnFire()) {
            return DamageCategory.FIRE;
         }
         if (victim.isFullyFrozen()) {
            return DamageCategory.FROST;
         }
      }
      LocalPlayer player = mc.player;
      if (player == null) {
         return victim == null ? null : DamageCategory.PHYSICAL;
      }
      var held = player.getMainHandItem();
      if (held.isEmpty()) {
         return DamageCategory.PHYSICAL;
      }
      var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem());
      if (id != null && "irons_spellbooks".equals(id.getNamespace())) {
         return DamageCategory.MAGIC;
      }
      if (held.isEnchanted()) {
         return DamageCategory.ENCHANTED;
      }
      return DamageCategory.PHYSICAL;
   }

   /** 枪械类物品的命名空间（Scorched Guns 等，全部来自本整合包实测的注册名）。 */
   private static final java.util.Set<String> GUN_NAMESPACES = java.util.Set.of("scguns");

   /** 法术类物品的命名空间（Iron's Spellbooks）。 */
   private static final java.util.Set<String> SPELL_NAMESPACES = java.util.Set.of("irons_spellbooks");

   /**
    * 先分族：DEBUFF（中毒/凋零）→ SPELL（法术）→ GUN（枪械）→ NORMAL（其余）。
    * 同时把细分类型写进 {@link #lastCategory}，供底色/描边取色使用。
    */
   private DamageFamily classifyFamily(LivingEntity entity) {
      // 清掉上一次的分类残留：减益跳字绝不能“沿用”上一刀的数字样式
      DamageProbe probe = entity == null ? null : this.probes.get(entity);
      this.currentTypeId = probe == null ? null : probe.typeId();
      this.lastCategory = DamageCategory.PHYSICAL;
      // ① DamageType 自动判定（整合服务器有效）
      if ((Boolean)DamagePhysicsConfig.DEBUG_CLASSIFICATION.get()) {
         LOGGER.info(
            "[damagephysics-fork] typeId={} attacker={} direct={} victim={}",
            this.currentTypeId, probe != null && probe.hasAttacker(), probe != null && probe.hasDirectEntity(),
            entity == null ? "null" : entity.getType().toString());
      }
      if (this.looksLikeDebuff(entity)) {
         if ((Boolean)DamagePhysicsConfig.DEBUG_CLASSIFICATION.get()) {
            LOGGER.info("[damagephysics-fork] -> DEBUFF");
         }
         return DamageFamily.DEBUFF;
      }
      // ①-b 法术：DamageType id 命中法术关键字，或来自 irons_spellbooks
      if (this.looksLikeSpellType(entity)) {
         this.lastCategory = DamageCategory.MAGIC;
         return DamageFamily.SPELL;
      }
      // ② 兜底：受伤者挂着中毒/凋零效果
      if (entity != null && (entity.hasEffect(MobEffects.POISON) || entity.hasEffect(MobEffects.WITHER))) {
         return DamageFamily.DEBUFF;
      }

      LocalPlayer player = Minecraft.getInstance().player;
      var held = player == null ? net.minecraft.world.item.ItemStack.EMPTY : player.getMainHandItem();
      if (!held.isEmpty()) {
         var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem());
         if (id != null) {
            String namespace = id.getNamespace();
            if (GUN_NAMESPACES.contains(namespace)) {
               return DamageFamily.GUN;
            }
            boolean extraProjectileNamespace = ((java.util.List<? extends String>)DamagePhysicsConfig.PROJECTILE_NAMESPACES.get())
               .contains(namespace);
            if (held.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem
               || held.getItem() instanceof net.minecraft.world.item.TridentItem
               || extraProjectileNamespace) {
               return DamageFamily.PROJECTILE;
            }
            if (SPELL_NAMESPACES.contains(namespace)) {
               this.lastCategory = DamageCategory.MAGIC;
               return DamageFamily.SPELL;
            }
         }
         if (held.isEnchanted()) {
            this.lastCategory = DamageCategory.ENCHANTED;
         }
      }
      return DamageFamily.NORMAL;
   }

   /** DamageType id 含法术关键字即视为法术（铁魔法等模组的伤害类型都带 spell/magic/arcane）。 */
   private boolean looksLikeSpellType(LivingEntity entity) {
      DamageProbe probe = entity == null ? null : this.probes.get(entity);
      String typeId = probe == null || probe.typeId() == null
         ? "" : probe.typeId().toLowerCase(java.util.Locale.ROOT);
      var keywords = (java.util.List<? extends String>)DamagePhysicsConfig.SPELL_TYPE_KEYWORDS.get();
      for (String keyword : keywords) {
         if (!keyword.isEmpty() && typeId.contains(keyword.toLowerCase(java.util.Locale.ROOT))) {
            return true;
         }
      }
      return false;
   }

   /**
    * 自动判定是否为 debuff/DoT：
    * <ol>
    *   <li>DamageType id 命中 {@code debuffTypeKeywords} 任一子串；</li>
    *   <li>没有攻击者也没有直接来源，且 id 不在 {@code debuffEnvironmentIds} 里。</li>
    * </ol>
    */
   private boolean looksLikeDebuff(LivingEntity entity) {
      if (!(Boolean)DamagePhysicsConfig.DEBUFF_AUTO_DETECT.get()) {
         return false;
      }
      DamageProbe probe = entity == null ? null : this.probes.get(entity);
      String typeId = probe == null || probe.typeId() == null
         ? "" : probe.typeId().toLowerCase(java.util.Locale.ROOT);
      var keywords = (java.util.List<? extends String>)DamagePhysicsConfig.DEBUFF_TYPE_KEYWORDS.get();
      for (String keyword : keywords) {
         if (!keyword.isEmpty() && typeId.contains(keyword.toLowerCase(java.util.Locale.ROOT))) {
            return true;
         }
      }
      if (probe == null
         || !(Boolean)DamagePhysicsConfig.DEBUFF_REQUIRE_NO_ATTACKER.get()
         || probe.hasAttacker() || probe.hasDirectEntity()) {
         return false;
      }
      var envIds = (java.util.List<? extends String>)DamagePhysicsConfig.DEBUFF_ENVIRONMENT_IDS.get();
      for (String env : envIds) {
         if (typeId.endsWith(":" + env) || typeId.equals(env)) {
            return false;
         }
      }
      return true;
   }

   /** 手持法术物品的稀有度颜色（Rarity#color），没有物品时返回 null。 */
   private Integer heldRarityColor() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return null;
      }
      // 主手 → 副手：铁魔法常把法术书拿在副手或靠快捷键施放
      var held = player.getMainHandItem();
      if (held.isEmpty()) {
         held = player.getOffhandItem();
      }
      if (held.isEmpty()) {
         return null;
      }
      String hex = switch (held.getRarity()) {
         case COMMON -> (String)DamagePhysicsConfig.COLOR_RARITY_COMMON.get();
         case UNCOMMON -> (String)DamagePhysicsConfig.COLOR_RARITY_UNCOMMON.get();
         case RARE -> (String)DamagePhysicsConfig.COLOR_RARITY_RARE.get();
         case EPIC -> (String)DamagePhysicsConfig.COLOR_RARITY_EPIC.get();
         default -> (String)DamagePhysicsConfig.COLOR_RARITY_OTHER.get();
      };
      int parsed = ForkStyle.parseHexOrDefault(hex, 0);
      return parsed == 0 ? null : parsed;
   }

   /** 手持物品是否附魔（普攻描边判据，独立于分类缓存，避免状态残留）。 */
   private boolean isHeldItemEnchanted() {
      LocalPlayer player = Minecraft.getInstance().player;
      return player != null && player.getMainHandItem().isEnchanted();
   }

   /** 记录本次伤害的类型，供随后生成数字时用（血量差检测那条路径本身拿不到 DamageSource）。 */
   @SubscribeEvent
   public void onLivingDamage(LivingDamageEvent.Post event) {
      LivingEntity victim = event.getEntity();
      if (victim == null) {
         return;
      }
      var source = event.getSource();
      var typeKey = source.typeHolder().unwrapKey();
      String typeId = typeKey.map(k -> k.location().toString()).orElse(source.getMsgId());
      this.lastProbeTypeId = typeId;
      this.probes.put(victim, new DamageProbe(
         typeId, source.getEntity() != null, source.getDirectEntity() != null));
      DamageCategory category = DamageCategory.fromMsgId(source.getMsgId());
      Entity attacker = event.getSource().getEntity();
      if (attacker instanceof LocalPlayer player && player.getMainHandItem().isEnchanted()) {
         category = DamageCategory.ENCHANTED;
      }
      this.pendingCategory.put(victim, category);
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

         // 玩家自身的伤害/治疗数字不显示：这里刻意不调用 processEntity(player)
      }
   }

   private void processEntity(LivingEntity entity) {
      float currentHealth = entity.getHealth();
      Float previousHealth = this.healthCache.get(entity);
      if (previousHealth != null) {
         float delta = previousHealth - currentHealth;
         if (delta > 0.05F) {
            boolean isCrit = this.checkAndConsumeCrit(entity);
            this.spawnDamageIndicator(entity, delta, isCrit, false, previousHealth);
         } else if (delta < -0.05F && (Boolean)DamagePhysicsConfig.ENABLE_HEALING_NUMBERS.get()) {
            float healAmount = -delta;
            this.spawnDamageIndicator(entity, healAmount, false, true, previousHealth);
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

   private void spawnDamageIndicator(LivingEntity entity, float amount, boolean isCrit, boolean isHeal, float healthBeforeHit) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.font != null) {
         double offsetX = (Math.random() - 0.5) * 0.3;
         double offsetY = (double)entity.getBbHeight() * 0.75 + (Math.random() - 0.5) * 0.2;
         double offsetZ = (Math.random() - 0.5) * 0.3;
         Vec3 worldPos = entity.position().add(offsetX, offsetY, offsetZ);
         Vec2 screenPos = DamageNumberRenderer.getInstance().projectToScreen(worldPos, this.tempScreenPos);
         if (screenPos != null) {
            String formattedText = DamageNumberStyle.formatDamageText(amount, isHeal);
            // ── 改版：第一步先分族，再按族决定底色 / 描边 / 是否走冲击缩放 ──
            DamageFamily family = isHeal ? DamageFamily.HEAL : this.classifyFamily(entity);
            DamageCategory category = isHeal ? null : this.lastCategory;
            float impactRatio = isHeal ? 0.0F : computeImpactRatio(amount, healthBeforeHit);
            float scaleRatio = (float)Math.pow(impactRatio, (Double)DamagePhysicsConfig.IMPACT_SCALE_GAMMA.get());
            float colorRatio = (float)Math.pow(impactRatio, (Double)DamagePhysicsConfig.IMPACT_COLOR_GAMMA.get());

            boolean useImpact;
            int color;
            int typeColor;
            if (isHeal) {
               useImpact = (Boolean)DamagePhysicsConfig.FAMILY_HEAL_USES_IMPACT.get();
               color = DamageNumberStyle.getBaseColor(false, true);
               typeColor = (Boolean)DamagePhysicsConfig.FAMILY_HEAL_OUTLINE.get() ? 0 : 0;
            } else {
               switch (family) {
                  case DEBUFF -> {
                     useImpact = (Boolean)DamagePhysicsConfig.FAMILY_DEBUFF_USES_IMPACT.get();
                     // 减益：固定底色（默认纯白），默认不描边，完全独立于之前的显示样式
                     color = ForkStyle.colorForTypeId(this.currentTypeId);
                     if (color == 0) {
                        color = ForkStyle.parseHexOrDefault((String)DamagePhysicsConfig.DEBUFF_FALLBACK_COLOR.get(), 0xFFFFFFFF);
                     }
                     typeColor = (Boolean)DamagePhysicsConfig.DEBUFF_OUTLINE.get()
                        ? ForkStyle.parseHexOrDefault(ForkStyle.outlineHexFor(category), 0)
                        : 0;
                  }
                  case SPELL -> {
                     useImpact = (Boolean)DamagePhysicsConfig.FAMILY_SPELL_USES_IMPACT.get();
                     // 三层取色：① 手持(主手→副手)法术物品稀有度色 ② DamageType 类型色表 ③ 法术兜底色
                     Integer rarityColor = (Boolean)DamagePhysicsConfig.SPELL_USE_RARITY_COLOR.get()
                        ? this.heldRarityColor()
                        : null;
                     int typeFill = ForkStyle.colorForTypeId(this.currentTypeId);
                     if (rarityColor != null) {
                        color = rarityColor;
                     } else if (typeFill != 0) {
                        color = typeFill;
                     } else if ((Boolean)DamagePhysicsConfig.FAMILY_SPELL_USES_TYPE_COLOR.get()) {
                        color = ForkStyle.parseHexOrDefault(ForkStyle.hexFor(category), 0xFFFFFFFF);
                     } else {
                        // 法术始终固定色，绝不回退到“按冲击变色”（用户要求：法术不随伤害变色）
                        color = ForkStyle.parseHexOrDefault(ForkStyle.hexFor(DamageCategory.MAGIC), 0xFFB388FF);
                     }
                     // 描边统一浅紫（与附魔深紫区分）
                     typeColor = (Boolean)DamagePhysicsConfig.FAMILY_SPELL_OUTLINE.get()
                        ? ForkStyle.parseHexOrDefault((String)DamagePhysicsConfig.SPELL_OUTLINE_COLOR.get(), 0)
                        : 0;
                  }
                  case PROJECTILE -> {
                     useImpact = (Boolean)DamagePhysicsConfig.FAMILY_PROJECTILE_USES_IMPACT.get();
                     color = this.resolveImpactColor(colorRatio);
                     typeColor = (Boolean)DamagePhysicsConfig.FAMILY_PROJECTILE_OUTLINE.get() ? 0 : 0;
                  }
                  case GUN -> {
                     useImpact = (Boolean)DamagePhysicsConfig.FAMILY_GUN_USES_IMPACT.get();
                     color = this.resolveImpactColor(colorRatio);
                     typeColor = (Boolean)DamagePhysicsConfig.FAMILY_GUN_OUTLINE.get() ? 0 : 0;
                  }
                  default -> {
                     useImpact = (Boolean)DamagePhysicsConfig.FAMILY_NORMAL_USES_IMPACT.get();
                     color = this.resolveImpactColor(colorRatio);
                     typeColor = (Boolean)DamagePhysicsConfig.FAMILY_NORMAL_OUTLINE_WHEN_ENCHANTED.get()
                        && this.isHeldItemEnchanted()
                        ? ForkStyle.parseHexOrDefault((String)DamagePhysicsConfig.ENCHANT_OUTLINE_COLOR.get(), 0)
                        : 0;
                  }
               }
            }

            // 字号只由「对受伤者的影响」决定：不再按伤害数值放大
            float scale = ((Double)DamagePhysicsConfig.BASE_FONT_SCALE.get()).floatValue()
               * (isCrit ? ((Double)DamagePhysicsConfig.CRIT_FONT_SCALE.get()).floatValue() : 1.0F);
            boolean useDistance = switch (family) {
               case HEAL -> (Boolean)DamagePhysicsConfig.FAMILY_HEAL_USES_DISTANCE.get();
               case DEBUFF -> (Boolean)DamagePhysicsConfig.FAMILY_DEBUFF_USES_DISTANCE.get();
               case SPELL -> (Boolean)DamagePhysicsConfig.FAMILY_SPELL_USES_DISTANCE.get();
               case GUN -> (Boolean)DamagePhysicsConfig.FAMILY_GUN_USES_DISTANCE.get();
               case PROJECTILE -> (Boolean)DamagePhysicsConfig.FAMILY_PROJECTILE_USES_DISTANCE.get();
               case NORMAL -> (Boolean)DamagePhysicsConfig.FAMILY_NORMAL_USES_DISTANCE.get();
            };
            if (useDistance) {
               scale *= this.computeDistanceScale(worldPos);
            }
            if (family == DamageFamily.DEBUFF && !useImpact) {
               scale *= ((Double)DamagePhysicsConfig.DEBUFF_FIXED_SCALE.get()).floatValue();
            } else if (useImpact) {
               scale *= computeImpactScale(scaleRatio);
            }
            int textWidth = mc.font.width(formattedText);
            int textHeight = 9;
            DamagePhysicsWorld.getInstance()
               .spawnScreen(screenPos.x, screenPos.y, amount, isCrit, isHeal, formattedText, color, scale, (float)textWidth, (float)textHeight);
            // ── 改版：把这次命中锚定到世界坐标，供 3D 渲染每帧重投影 ──
            var spawned = DamagePhysicsWorld.getInstance().getActiveBodies();
            if (!spawned.isEmpty()) {
               var body = spawned.get(spawned.size() - 1);
               body.worldX = worldPos.x;
               body.worldY = worldPos.y;
               body.worldZ = worldPos.z;
               body.worldAnchored = (Boolean)DamagePhysicsConfig.ENABLE_WORLD_SPACE.get();
               body.impactRatio = impactRatio;
               body.rainbow = !isHeal && family != DamageFamily.DEBUFF && useImpact && isRainbowHit(amount, entity);
               body.typeColor = typeColor;
            }
            if (isCrit) {
               ImpactSparkManager.getInstance().spawnScreenCritBurst(screenPos.x, screenPos.y, color);
            }
         }
      }
   }



   /**
    * 倒数曲线：{@code scale = clamp(reference / distance, min, max)}。
    * 参考距离处正好 1.0；越近越大（封顶 max），越远越小（封底 min）。
    */
   private float computeDistanceScale(Vec3 worldPos) {
      if (!(Boolean)DamagePhysicsConfig.ENABLE_DISTANCE_SCALE.get()) {
         return 1.0F;
      }

      double reference = (Double)DamagePhysicsConfig.DISTANCE_SCALE_REFERENCE.get();
      double lowerClamp = (Double)DamagePhysicsConfig.DISTANCE_SCALE_MIN.get();
      double upperClamp = (Double)DamagePhysicsConfig.DISTANCE_SCALE_MAX.get();

      Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      double distance = camPos.distanceTo(worldPos);
      if (distance < MIN_DISTANCE_EPSILON) {
         return (float)upperClamp;
      }

      double factor = reference / distance;
      return (float)Math.max(lowerClamp, Math.min(upperClamp, factor));
   }

   public void clear() {
      this.healthCache.clear();
      this.lastCritHitTime.clear();
   }
}
