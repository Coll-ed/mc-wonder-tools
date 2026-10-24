package com.damagephysics.fork;

/**
 * 伤害类型分类（改版新增）。
 *
 * <p>用途：决定伤害数字的颜色与描边。分类来源是 {@code DamageSource.getMsgId()}
 * 与攻击者手持物品的附魔状态，全部是**只读**信息，不改变任何伤害逻辑。
 *
 * <p>为什么用 msgId 而不是 DamageType tag：整合包里带自定义伤害类型（法术/枪械等），
 * tag 未必覆盖全；msgId 是原版与大多数模组都会填的稳定字符串，命中率更高。
 */
public enum DamageCategory {
   /** 普通物理伤害（近战/箭矢等，未命中其它分类）。 */
   PHYSICAL,
   /** 魔法/法术伤害（msgId 含 magic）。 */
   MAGIC,
   /** 火焰/岩浆/燃烧。 */
   FIRE,
   /** 冰冻/细雪/霜冻。 */
   FROST,
   /** 雷击。 */
   LIGHTNING,
   /** 附魔武器造成的伤害（覆盖上面的分类，用于紫色荧光线）。 */
   ENCHANTED,
   /** 治疗数字。 */
   HEAL;

   /** 用于 msgId 关键字匹配的子串（全部小写）。 */
   public static DamageCategory fromMsgId(String msgId) {
      if (msgId == null || msgId.isEmpty()) {
         return PHYSICAL;
      }
      String id = msgId.toLowerCase(java.util.Locale.ROOT);
      if (id.contains("lightning")) {
         return LIGHTNING;
      }
      if (id.contains("freeze") || id.contains("frost") || id.contains("cold")) {
         return FROST;
      }
      if (id.contains("fire") || id.contains("lava") || id.contains("burn") || id.contains("hot")) {
         return FIRE;
      }
      if (id.contains("magic") || id.contains("spell") || id.contains("wither")) {
         return MAGIC;
      }
      return PHYSICAL;
   }
}
