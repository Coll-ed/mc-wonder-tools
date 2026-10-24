package com.damagephysics.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * DamagePhysics 配置（改版）。
 *
 * <p>与上游 0.6.2 的差异：只新增 {@code [distance]} 一组键，用于按距离缩放伤害数字；
 * 原有 {@code physics.*} / {@code visuals.*} 的键名、默认值与取值范围全部保持上游原样，
 * 这样玩家已有的 {@code damagephysics-client.toml} 不会被重置。
 *
 * <p>关于初始化顺序：反编译出来的源码把 {@code SPEC} 排在了 {@code BUILDER} 之前，
 * 那是反编译器对 {@code <clinit>} 顺序的错误呈现；照抄会在类初始化时 NPE。
 * 这里改成显式静态块：先 BUILDER、再各分节的值、最后 build() 出 SPEC。
 */
public class DamagePhysicsConfig {
   public static final Builder BUILDER;
   public static final ModConfigSpec SPEC;

   // ---- [physics] ----
   public static final DoubleValue GRAVITY;
   public static final DoubleValue AIR_DRAG;
   public static final DoubleValue SPAWN_IMPULSE_BASE;
   public static final DoubleValue CRIT_IMPULSE_MULTIPLIER;
   public static final IntValue LIFETIME_TICKS;
   public static final IntValue MAX_CONCURRENT_NUMBERS;
   public static final BooleanValue ENABLE_COLLISIONS;

   // ---- [visuals] ----
   public static final ConfigValue<String> CRIT_COLOR;
   public static final ConfigValue<String> NORMAL_COLOR;
   public static final ConfigValue<String> HEAL_COLOR;
   public static final DoubleValue MIN_FONT_SCALE;
   public static final DoubleValue MAX_FONT_SCALE;
   public static final DoubleValue SCALE_REFERENCE_DAMAGE;
   public static final BooleanValue ENABLE_IMPACT_SPARKS;
   public static final BooleanValue ENABLE_HEALING_NUMBERS;

   // ---- [distance]（改版新增）----
   public static final BooleanValue ENABLE_DISTANCE_SCALE;
   public static final DoubleValue DISTANCE_SCALE_REFERENCE;
   public static final DoubleValue DISTANCE_SCALE_MIN;
   public static final DoubleValue DISTANCE_SCALE_MAX;

   // ---- [damageScale]（改版新增）----

