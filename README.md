# MC开发用的各种妙妙小工具

Minecraft 1.21.1 / NeoForge 模组源码合集。本目录是**本地总仓库**：MC 模组都在这里混放（大杂烩），上传时只把 MC 部分作为「MC开发用的各种妙妙小工具」这一个仓库推送。

- 目标环境：Minecraft 1.21.1 · NeoForge 21.1.234 · JDK 21
- 每个模组独立构建，互不依赖

> 本目录里还放着几个**非 MC 项目**（`ned/`、`orca-character/`、`drive-fairy/`）和三个**通用工具**（`cecil-patcher/`、`live2d-moc3/`、`asset-triage/`）。
> 它们各自是独立 git 仓库、单独推送，已被本仓库的 `.gitignore` 排除，不属于 MC 合集。

## 收录清单（MC 部分）

| 目录 | 模组名 | 版本 | 说明 |
|---|---|---|---|
| `vitalxp-source/` | 生命力经验 | 1.21.1-1.0.0 | 怪物经验掉落随其最大生命值动态调整，倍率可配置，支持黑白名单 |
| `flatxp-source/` | 平坦经验 | 1.21.1-1.0.0 | 所有等级升级所需经验固定为 7 点，不再递增 |
| `vitalslayer-source/` | 百分比杀伤 | 1.21.1-1.0.0 | 新增「百分比杀伤」附魔：额外造成目标最大生命值 2.5% × 等级伤害 |
| `experiencecopy-source/` | 经验复制 | 1.21.1-1.0.0 | 通过铁砧与附魔之瓶复制工具武器，消耗经验逐步恢复耐久上限 |
| `swiftstrike-source/` | 迅捷打击 | 1.21.1-1.0.0 | 按玩家当前移动速度造成额外伤害，骑马与药水效果显著 |
| `equipmentfusion-src/` | Equipment Fusion | 1.0.0 | 装备融合（工程名沿用模板 `TongDaWayMod`） |
| `DamagePhysics-fork/` | YG & Little Code Sauce · DamagePhysics Fork | 0.6.2-fork18 | 基于 DamagePhysics 0.6.2（TheKryzzz_，MIT）的客户端改版。**目录 README 内有 fork1~fork18 版本清单** |
| `compatpatch/` | Compat Patch 兼容补丁 | — | 独立兼容补丁模组：mixin 修 7 项整合包崩溃/冲突（空 ItemStack / VSS / EMI / Jupiter / Curios / Wakes / Magnet 删除），不改原 jar，每项一个开关 |

## 构建

Gradle 工程各自独立执行，需 JDK 21：

```bash
cd <项目目录>
./gradlew build          # Windows: gradlew.bat build
```

- `DamagePhysics-fork`：反编译改版工程，只跑 `compileJava`，依赖 `libs/original.jar`（需自备上游 0.6.2 的原版 jar），详见其 README。
- `compatpatch`：不是 Gradle 工程，用 PowerShell 脚本构建部署（`build.ps1` / `deploy-compatpatch.ps1`）。

> 仓库只收源码：成品 jar、`build/`、`.gradle/` 均由 `.gitignore` 排除（`gradle/wrapper/gradle-wrapper.jar` 例外，构建必需）。

## 非 MC 项目（独立仓库）

| 目录 | 项目 | 引擎 |
|---|---|---|
| `ned/` | 还不够难（Not Enough Difficulty） | 杀戮尖塔 2 · Godot 4.5.1 + BaseLib |
| `orca-character/` | 银龙奥卡 | 杀戮尖塔 2 · Godot |
| `drive-fairy/` | 驱动妖精 3D 空战 | Unity 2022.3 + BepInEx 6 |

## 通用工具（未上传）

`cecil-patcher/`（Unity IL 补丁）、`live2d-moc3/`（moc3 v6 解析）、`asset-triage/`（资源筛选台账）。

## 许可

- `DamagePhysics-fork` 上游 MIT（TheKryzzz_），改版同按 MIT，保留署名。
- 其余模组标注 MIT，作者见各工程 `gradle.properties`。
