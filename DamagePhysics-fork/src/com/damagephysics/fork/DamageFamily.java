package com.damagephysics.fork;

/**
 * 伤害族（改版新增）：先分类，再按族决定底色 / 描边 / 是否参与冲击缩放。
 *
 * <ul>
 *   <li>{@link #DEBUFF}：中毒、凋零这类环境伤害 —— 固定颜色、固定字号、不参与冲击缩放。</li>
 *   <li>{@link #SPELL}：法术（铁魔法等）—— 法术底色 + 法术描边，参与冲击缩放。</li>
 *   <li>{@link #NORMAL}：近战/普通攻击 —— 冲击渐变底色 + 附魔时白色描边，参与冲击缩放。</li>
 *   <li>{@link #GUN}：枪械（Scorched Guns 等）—— 冲击渐变底色、无描边、不参与冲击缩放。</li>
 * </ul>
 */
public enum DamageFamily {
   DEBUFF,
   SPELL,
   NORMAL,
   GUN,
   /** 弓/弩/三叉戟等投掷物：不吃距离缩放，吃冲击缩放与生命变色，无描边。 */
   PROJECTILE,
   /** 非玩家的恢复生命数字：吃距离与冲击缩放，固定绿色、无描边。 */
   HEAL
}