   // ---- [colors]（改版新增）----
   public static final BooleanValue ENABLE_TYPE_COLORS;
   public static final ConfigValue<String> COLOR_PHYSICAL;
   public static final ConfigValue<String> COLOR_MAGIC;
   public static final ConfigValue<String> COLOR_FIRE;
   public static final ConfigValue<String> COLOR_FROST;
   public static final ConfigValue<String> COLOR_LIGHTNING;
   public static final ConfigValue<String> COLOR_ENCHANTED;
   public static final BooleanValue ENABLE_TYPE_OUTLINE;
   /** 描边是否只取上下左右四邻域（默认 true：8 邻域在放大后会让字形发胖）。 */
   public static final BooleanValue OUTLINE_FOUR_NEIGHBOUR_ONLY;
   /** 每个类型的**描边**颜色（与数字颜色分开配置）。 */
   public static final ConfigValue<String> OUTLINE_COLOR_PHYSICAL;
   public static final ConfigValue<String> OUTLINE_COLOR_MAGIC;
   public static final ConfigValue<String> OUTLINE_COLOR_FIRE;
   public static final ConfigValue<String> OUTLINE_COLOR_FROST;
   public static final ConfigValue<String> OUTLINE_COLOR_LIGHTNING;
   public static final ConfigValue<String> OUTLINE_COLOR_ENCHANTED;
   /** 中毒/凋零这类“不由玩家造成”的减益伤害：固定颜色 + 固定字号，不参与冲击缩放与彩虹。 */
   public static final DoubleValue DEBUFF_FIXED_SCALE;
   /** 基础字号：不再按伤害数值放大，字号只由「对受伤者的影响」决定。 */
   public static final DoubleValue BASE_FONT_SCALE;
   /** 暴击额外倍率（默认 1.0 = 不额外放大，保持“只看影响”的语义）。 */
   public static final DoubleValue CRIT_FONT_SCALE;
   public static final DoubleValue RAINBOW_SPEED;
   public static final BooleanValue ENABLE_OUTLINE;
   public static final IntValue OUTLINE_WIDTH;
   /** 描边颜色压向黑色的比例（0 = 原色，1 = 纯黑），让叠边看起来像一条压深的边而不是第二圈字。 */
   public static final DoubleValue OUTLINE_DARKEN;
   /** 四类伤害族各自的处理策略（先分类，再按族决定底色/描边/是否走冲击缩放）。 */
   public static final BooleanValue FAMILY_DEBUFF_USES_IMPACT;
   public static final BooleanValue FAMILY_SPELL_USES_IMPACT;
   public static final BooleanValue FAMILY_SPELL_USES_TYPE_COLOR;
   public static final BooleanValue FAMILY_SPELL_OUTLINE;
   public static final BooleanValue FAMILY_NORMAL_USES_IMPACT;
   public static final BooleanValue FAMILY_NORMAL_OUTLINE_WHEN_ENCHANTED;
   public static final BooleanValue FAMILY_GUN_USES_IMPACT;
   public static final BooleanValue FAMILY_GUN_OUTLINE;
   /** 投射物（弓/弩/三叉戟）：吃冲击缩放、不吃距离缩放、无描边。 */
   public static final BooleanValue FAMILY_PROJECTILE_USES_IMPACT;
   public static final BooleanValue FAMILY_PROJECTILE_USES_DISTANCE;
   public static final BooleanValue FAMILY_PROJECTILE_OUTLINE;
   /** 冲击比例 → 字号/颜色的曲线指数：<1 让中低比例更快变大变红。 */
   public static final DoubleValue IMPACT_SCALE_GAMMA;
   public static final DoubleValue IMPACT_COLOR_GAMMA;
   /** 减益（中毒/凋零）固定底色的默认白 + 是否描边。 */
   /** 减益分色：中毒=深绿，凋零=深灰。 */
   /** 法术：底色是否取手持物品稀有度颜色（等级色，如传说=金）。 */
   public static final BooleanValue SPELL_USE_RARITY_COLOR;
   /** 法术描边统一浅紫；附魔普攻描边深紫（两者要区分）。 */
   public static final ConfigValue<String> COLOR_RARITY_COMMON;
   public static final ConfigValue<String> COLOR_RARITY_UNCOMMON;
   public static final ConfigValue<String> COLOR_RARITY_RARE;
   public static final ConfigValue<String> COLOR_RARITY_EPIC;
   public static final ConfigValue<String> COLOR_RARITY_OTHER;
   public static final ConfigValue<String> SPELL_OUTLINE_COLOR;
   public static final ConfigValue<String> ENCHANT_OUTLINE_COLOR;
   /** 额外的投射物命名空间（模组弓/弩），作为「弓类」判定。 */
   public static final ConfigValue<java.util.List<? extends String>> PROJECTILE_NAMESPACES;
   public static final BooleanValue DEBUFF_OUTLINE;
   /** 自动判定：DamageType id 关键字 / 无攻击者 / 落回深灰。 */
   public static final BooleanValue DEBUFF_AUTO_DETECT;
   public static final ConfigValue<java.util.List<? extends String>> DEBUFF_TYPE_KEYWORDS;
   public static final ConfigValue<java.util.List<? extends String>> DEBUFF_ENVIRONMENT_IDS;
   public static final BooleanValue DEBUFF_REQUIRE_NO_ATTACKER;
   /** DamageType id 子串 → 颜色，写成 "substring=#RRGGBB"；同一张表既做 debuff 分色也做法术兜底色。 */
   public static final ConfigValue<java.util.List<? extends String>> TYPE_COLOR_MAP;
   /** 法术判定的 DamageType id 关键字（单一日志源）。 */
   public static final ConfigValue<java.util.List<? extends String>> SPELL_TYPE_KEYWORDS;
   /** 打开后每刀在日志里打印 typeId / 分类结果，用于定位识别问题。 */
   public static final BooleanValue DEBUG_CLASSIFICATION;
   public static final ConfigValue<String> DEBUFF_FALLBACK_COLOR;
   /** 各族是否参与距离/深度缩放（枪、投射物类不吃距离缩放，防止远距离全屏小字）。 */
   public static final BooleanValue FAMILY_GUN_USES_DISTANCE;
   public static final BooleanValue FAMILY_SPELL_USES_DISTANCE;
   public static final BooleanValue FAMILY_NORMAL_USES_DISTANCE;
   public static final BooleanValue FAMILY_DEBUFF_USES_DISTANCE;
   /** 恢复生命（仅非玩家）：吃距离与冲击缩放，固定绿色、无描边。 */
   public static final BooleanValue FAMILY_HEAL_USES_IMPACT;
   public static final BooleanValue FAMILY_HEAL_USES_DISTANCE;
   public static final BooleanValue FAMILY_HEAL_OUTLINE;

