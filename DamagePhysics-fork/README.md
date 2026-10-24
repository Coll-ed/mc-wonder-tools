# YG & Little Code Sauce · DamagePhysics Fork

由 **YG** 与 **Little Code Sauce** 一起做的伤害数字改版。
上游是 **DamagePhysics 0.6.2**（作者 TheKryzzz_，MIT），本目录只包含**被修改/新增的类**，其余类在编译时直接引用 `libs/original.jar`。

> 上游 MIT：保留原作者署名（`mods.toml` 里 `authors` 未改），改版代码同样按 MIT 处理。

## 目录

| 路径 | 内容 |
|---|---|
| `src/` | 改版源码（7 个类 + 3 个 fork 辅助类） |
| `original-decompiled/` | Vineflower 1.10.1 反编译出的上游全部源码（含资源，仅参照） |
| `libs/original.jar` | 上游 0.6.2 原版 jar（编译依赖 + 打包基底） |
| `build.gradle` / `settings.gradle` | MDG 编译工程（只跑 `compileJava`） |
| `DamagePhysics-NeoForge-1.21.1-0.6.2-fork14.jar` | 当前成品（185 KB） |

## 编译 / 打包

```bash
JAVA_HOME=<jdk21> GRADLE_USER_HOME=<缓存> gradle -p . compileJava --no-configuration-cache
cp libs/original.jar out.jar
(cd build/classes/java/main && jar --update --file ../../../../out.jar $(find . -name '*.class' | sed 's|^\./||'))
```

⚠️ 必须把**编译产出的全部 class**（含 `DamageEventListener$1.class` 这类 `switch` 合成类）一起打入，
漏一个就会 `NoClassDefFoundError`（fork9 踩过这个坑）。

## 六族策略（先分类，再决定底色/描边/缩放）

| 族 | 判定（客户端可用线索） | 底色 | 描边 | 冲击缩放 | 距离缩放 |
|---|---|---|---|---|---|
| **DEBUFF** | 受伤者带 `POISON` → 深绿 `#2E7D32`；带 `WITHER` → 深灰 `#3A3A3A` | 固定 | 无 | ✗ | ✗ |
| **GUN** | 手持物品命名空间 `scguns` | 白→红（冲击） | 无 | ✗ | ✗ |
| **PROJECTILE** | `ProjectileWeaponItem` / `TridentItem` / `projectileNamespaces` 配置里的命名空间 | 白→红（冲击） | 无 | ✓ | ✗ |
| **SPELL** | 命名空间 `irons_spellbooks` | **稀有度色**（common 白 / uncommon 绿 / rare 青 / epic 粉 / 其它=金） | **浅紫 `#D8B4FE`** | ✓ | ✓ |
| **HEAL** | 生命上升，且**非玩家** | 固定绿 `#55FF55` | 无 | ✓ | ✓ |
| **NORMAL** | 其余 | 白→红（冲击） | 附魔时**深紫 `#6B21A8`** | ✓ | ✓ |

玩家**自身**的伤害与治疗数字不显示（`scanEntityHealth` 里刻意跳过本地玩家）。

## 冲击比例（字号与“生命变色”的共同来源）

```
impact = clamp(伤害 / 命中前生命, 0..1)      # 击杀那一下按 1.0
字号   = baseFontScale × (impact ^ impactScaleGamma) × 深度缩放 × 暴击倍率
颜色   = lerp(impactColorLow → impactColorHigh, impact ^ impactColorGamma)
彩虹   = 单刀 ≥ 对方最大生命 × rainbowMaxHealthPercent
```

## 3D 世界锚定

数字锚在命中点（`body.worldX/Y/Z`），每帧重新投影：

```
drawX = 投影(锚点).x + (物理插值位置.x − 出生屏幕坐标.x)
scale *= clamp(worldSpaceReferenceDepth / 深度, min, max)
```
因此数字会随相机透视缩放、跟着目标走，同时保留原来的弹跳/重力手感。

## 热重载

所有开关都是 NeoForge **client 配置**（`config/damagephysics-client.toml`），读取点全部是调用时 `.get()`：
改文件保存即可生效（`ModConfigEvent.Reloading`），**不需要重启**。

## 版本清单（自用迭代，未对外发布；逐版演进无 git，故合集只保留最终源码）

| 版本 | 相对上游的改动点 | 可构建 |
|---|---|---|
| fork1 | 距离缩放（倒数曲线）+ 伤害绝对值三段式缩放 | 源码未留 |
| fork2 | 按伤害类型取色 / 附魔描边 / 动态彩虹（绝对伤害阈值） | 源码未留 |
| fork3 | 分类改客户端判据（远端服收不到 `LivingDamageEvent`） | 源码未留 |
| fork4 | 3D 世界锚定（贴命中点、按深度缩放） | 源码未留 |
| fork5 | 冲击比例驱动字号与白→红；彩虹按最大生命百分比 | 源码未留 |
| fork6 | 主色/描边分离 | 源码未留 |
| fork7 | 描边改 4 邻域 + 压暗；减益独立分支 | 源码未留 |
| fork8 | 去掉按伤害数值放大；减益绝对优先 | 源码未留 |
| fork9 | 四族雏形（DEBUFF/SPELL/NORMAL/GUN） | ❌ 漏打合成类崩溃 |
| fork10 | 修复：打包全部合成类 | 源码未留 |
| fork11 | 冲击 gamma、暴击 1.5×、族级距离开关 | 源码未留 |
| fork12 | 玩家自身不显示；新增 HEAL 族；删死代码 | 源码未留 |
| fork13 | 新增 PROJECTILE 族（弓/弩/三叉戟 + 模组命名空间） | 源码未留 |
| fork14 | 减益分色、法术稀有度底色、浅紫/深紫双描边、品牌化 | 源码未留 |
| fork15 | debuff 自动判定（DamageType 关键字 / 无攻击者） | 源码未留 |
| fork16 | `typeColorMap` 类型色表；法术按 typeId 识别 | 源码未留 |
| fork17 | 补全铁魔法 9 个学派伤害类型颜色 | 约等于当前 |
| **fork18** | 中文介绍；`spellTypeKeywords` 配置表；`debugClassification` 日志；私有 LOGGER 修复 | ✅ 本目录即此版 |

## 已知边界

- 客户端拿不到 `DamageSource`（`LivingDamageEvent` 是服务端事件），分类靠「受伤者状态 + 本地玩家手持物品」的间接线索；
  箭矢/投掷物由手持物品判断，别人射出的箭无法区分。
- `drawString` 只能整字平移，描边是「同字 4 邻域偏移 + 压暗（`outlineDarken`）」的近似；
  真正的字形轮廓描边需要自建 RenderTarget + shader。
- `Rarity.color` 在 1.21.1 是 `private`，所以稀有度颜色走本模组自己的配置映射（顺带覆盖模组自定义稀有度）。
