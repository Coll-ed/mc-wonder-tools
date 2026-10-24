package com.damagephysics.physics;

public class DamageNumberBody {
   public final Vec2 position = new Vec2();
   public final Vec2 prevPosition = new Vec2();
   public final Vec2 velocity = new Vec2();
   public float mass = 1.0F;
   public float invMass = 1.0F;
   public float angle = 0.0F;
   public float prevAngle = 0.0F;
   public float angularVelocity = 0.0F;
   public float deformationX = 1.0F;
   public float deformationY = 1.0F;
   public int age = 0;
   public int maxLifetime = 40;
   public float damageAmount = 0.0F;
   public String text = "";
   public boolean isCrit = false;
   public boolean isHeal = false;
   public int color = -1;
   public float baseScale = 1.0F;
   public float width = 12.0F;
   public float height = 9.0F;
   public boolean active = false;

   // ── 改版新增：世界锚点（3D 渲染用）──
   /** 命中点在世界中的坐标（方块）。 */
   public double worldX = 0.0;
   public double worldY = 0.0;
   public double worldZ = 0.0;
   /** true 表示该数字锚定在世界坐标上，渲染时每帧重新投影（随相机透视缩放）。 */
   public boolean worldAnchored = false;
   /** 生成时的屏幕坐标，用于把屏幕空间的物理位移换算成相对锚点的像素偏移。 */
   public float spawnScreenX = 0.0F;
   public float spawnScreenY = 0.0F;
   /** 本次伤害占命中前生命的比例（0..1），用于大小/颜色插值。 */
   public float impactRatio = 0.0F;
   /** 单刀超过最大生命阈值时为 true：渲染时走动态彩虹色。 */
   public boolean rainbow = false;
   /** 伤害类型对应的描边色（ARGB）；0 表示不描边。 */
   public int typeColor = 0;

   public DamageNumberBody() {
      this.reset();
   }

   public void reset() {
      this.position.setZero();
      this.prevPosition.setZero();
      this.velocity.setZero();
      this.mass = 1.0F;
      this.invMass = 1.0F;
      this.angle = 0.0F;
      this.prevAngle = 0.0F;
      this.angularVelocity = 0.0F;
      this.deformationX = 1.0F;
      this.deformationY = 1.0F;
      this.age = 0;
      this.maxLifetime = 40;
      this.damageAmount = 0.0F;
      this.text = "";
      this.isCrit = false;
      this.isHeal = false;
      this.color = -1;
      this.baseScale = 1.0F;
      this.width = 12.0F;
      this.height = 9.0F;
      this.active = false;
      this.worldX = 0.0;
      this.worldY = 0.0;
      this.worldZ = 0.0;
      this.worldAnchored = false;
      this.spawnScreenX = 0.0F;
      this.spawnScreenY = 0.0F;
      this.impactRatio = 0.0F;
      this.rainbow = false;
      this.typeColor = 0;
   }

   public void initScreen(
      float startX,
      float startY,
      float velX,
      float velY,
      float damage,
      String text,
      boolean isCrit,
      boolean isHeal,
      int color,
      float baseScale,
      int maxLifetime,
      float textWidth,
      float textHeight
   ) {
      this.position.set(startX, startY);
      this.prevPosition.set(startX, startY);
      this.spawnScreenX = startX;
      this.spawnScreenY = startY;
      this.velocity.set(velX, velY);
      this.mass = Math.max(0.5F, Math.min(3.0F, damage / 5.0F));
      this.invMass = 1.0F / this.mass;
      this.angle = 0.0F;
      this.prevAngle = 0.0F;
      this.angularVelocity = velX * 0.02F;
      this.deformationX = isCrit ? 1.4F : 1.15F;
      this.deformationY = isCrit ? 0.7F : 0.9F;
      this.age = 0;
      this.maxLifetime = maxLifetime;
      this.damageAmount = damage;
      this.text = text;
      this.isCrit = isCrit;
      this.isHeal = isHeal;
      this.color = color;
      this.baseScale = baseScale;
      this.width = Math.max(8.0F, textWidth * baseScale);
      this.height = Math.max(8.0F, textHeight * baseScale);
      this.active = true;
   }

   public void integrate(float dt, float gravity, float dragCoeff) {
      this.prevAngle = this.angle;
      float substepDrag = (float)Math.pow((double)dragCoeff, (double)dt);
      this.angularVelocity *= substepDrag;
      this.angle = this.angle + this.angularVelocity * dt * 60.0F;
      this.prevPosition.set(this.position);
      this.velocity.y += gravity * dt * 60.0F;
      this.velocity.scale(substepDrag);
      this.position.x = this.position.x + this.velocity.x * dt * 60.0F;
      this.position.y = this.position.y + this.velocity.y * dt * 60.0F;
      float deformRelaxSpeed = 8.0F * dt;
      this.deformationX = this.deformationX + (1.0F - this.deformationX) * deformRelaxSpeed;
      this.deformationY = this.deformationY + (1.0F - this.deformationY) * deformRelaxSpeed;
   }

   public float getAlpha() {
      if (this.active && this.maxLifetime > 0) {
         float progress = (float)this.age / (float)this.maxLifetime;
         if (progress < 0.75F) {
            return 1.0F;
         } else {
            float fadeProgress = (progress - 0.75F) / 0.25F;
            float clamped = Math.max(0.0F, Math.min(1.0F, 1.0F - fadeProgress));
            return clamped * clamped;
         }
      } else {
         return 0.0F;
      }
   }

   public float getPopScale() {
      if (this.age < 5) {
         float t = (float)this.age / 5.0F;
         float popMultiplier = this.isCrit ? 1.4F : 1.15F;
         return 1.0F + (popMultiplier - 1.0F) * (1.0F - t);
      } else {
         return 1.0F;
      }
   }

   public Vec2 getInterpolatedPosition(float partialTick, Vec2 out) {
      return Vec2.lerp(this.prevPosition, this.position, partialTick, out);
   }

   public float getInterpolatedAngle(float partialTick) {
      return this.prevAngle + (this.angle - this.prevAngle) * partialTick;
   }

   public boolean isExpired() {
      return !this.active || this.age >= this.maxLifetime;
   }
}