   // ---- [worldSpace]（改版新增：3D 世界空间渲染）----
   public static final BooleanValue ENABLE_WORLD_SPACE;
   public static final DoubleValue WORLD_SPACE_REFERENCE_DEPTH;
   public static final DoubleValue WORLD_SPACE_SCALE_MIN;
   public static final DoubleValue WORLD_SPACE_SCALE_MAX;

   // ---- [impact]（改版新增：按当前生命百分比驱动大小/颜色/彩虹）----
   public static final BooleanValue ENABLE_IMPACT_SCALING;
   public static final DoubleValue IMPACT_SCALE_MIN;
   public static final DoubleValue IMPACT_SCALE_MAX;
   public static final BooleanValue ENABLE_IMPACT_COLORS;
   public static final ConfigValue<String> IMPACT_COLOR_LOW;
   public static final ConfigValue<String> IMPACT_COLOR_HIGH;
   public static final DoubleValue RAINBOW_MAX_HEALTH_PERCENT;

   static {
      BUILDER = new Builder();

      BUILDER.comment("DamagePhysics Mod Configuration").push("physics");
      GRAVITY = BUILDER.comment("Downward gravity acceleration applied per tick.")
         .defineInRange("gravity", 0.15, 0.0, 2.0);
      AIR_DRAG = BUILDER.comment("Air resistance multiplier applied each tick.")
         .defineInRange("airDrag", 0.98, 0.5, 1.0);
      SPAWN_IMPULSE_BASE = BUILDER.comment("Base upward velocity impulse on spawn.")
         .defineInRange("spawnImpulseBase", 3.0, 0.5, 15.0);
      CRIT_IMPULSE_MULTIPLIER = BUILDER.comment("Impulse velocity multiplier applied to critical hits.")
         .defineInRange("critImpulseMultiplier", 1.5, 1.0, 4.0);
      LIFETIME_TICKS = BUILDER.comment("Lifetime of a damage number in ticks (20 ticks = 1 second).")
         .defineInRange("lifetimeTicks", 40, 10, 200);
      MAX_CONCURRENT_NUMBERS = BUILDER.comment("Maximum simultaneous damage numbers rendered on screen.")
         .defineInRange("maxConcurrentNumbers", 40, 5, 200);
      ENABLE_COLLISIONS = BUILDER.comment("Whether damage numbers collide with each other to prevent unreadable stacking.")
         .define("enableCollisions", true);
      BUILDER.pop();

      BUILDER.push("visuals");
      CRIT_COLOR = BUILDER.comment("Hex color for critical hits (format: #RRGGBB).")
         .define("critColor", "#FFAA00");
      NORMAL_COLOR = BUILDER.comment("Hex color for normal damage hits (format: #RRGGBB).")
         .define("normalColor", "#FFFFFF");
      HEAL_COLOR = BUILDER.comment("Hex color for healing numbers (format: #RRGGBB).")
         .define("healColor", "#55FF55");
      MIN_FONT_SCALE = BUILDER.comment("Minimum text scale factor for small hits.")
         .defineInRange("minFontScale", 0.8, 0.2, 3.0);
      MAX_FONT_SCALE = BUILDER.comment("Maximum text scale factor for massive hits.")
         .defineInRange("maxFontScale", 2.0, 0.5, 5.0);
      SCALE_REFERENCE_DAMAGE = BUILDER.comment("Damage amount that scales up to maximum font size.")
         .defineInRange("scaleReferenceDamage", 20.0, 1.0, 1000.0);
      ENABLE_IMPACT_SPARKS = BUILDER.comment("Whether to render 2D spark particles on critical hits.")
         .define("enableImpactSparks", true);
      ENABLE_HEALING_NUMBERS = BUILDER.comment("Whether to detect and render healing numbers (health increases).")
         .define("enableHealingNumbers", true);
      BUILDER.pop();

      BUILDER.comment("Distance-based scaling of damage numbers (fork feature).").push("distance");
      ENABLE_DISTANCE_SCALE = BUILDER
         .comment("Scale damage numbers by their distance to the camera.",
            "Close hits look bigger, far hits smaller, so numbers feel anchored in the world.")
         .define("enableDistanceScale", true);
      DISTANCE_SCALE_REFERENCE = BUILDER
         .comment("Distance in blocks at which the scale factor is exactly 1.0.")
         .defineInRange("distanceScaleReference", 16.0, 1.0, 128.0);
      DISTANCE_SCALE_MIN = BUILDER
         .comment("Lower clamp of the scale factor (used for far targets).")
         .defineInRange("distanceScaleMin", 0.45, 0.05, 1.0);
      DISTANCE_SCALE_MAX = BUILDER
         .comment("Upper clamp of the scale factor (used for point-blank targets).")
         .defineInRange("distanceScaleMax", 1.5, 1.0, 5.0);
      BUILDER.pop();

      BUILDER.comment("Damage-magnitude scaling of damage numbers (fork feature).").push("damageScale");
      BUILDER.pop();

      BUILDER.comment("Per-damage-type colors, rainbow and outline (fork features).").push("colors");
      ENABLE_TYPE_COLORS = BUILDER
         .comment("Color damage numbers by the damage type instead of the flat normal/crit colors.")
         .define("enableTypeColors", true);
      COLOR_PHYSICAL = BUILDER.comment("Color for plain physical hits.")
         .define("colorPhysical", "#FFFFFF");
      COLOR_MAGIC = BUILDER.comment("Color for magic/spell damage.")
         .define("colorMagic", "#B388FF");
      COLOR_FIRE = BUILDER.comment("Color for fire/lava/burning damage.")
         .define("colorFire", "#FF8844");
      COLOR_FROST = BUILDER.comment("Color for freeze/frost damage.")
         .define("colorFrost", "#88DDFF");
      COLOR_LIGHTNING = BUILDER.comment("Color for lightning damage.")
         .define("colorLightning", "#FFEE66");
      COLOR_ENCHANTED = BUILDER.comment("Color for hits from enchanted weapons.")
         .define("colorEnchanted", "#D9A6FF");
      ENABLE_TYPE_OUTLINE = BUILDER
         .comment("Draw the damage-type color as an outline around the number.",
            "The fill color stays the impact gradient (white to red), so both read at once.")
         .define("enableTypeOutline", true);
      OUTLINE_FOUR_NEIGHBOUR_ONLY = BUILDER
         .comment("Only sample the 4 direct neighbours for the outline.",
            "8-neighbour sampling makes scaled-up glyphs look bloated; 4 keeps the edge tight.")
         .define("outlineFourNeighbourOnly", true);
      OUTLINE_COLOR_PHYSICAL = BUILDER.comment("Outline color for plain physical hits.").define("outlineColorPhysical", "#FFFFFF");
      OUTLINE_COLOR_MAGIC = BUILDER.comment("Outline color for magic damage.").define("outlineColorMagic", "#B388FF");
      OUTLINE_COLOR_FIRE = BUILDER.comment("Outline color for fire damage.").define("outlineColorFire", "#FF8844");
      OUTLINE_COLOR_FROST = BUILDER.comment("Outline color for frost damage.").define("outlineColorFrost", "#88DDFF");
      OUTLINE_COLOR_LIGHTNING = BUILDER.comment("Outline color for lightning damage.").define("outlineColorLightning", "#FFEE66");
      OUTLINE_COLOR_ENCHANTED = BUILDER.comment("Outline color for enchanted-weapon hits (user asked for white).").define("outlineColorEnchanted", "#FFFFFF");
      DEBUFF_FIXED_SCALE = BUILDER
         .comment("Scale factor used for poison/wither numbers when debuffFixedStyle is on.")
         .defineInRange("debuffFixedScale", 1.0, 0.1, 5.0);
      BASE_FONT_SCALE = BUILDER
         .comment("Base font scale for every number.",
            "Damage magnitude no longer changes the size: size comes only from the impact ratio (damage / current health).")
         .defineInRange("baseFontScale", 1.0, 0.1, 5.0);
      CRIT_FONT_SCALE = BUILDER
         .comment("Extra multiplier for critical hits (1.0 keeps size purely impact-driven).")
         .defineInRange("critFontScale", 1.5, 0.5, 3.0);
      RAINBOW_SPEED = BUILDER
         .comment("Rainbow cycle speed in full hue turns per second.")
         .defineInRange("rainbowSpeed", 0.6, 0.05, 10.0);
      ENABLE_OUTLINE = BUILDER
         .comment("Draw an outline around numbers (used for enchanted hits).")
         .define("enableOutline", true);
      OUTLINE_WIDTH = BUILDER
         .comment("Outline thickness in GUI pixels (1 = the 4 direct neighbours only).")
         .defineInRange("outlineWidth", 1, 1, 3);
      OUTLINE_DARKEN = BUILDER
         .comment("How much the outline color is darkened toward black (0 = pure type color, 1 = black).",
            "A darkened edge reads as a real stroke instead of a second copy of the glyphs.")
         .defineInRange("outlineDarken", 0.45, 0.0, 0.9);

      BUILDER.comment("Per-family handling: classify first, then decide fill/outline/scaling (fork feature).").push("family");
      FAMILY_DEBUFF_USES_IMPACT = BUILDER
         .comment("Poison/wither: no impact scaling (fixed color and fixed size).")
         .define("familyDebuffUsesImpact", false);
      FAMILY_SPELL_USES_IMPACT = BUILDER
         .comment("Spells: scale by impact ratio.")
         .define("familySpellUsesImpact", true);
      FAMILY_SPELL_USES_TYPE_COLOR = BUILDER
         .comment("Spells: fill uses the spell color instead of the white-to-red impact gradient.")
         .define("familySpellUsesTypeColor", true);
      FAMILY_SPELL_OUTLINE = BUILDER
         .comment("Spells: draw the spell-colored outline.")
         .define("familySpellOutline", true);
      FAMILY_NORMAL_USES_IMPACT = BUILDER
         .comment("Normal attacks: scale by impact ratio.")
         .define("familyNormalUsesImpact", true);
      FAMILY_NORMAL_OUTLINE_WHEN_ENCHANTED = BUILDER
         .comment("Normal attacks: outline only when the weapon is enchanted.")
         .define("familyNormalOutlineWhenEnchanted", true);
      FAMILY_GUN_USES_IMPACT = BUILDER
         .comment("Guns: no impact scaling (fixed size).")
         .define("familyGunUsesImpact", false);
      FAMILY_GUN_OUTLINE = BUILDER
         .comment("Guns: no outline.")
         .define("familyGunOutline", false);
      FAMILY_PROJECTILE_USES_IMPACT = BUILDER
         .comment("Bows/crossbows/tridents: scale by impact ratio.")
         .define("familyProjectileUsesImpact", true);
      FAMILY_PROJECTILE_USES_DISTANCE = BUILDER
         .comment("Bows/crossbows/tridents: apply distance/depth scaling (default off).")
         .define("familyProjectileUsesDistance", false);
      FAMILY_PROJECTILE_OUTLINE = BUILDER
         .comment("Bows/crossbows/tridents: outline (default off).")
         .define("familyProjectileOutline", false);
      IMPACT_SCALE_GAMMA = BUILDER
         .comment("Exponent applied to the impact ratio before scaling. Below 1 grows faster early on.")
         .defineInRange("impactScaleGamma", 0.70, 0.2, 3.0);
      IMPACT_COLOR_GAMMA = BUILDER
         .comment("Exponent applied to the impact ratio before color blending. Below 1 turns red earlier.")
         .defineInRange("impactColorGamma", 0.55, 0.2, 3.0);
      SPELL_USE_RARITY_COLOR = BUILDER
         .comment("Spells: fill color follows the held item's rarity color (common=white, rare=blue, epic=purple, legendary=gold).")
         .define("spellUseRarityColor", true);
      COLOR_RARITY_COMMON = BUILDER.comment("Spell fill color for COMMON rarity.").define("rarityColorCommon", "#FFFFFF");
      COLOR_RARITY_UNCOMMON = BUILDER.comment("Spell fill color for UNCOMMON rarity.").define("rarityColorUncommon", "#55FF55");
      COLOR_RARITY_RARE = BUILDER.comment("Spell fill color for RARE rarity.").define("rarityColorRare", "#55FFFF");
      COLOR_RARITY_EPIC = BUILDER.comment("Spell fill color for EPIC rarity.").define("rarityColorEpic", "#FF55FF");
      COLOR_RARITY_OTHER = BUILDER
         .comment("Spell fill color for any other (modded) rarity, e.g. legendary gold.")
         .define("rarityColorOther", "#FFAA00");
      SPELL_OUTLINE_COLOR = BUILDER
         .comment("Spells: unified light-purple outline color.")
         .define("spellOutlineColor", "#D8B4FE");
      ENCHANT_OUTLINE_COLOR = BUILDER
         .comment("Normal attacks with enchanted weapon: dark-purple outline (distinct from spell light purple).")
         .define("enchantOutlineColor", "#6B21A8");
      PROJECTILE_NAMESPACES = BUILDER
         .comment("Extra mod namespaces whose items count as bows/projectiles.")
         .defineList("projectileNamespaces", java.util.List.of("minecraft"), e -> e instanceof String);
      DEBUFF_OUTLINE = BUILDER
         .comment("Whether debuff numbers get an outline (default off).")
         .define("debuffOutline", false);
      DEBUFF_AUTO_DETECT = BUILDER
         .comment("Auto-detect debuff/DoT damage from the DamageType id and attacker presence.",
            "Works on the integrated server (single player); on a remote server the client never receives damage events.")
         .define("debuffAutoDetect", true);
      DEBUFF_TYPE_KEYWORDS = BUILDER
         .comment("DamageType id substrings treated as debuff/DoT damage.")
         .defineList("debuffTypeKeywords", java.util.List.of(
            "poison", "wither", "bleed", "bleeding", "decay", "corrupt", "curse",
            "acid", "venom", "plague", "rot", "pest", "dot", "frostbite", "soul_drain"), e -> e instanceof String);
      DEBUFF_ENVIRONMENT_IDS = BUILDER
         .comment("Damage ids that are NOT debuffs even without an attacker (fall, drowning, lava...).")
         .defineList("debuffEnvironmentIds", java.util.List.of(
            "fall", "drown", "in_wall", "out_of_world", "cramming", "fly_into_wall",
            "hot_floor", "on_fire", "lava", "campfire", "sweet_berry_bush", "stalagmite",
            "freeze", "starve", "explosion", "explosion.player", "thorns", "falling_block",
            "lightning_bolt", "in_fire", "cactus", "outside_border", "generic", "magic",
            "indirect_magic"), e -> e instanceof String);
      DEBUFF_REQUIRE_NO_ATTACKER = BUILDER
         .comment("Also treat attacker-less, indirect-less damage as a debuff (disabled by default: too many false positives).")
         .define("debuffRequireNoAttacker", false);
      SPELL_TYPE_KEYWORDS = BUILDER
         .comment("DamageType id substrings treated as spell damage.")
         .defineList("spellTypeKeywords", java.util.List.of(
            "spell", "magic", "arcane", "blood", "irons_spellbooks"), e -> e instanceof String);
      DEBUG_CLASSIFICATION = BUILDER
         .comment("Log the DamageType id and the resolved family for every damage number.")
         .define("debugClassification", false);
      TYPE_COLOR_MAP = BUILDER
         .comment("DamageType id substring to fill color, entries like \"poison=#2E7D32\".",
            "Used for debuff coloring and as the spell fill fallback when the held item rarity is unknown.")
         .defineList("typeColorMap", java.util.List.of(
            "poison=#2E7D32", "wither=#3A3A3A", "bleed=#B3131B", "decay=#6B7A3A",
            "corrupt=#6A3FA0", "curse=#5A3E75", "acid=#9ACD32", "venom=#4CBB17",
            "plague=#7A8B3C", "rot=#5C4033", "frostbite=#88DDFF",
            "fire=#FF8844", "lava=#FF6622", "freeze=#88DDFF", "cold=#88DDFF",
            "lightning=#FFEE66", "magic=#B388FF", "spell=#B388FF", "arcane=#B388FF",
            "holy=#FFF2A8", "shadow=#4B0082", "soul=#7B68EE",
            "blood_magic=#B3131B", "fire_magic=#FF8844", "ice_magic=#88DDFF",
            "lightning_magic=#FFEE66", "holy_magic=#FFF2A8", "eldritch_magic=#6A3FA0",
            "ender_magic=#7B4BC8", "evocation_magic=#B388FF", "nature_magic=#7AC943"), e -> e instanceof String);
      DEBUFF_FALLBACK_COLOR = BUILDER
         .comment("Fallback fill color for any debuff that has no dedicated color.")
         .define("debuffFallbackColor", "#3A3A3A");
      FAMILY_GUN_USES_DISTANCE = BUILDER
         .comment("Guns: apply distance/depth scaling (default off, keeps far shots readable).")
         .define("familyGunUsesDistance", false);
      FAMILY_SPELL_USES_DISTANCE = BUILDER
         .comment("Spells: apply distance/depth scaling.")
         .define("familySpellUsesDistance", true);
      FAMILY_NORMAL_USES_DISTANCE = BUILDER
         .comment("Normal attacks: apply distance/depth scaling.")
         .define("familyNormalUsesDistance", true);
      FAMILY_HEAL_USES_IMPACT = BUILDER
         .comment("Healing numbers: scale by impact ratio (heal amount / health before).")
         .define("familyHealUsesImpact", true);
      FAMILY_HEAL_USES_DISTANCE = BUILDER
         .comment("Healing numbers: apply distance/depth scaling.")
         .define("familyHealUsesDistance", true);
      FAMILY_HEAL_OUTLINE = BUILDER
         .comment("Healing numbers: outline (default off).")
         .define("familyHealOutline", false);
      FAMILY_DEBUFF_USES_DISTANCE = BUILDER
         .comment("Debuffs: apply distance/depth scaling (default off: fixed size).")
         .define("familyDebuffUsesDistance", false);
      BUILDER.pop();
      BUILDER.pop();

      BUILDER.comment("World-space (3D) rendering of damage numbers (fork feature).").push("worldSpace");
      ENABLE_WORLD_SPACE = BUILDER
         .comment("Anchor damage numbers to the hit position in the world and re-project them every frame,",
            "so they shrink with distance and stay glued to the target instead of floating on a flat screen plane.")
         .define("enableWorldSpace", true);
      WORLD_SPACE_REFERENCE_DEPTH = BUILDER
         .comment("Camera-space depth (blocks) at which the world-space scale factor is exactly 1.0.")
         .defineInRange("worldSpaceReferenceDepth", 8.0, 1.0, 128.0);
      WORLD_SPACE_SCALE_MIN = BUILDER
         .comment("Lower clamp of the world-space scale factor (far numbers).")
         .defineInRange("worldSpaceScaleMin", 0.15, 0.02, 1.0);
      WORLD_SPACE_SCALE_MAX = BUILDER
         .comment("Upper clamp of the world-space scale factor (point-blank numbers).")
         .defineInRange("worldSpaceScaleMax", 2.20, 1.0, 6.0);
      BUILDER.pop();

      BUILDER.comment("Impact scaling: size and color driven by damage as a share of the victim's CURRENT health.").push("impact");
      ENABLE_IMPACT_SCALING = BUILDER
         .comment("Scale the number by damage / current health before the hit.",
            "A tiny chip off a full-health mob stays small; the same hit on a nearly dead mob grows and turns red.")
         .define("enableImpactScaling", true);
      IMPACT_SCALE_MIN = BUILDER
         .comment("Scale factor at impact ratio 0 (barely any damage relative to remaining health).")
         .defineInRange("impactScaleMin", 0.35, 0.05, 3.0);
      IMPACT_SCALE_MAX = BUILDER
         .comment("Scale factor at impact ratio 1 (the hit removes all remaining health).")
         .defineInRange("impactScaleMax", 3.60, 0.5, 8.0);
      ENABLE_IMPACT_COLORS = BUILDER
         .comment("Blend the number color from impactColorLow to impactColorHigh as the impact ratio grows.")
         .define("enableImpactColors", true);
      IMPACT_COLOR_LOW = BUILDER
         .comment("Color at impact ratio 0.")
         .define("impactColorLow", "#FFFFFF");
      IMPACT_COLOR_HIGH = BUILDER
         .comment("Color at impact ratio 1.")
         .define("impactColorHigh", "#FF2020");
      RAINBOW_MAX_HEALTH_PERCENT = BUILDER
         .comment("A single hit that removes this share of the victim's MAX health turns the number rainbow.",
            "0.20 = 20% of max health. Set to 0 to disable.")
         .defineInRange("rainbowMaxHealthPercent", 0.20, 0.0, 1.0);
      BUILDER.pop();

      SPEC = BUILDER.build();
   }
}
