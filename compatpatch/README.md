# Compat Patch —— 独立兼容补丁模组

**不改任何原模组 jar**。全部修复用 mixin 挂到目标模组的类上，每项一个开关。

- 目标类用 `@Mixin(targets = "字符串类名")` 引用 → **不对被修模组产生任何编译期依赖**
- 每个目标模组一个独立 mixin 配置（`required: false`）→ 目标模组不在时只记警告，不会让游戏起不来
- 每项修复一个开关（`config/compatpatch.properties`）→ 关掉即退回原模组行为，不必卸载模组
- 回滚 = `.\deploy-compatpatch.ps1 rollback`（移除这一个 jar 即可，原 jar 从未被改动）

来源日志：`logs/latest.log`（对应 `minecraft-exported-logs-2026-10-10T20-57-05.log` 那个会话）
日志规模：**486 行 ERROR/FATAL**，按签名归并后定出 5 项「反复报错」。

---

## 修复进度

| # | 次数 | 问题 | 状态 |
|---|---|---|---|
| ① | 1（**崩服**） | `io.netty EncoderException: Empty ItemStack not allowed` | ✅ 已修，已部署 |
| ④ | 26 | Jupiter `Cannot load key=... Double` + NPE | ✅ 已修，已部署 |
| ③ | 15 | EMI `Exception thrown registering repair recipes` | ✅ 已修，已部署 |
| ⑤ | 8 | Curios `X is not a registered slot type!` | ✅ 已修（纯数据文件），已部署 |
| ② | **508** | VSS `IllegalArgumentException: Duplicate section 0` | ✅ 已修，已部署 |
| ⑥ | 持续 | Wakes「破浪」面片在水消失后永久残留 | ✅ 已修，已部署 |
| ⑦ | 持续 | NeoEnchant 的 Magnet 附魔（会把刚生成的物品抢走 → 相机收女仆时女仆消失） | ✅ 已**删除**，已部署 |

---

## ① scguns 投掷物生成数据的空 ItemStack 崩服

### 崩溃链（日志 L12297、L12459~L12485）
```
scguns ThrowableChokeBombEntity 命中玩家 → Player.hurt
  → gytrinket DamageManager.onLivingAttack → ShieldHandler.handle
  → ReflectShieldType.processReflectAfterShieldDamage → reflectProjectile
  → EntityType.create(level) 新建同类型投掷物（不复制 item，item 保持 ItemStack.EMPTY）
  → ServerLevel.addFreshEntity → ServerEntity.sendPairingData
  → AdvancedAddEntityPayload → ThrowableItemEntity.writeSpawnData:171
  → ItemStack$2.encode → EncoderException: Empty ItemStack not allowed → 崩服
```

### 根因（读字节码确认）
- `ThrowableItemEntity` 两个构造器都写 `this.item = ItemStack.EMPTY`；`getItem()` 也不保证非空
  → **item 字段本来就允许为空**
- 但 `writeSpawnData` / `readSpawnData` 用的是 `ItemStack.STREAM_CODEC`
- 而 `ItemStack.STREAM_CODEC` 就是 `ItemStack$2`：**把 encode/decode 全部委托给 `OPTIONAL_STREAM_CODEC`，只在外面多一层非空断言**
  （encode 抛 `EncoderException`、decode 抛 `DecoderException`）

### 修法
把这两个方法里的字段换成 `OPTIONAL_STREAM_CODEC`（`@Redirect` 掉那两处 `GETSTATIC`）。

**为什么零风险**：两者**线格式完全相同**（非空版只是外层校验包装），所以协议不变、
两端混用也不会错位；改的只是「写入方接受自己字段本来就可能取到的值」。
`readSpawnData` 也一起换，保持读写一致。

---

## ④ Jupiter 配置项值为 null 时的 NPE

### 现象（日志 L3430~L3451，13 个键 × 客户端/服务端各一次 = 26 行）
```
[Jupiter/ERROR] Cannot load key=Music Volume (default: 1.0), type=java.lang.Double in config=witherreincarnated:CLIENT
java.lang.NullPointerException: Cannot invoke "Object.getClass()" because "value" is null
  at NightConfigHolder.processEntry(NightConfigHolder.java:157)
```

### 根因
`processEntry` 开头就是 `type.isAssignableFrom(value.getClass()) && ...`，而 `value` 是**配置文件里的实际值**。
同一份日志更早处给出了根由：

```
[ModConfigSpec/CORE]: Configuration ... is not correct. Correcting
[ModConfigSpec/CORE]: Incorrect key Music Volume (default: 1 was corrected from ... to its default, null
```

即 **NeoForge 认为这些键类型不对，把它们「校正」成了 null**，Jupiter 拿到 null 后直接 `getClass()` → NPE。

### 修法
在 `processEntry` 开头注入：值为 null 时直接 `cancel()`。

**为什么零语义变更**：原实现里「条件不成立」的分支（字节码偏移 70）本来就是
**什么都不做、直接返回**（既不建 builder 也不 set 引用）。
所以「值为 null 时跳过」≡ 作者本意，只是把「抛 NPE」换成「按类型不符处理」。

---

## ② VSS `Duplicate section 0`（已修）

```
[VSS-ColumnProcessor/ERROR] [VSS/]: Failed to process voxel column transfer N at X,Z
java.lang.IllegalArgumentException: Duplicate section 0
  at ClientColumnProcessor.decodeColumn(ClientColumnProcessor.java:208)
```

### 现象与量级
本文档写"296 次"是按原始导出日志统计；**复核更晚的 `logs/latest.log` 时是 508 次**，且
**508/508 全部**堆栈指向 `ClientColumnProcessor.decodeColumn`，服务端 0 条：

| 统计项 | 次数 |
|---|---|
| `IllegalArgumentException: Duplicate section` | 508 |
| ↳ 堆栈指向客户端 `decodeColumn` | **508** |
| ↳ 堆栈指向服务端 `LoadedColumnData` | **0** |
| `Section Y manifest is not strictly ordered`（服务端） | **0** |
| `Section length manifest does not match section Y manifest`（服务端） | **0** |
| VSS 的失败形态种类 | **只有这 1 种** |

### 那个抛点是自建断言，代价是丢整根柱子
```java
byte sectionY = buf.readByte();
if (!seen.add((int) sectionY)) {
    throw new IllegalArgumentException("Duplicate section " + sectionY);   // ← 就是第 208 行
}
```
抛出后**整根柱子被丢弃**（远景点缺失），不是崩溃。

### 根因定位：重复出现在「上线之后」，服务端数据本身是干净的

逐个环节读字节码核实，**每一环都会拒绝重复**：

| 环节 | 拒绝方式 | 日志里的证据 |
|---|---|---|
| `LoadedColumnData` | `sortSectionManifest` **先排序再查相邻重复**（排序让任何重复变相邻）→ 抛 | 服务端 0 条堆栈 |
| `EncodedColumnData` | `validateSectionManifestOrder` 要求**严格递增** | 0 次 "strictly ordered" |
| `VoxelColumnS2CPayload` | `sanitizeReplacementSections` **先 `Arrays.sort` 再查相邻重复** → 抛 `Duplicate replacement section` | 0 次该文案 |
| `ColumnPayloadSplitter` | 分包是**正确划分**（每个 section 只进一组），`readSections`↔`writeGroup` 往返自洽 | — |

**结论**：清单（`replacementSectionYs`）与 section Y 列表都**必然唯一**，
所以重复是在**存储 → 线路 → 客户端多段拼装**之间才出现的 ——
服务端自己没有任何察觉（这正是它没报错的原因）。

### 修法：只容错那一处断言（不掩盖上游）

```java
@Redirect(method = "decodeColumn",
          at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"),
          require = 0)
private boolean compatpatch$tolerateDuplicateSectionY(Set<Object> seen, Object value) {
    boolean firstTime = seen.add(value);          // 仍然真的调用 → seen 保持去重语义
    if (!firstTime) DuplicateSectionLog.record(...);   // 显式记录（按次数节流）
    return true;                                  // 永远视为"首次见到" → 不再抛
}
```
重复的那一段按**后到者覆盖**生效（两个候选属于同一根柱子、同一个 Y，都是该位置的合法数据）。

### 为什么这**一处**就够了（不是"把异常往后挪"）

后面还有两处校验，逐条论证它们在容错后**必然成立**：

1. `manifest.length == seen.size()`：
   - 清单**必然唯一**（上表第 3 行：payload 构造时排序查重，有重复会抛）
   - `seen` 是 `HashSet`，`size()` = **去重计数**
   - 两者都等于"不同 Y 的个数" ⇒ 相等 ✓
2. `for (y : manifest) seen.contains(y)`：清单里每个 Y 都在 body 里读到过 ⇒ 成立 ✓
3. 下游 `VoxelColumnData` 的 **4 个构造函数都不做唯一性校验**（已逐字节码确认）
   ⇒ 重复 Y 在数据结构上被接受，不会在更下游炸 ✓

### 为什么不改服务端（原本的判断被证据推翻）

初版判断是"在清单产生处去重"，理由是"客户端还有第二处清单校验会接着抛"。
复核后**推翻**了：
- 清单根本不可能有重复 —— 三个环节各自都会拒绝（上表），日志也证明它们从未触发；
- 因此"在清单产生处去重"没有可修的对象；真正会因重复而抛的**只有 `decodeColumn` 这一处**；
- 而第二处校验在清单唯一的前提下**天然成立**（上面第 1、2 条）。

### 复现与验证

- 修完后 VSS 的失败形态应当从 508 次降到 0；日志里会出现（节流的）
  `VSS 柱子里有重复的 section Y=…：已容错继续解码` WARN，这正是"没静默吞掉"的凭据。
- 想恢复原行为：`config/compatpatch.properties` 里 `fixVssDuplicateSection=false`（**那处断言立刻恢复**）。

## ⑤ Curios 8 个未注册槽位（已修，纯数据文件）

```
[Curios API/ERROR] (×8): aether_accessory / aether_cape / aether_gloves / aether_pendant /
                         aether_ring / aether_shield / scroll / feet is not a registered slot type!
```

### 根因
- 报错来自 `CuriosEntityManager`，它读 `data/<ns>/curios/entities/*.json` 里的 `slots` 数组
- 引用方是 **`touhoulittlemaid`**：`entities/aether.json`（6 个 aether 槽）与 `entities/curios.json`（`scroll`/`feet`）
- 而那些槽位类型**从来没有被定义过** —— Aether 1.5.10 的 jar 里没有任何 curios 数据
- **Curios 最终按 `ResourceLocation.getPath()`（即文件名）建键**（读 `CuriosSlotManager.apply` 得出：
  它 `map.put(path, ...)`，并跳过 `curios` 命名空间与 `_` 开头的路径），
  所以实体文件里的裸名与 `data/<任意ns>/curios/slots/<同名>.json` 逐字对应

### 修法
在补丁模组里提供 8 个槽位定义（**纯数据文件，不需要 mixin**）：
`data/compatpatch/curios/slots/{aether_accessory,aether_cape,aether_gloves,aether_pendant,aether_ring,aether_shield,scroll,feet}.json`

字段按 Curios 的 `fromJson`（全部可选）填：`order` + `icon`（复用 Curios 自带贴图，避免缺贴图）+ `validators: ["curios:tag"]`，
与 Curios 自带槽位定义写法一致。

> 注意：键只用文件名，命名空间会被忽略。用 `compatpatch` 命名空间是为了明示来源，
> 也避免冒充 Aether 的数据。若将来 Aether 自己补了这些定义，两边同名会有一条被覆盖（`replace` 默认 false）。

### 完备性证明
`tools/verify-curios-slots.ps1` 扫描全实例并复现结论：

```
已定义槽位 13 个；未定义但被引用（条件满足）8 个
  aether_accessory <- touhoulittlemaid:entities/aether
  ... (aether_cape/gloves/pendant/ring/shield 同)
  feet             <- touhoulittlemaid:entities/curios
  scroll           <- touhoulittlemaid:entities/curios
[PASS] 恰好等于日志报的 8 个 —— 补丁覆盖完整
```

**这里有个必须踩对的坑**：entity 文件带 `neoforge:conditions` 的 `mod_loaded` 守卫 ——
对应模组没装就整文件跳过、不会报错。**不过滤条件会得到 58 个**"缺失"名字（绝大多数是因为模组没装），
过滤后才是 8 个。另外"已安装 modId"只能取 `[[mods]]` 段，
把 `[[dependencies.*]]` 里声明的依赖也算进来会多出一个 `an_focus`（Ars Nouveau 其实没装）。


## ③ ice_and_fire_spellbooks 字段类型不匹配（已修）

```
java.lang.NoSuchFieldError: Class com.iafenvoy.iceandfire.registry.IafItems does not have member field
  'dev.architectury.registry.registries.RegistrySupplier DRAGONSTEEL_FIRE_INGOT'
  at ice_and_fire_spellbooks.registries.ArmorMaterialRegistries.lambda$static$0(ArmorMaterialRegistries.java:35)
  at emi.VanillaPlugin.addRepair(VanillaPlugin.java:497)
```

### 根因：字段名对、**类型不对**
`NoSuchFieldError` 的报文里带着**期望描述符**，所以 JVM 是按「名字 + 描述符」解析失败的。

| | `IafItems.DRAGONSTEEL_FIRE_INGOT` 的类型 |
|---|---|
| 你装的 Ice and Fire 2.1.3（iafenvoy NeoForge 移植） | `net.neoforged.neoforge.registries.DeferredItem<Item>` |
| `ice_and_fire_spellbooks` 2.3.3 编译时 | `dev.architectury.registry.registries.RegistrySupplier` |

即该扩展模组是**对着另一个（Architectury 版）Ice and Fire 编译的**。
后果不只是刷日志：`ArmorMaterialRegistries` 的静态初始化会失败 → **该模组的盔甲材料是坏的**。

### 影响范围（已全量扫描）
`ice_and_fire_spellbooks` 共 42 个类，**只有 2 个**含 architectury `RegistrySupplier` 描述符，
真正引用 `IafItems` 的只有 `ArmorMaterialRegistries` —— 所以**可以打补丁，不必换模组版本**。

具体是 **6 个 lambda**（都是 `()Lnet/minecraft/world/item/crafting/Ingredient;`），各含一处坏 `getstatic`：

| lambda | 字段 |
|---|---|
| `lambda$static$0` / `lambda$static$6` | `DRAGONSTEEL_FIRE_INGOT` |
| `lambda$static$2` / `lambda$static$8` | `DRAGONSTEEL_ICE_INGOT` |
| `lambda$static$4` / `lambda$static$10` | `DRAGONSTEEL_LIGHTNING_INGOT` |

每个 lambda 的形状都是：
```java
return Ingredient.of(new ItemLike[]{ IafItems.DRAGONSTEEL_XXX_INGOT.get() });
```

### 修法（已实施）
在这 6 个 lambda 的 `HEAD` 直接给出 `Ingredient`（`@Inject(cancellable = true)` + `setReturnValue`），
**不碰那处坏 `getstatic`** —— 于是根本不会去解析那个错误描述符，`NoSuchFieldError` 不再发生。

物品由 `IafItemsShim` 用反射从**真实字段**取（`Class.forName` + `Field.get` + `DeferredHolder#get()`），
缓存在 `ConcurrentHashMap`；反射失败时记 ERROR 并回退 `Items.AIR`（**不静默吞掉**）。

> 早先设想的"用 shim 实现 architectury `RegistrySupplier` 再 `@Redirect` 那 6 处 `getstatic`"被放弃：
> 既然目标是"让静态初始化不再失败"，直接在 HEAD 给出结果更小、更少依赖
> （不必依赖 architectury 存在），也不必构造一个假的接口实现。

---

## 构建 / 部署 / 回滚 / 验证

```powershell
.\build.ps1                          # 编译 src + 打包 res → build\compatpatch-1.0.0.jar
.\verify.ps1                         # 离线校验（静态 79 项 + -Xverify:all 装载 9 个类）

.\deploy-compatpatch.ps1 status      # 查看是否在 mods 里（含 SHA 指纹）
.\deploy-compatpatch.ps1 deploy      # 放入 mods
.\deploy-compatpatch.ps1 disable     # 移到 mods\disabled（停用不删）
.\deploy-compatpatch.ps1 rollback    # 移除（回到未打补丁）
.\tools\verify-curios-slots.ps1      # ⑤ 的完备性证明（全实例扫描，复跑用）
```

机器相关路径/版本标签/排除规则都在 `deploy.config.psd1`（脚本不含写死路径）。
类路径构造抽在 `tools\lib\Classpath.ps1`，`build.ps1` 与 `verify.ps1` **共用同一份**（不复制粘贴）。
> `deploy.config.psd1` 与所有 `.ps1` 都必须是 **UTF-8 with BOM** —— PowerShell 5.1 按 GBK 读无 BOM 文件时，
> 中文注释会导致整行被吞（本项目实测踩到过）。

## 离线验证

`.\verify.ps1`，**静态 79 项 + 类装载 9 项全部通过**（`-Xverify:all`，不启动游戏）：

- **A. jar 内容**：类的清单齐全（含 4 个 mixin 配置与 `DuplicateSectionLog`）
- **B. mixin 配置与挂载**：modId、**4 个** `[[mixins]]`、每个配置都是 `required:false`
- **C. 注解常量已内联**：目标类名/方法名/字段描述符确实作为字面量进了常量池
  （没内联的话运行期注解值就是字段引用，直接出错）
- **D1. scguns 注入点**：`writeSpawnData` / `readSpawnData` 描述符匹配，
  且两个方法里**确实有** `GETSTATIC ItemStack.STREAM_CODEC`（`@Redirect` 的落点）
- **D2. jupiter 注入点**：`processEntry` 描述符完全匹配，且该方法里 `getClass()` 出现 2 次、
  `isAssignableFrom` 2 次 —— 与「对 null 调 getClass()」的报错位置一致
- **D3. 线格式相同的证据**：读 `ItemStack$2` 字节码，确认它引用 `OPTIONAL_STREAM_CODEC`
  且含 `Empty ItemStack not allowed` 文案 → 证明它只是外层非空校验包装
- **D4. ③ 的 6 个注入点 + 类型不匹配证明**：6 个 `()Ingredient` lambda 都在、
  各自坏 `getstatic` 的**期望描述符都是 architectury `RegistrySupplier`**，
  而 `IafItems` 里同名字段的**实际描述符是 `DeferredItem`** → 根因成立
- **D5. ⑤ 的 8 个槽位文件**：都在，且都定义了 `order` 与 `icon`
- **E. ② 的注入点与容错安全性**：
  - `decodeColumn` 存在且签名与日志点名处一致
  - 该方法里 `Set.add(Object)` **恰好 1 处**（重定向不会误伤别的调用），
    `Set.size()` / `Set.contains()` 各 1 处且未被改动
  - `VoxelColumnData` 的 4 个构造函数**都不抛唯一性异常** → 重复 Y 结构上被接受
  - `VoxelColumnS2CPayload` 含 `sanitizeReplacementSections` + 调用 `Arrays.sort` +
    带 `Duplicate replacement section` 文案 → **清单必唯一**，第二处校验天然成立
- **装载校验**：9 个补丁类（含新增的 `DuplicateSectionLog` 与 `ClientColumnProcessorMixin`）
  用 `-Xverify:all` 真的装进 JVM（只 Load 不 Initialize，不需要模组运行环境）

**尚未实机验证**（这是唯一还没做的验收环节，需要在游戏里触发）：
1. ①：用 scguns 的窒息弹之类的投掷物打自己 + 带 gytrinket 反射盾 → 确认不再崩服
2. ④：启动后看 Jupiter 那 26 行 `Cannot load key=` 是否消失
3. ③：看 EMI 那 15 行 `Exception thrown registering repair recipes` 是否消失
4. ⑤：看 8 行 `is not a registered slot type!` 是否消失
5. ②：看 508 行 `Duplicate section` 是否消失，取而代之是节流的「已容错继续解码」WARN

---

# ⑥ Wakes：水花面片（破浪）在水消失后永久残留

## 现象（用户截图确认）
水源消失后，草地上会**永久留着**几片扁平的"破浪"面片（浅紫白底 + 蓝色碎块 + 一道深色斜线，
即 `splash_plane_animation.png`），周围一点水都没有。

## 根因（读字节码确认）
`SplashPlaneParticle` 每帧只按这个决定生死：

```java
if (!config.renderSplashPlane) die();
if (owner == null) die();
keep = wake.onWaterSurface()                        // ① 用的是**缓存的标志位**
    && !(玩家 && 鞘翅滑翔)
    && owner.getDeltaMovement().horizontalDistance() >= 0.01;
if (keep) aliveTick(wake); else die();
```

两个漏洞：
1. **它从不调用基类 tick 做 aging，没有任何寿命上限** —— 唯一的消失途径就是那条 `die()`
2. **它从不复查自己/主人脚下还是不是水**，只看缓存标志位。水被挖掉后该标志与真实地形脱节
   （主人不再 tick 时会冻结在旧值）⇒ 判定一直通过 ⇒ 面片冻结在最后位置永不消失

另外核实：`WakeNode.inValidPos()`（本来就是"这格还是水吗"的正确判据）
**只在 `Position`/`QuadTree`/`WakeNode` 内部被引用，`WakeHandler` 从不调用它** ——
"水没了就剔除"在节点和面片两条路上都没做。

## 修法（只加保险，正常游玩行为不变）
在 tick 最前面加两道**独立于缓存标志**的检查：
1. 主人已 `isRemoved()`/`!isAlive()`，或已不是实体 → 立刻清理
2. 用**世界真实流体状态**复查主人所在格与其下方一格；连续 **20 tick**（=1 秒，具名常量
   `MAX_TICKS_WITHOUT_WATER`，给流动水更新留恢复余量）都没有水 → 清理残留面片

## 关于方法名（Fabric 模组 + Connector）
Wakes 是 Fabric 模组：磁盘上是 intermediary 名（`method_3070`/`method_3085`），
运行时被 Connector 重映射成 Mojang 名。编译期无法确定哪个生效，所以：
- 注入器对 `tick` 与 `method_3070` **各挂一个**（都 `require = 0`）
- 移除方法用反射按 `remove` → `method_3085` 顺序试，**命中哪个会记进日志**；都失败则 WARN
- 校验器输出里能看到实测证据：`tick=false / method_3070=true`、`remove=false / method_3085=true`

## 验证
- 6 项静态校验（目标类可读、每帧判定方法存在、移除方法存在、`owner` 字段存在、两个候选名都在常量池）
- `-Xverify:all` 装载 `SplashPlaneParticleMixin` 与反射访问器通过

---

# ⑦ 删除 NeoEnchant 的 Magnet 附魔

## 现象
在车万女仆里用**相机收女仆**时，照片会在生成瞬间被抓走，**女仆随之消失**（女仆数据的唯一副本就在那张照片里）。

## 根因
`NeoEnchant-5.14.0.jar`（modId `mr_neoenchant`，**纯数据包模组：无代码、无配置、无开关**）
的 Magnet 附魔是一个**每 tick 跑的 mcfunction**（`data/minecraft/tags/function/tick.json` → `enchantplus:main`），
全文只有一行：

```mcfunction
execute as @a at @s as @e[type=minecraft:item,distance=..10] if data entity @s {Age:0s} unless data entity @s Thrower run tp @s @p[predicate=enchantplus:magnet]
```

即：把玩家 10 格内、**本 tick 刚生成**且**不是玩家丢出**的物品实体，**直接瞬移到带 Magnet 的玩家身上**。

而 TLM 的相机是 `Containers.dropItemStack(level, player.getX/Y/Z, photo)` —— 照片是**刚生成的全新物品实体**
（`Age=0`、无 `Thrower`）⇒ 正好命中 ⇒ 照片在交付前被抓走，女仆已经被删除，**数据丢失**。

## 处置：直接删除这个附魔（数据覆盖，不改任何原 jar）

| 覆盖文件 | 作用 |
|---|---|
| `data/enchantplus/enchantment/chestplate/magnet.json` | 加 `neoforge:conditions: [{type: neoforge:false}]` ⇒ **条目不被注册 = 真的不存在**（`neoforge:false` 已从 NeoForge 字节码确认：`CONDITION_CODECS.register("false", ...)`）；同时把 `supported_items` 置为 `minecraft:air` 作双保险 |
| `data/enchantplus/function/main.mcfunction` | **空函数** ⇒ 行为彻底不执行（已核实 5.14.0 该函数只做 magnet 一件事） |
| `data/enchantplus/predicate/magnet.json` | 恒假（`random_chance` + `chance 0.0`）⇒ 万一以后版本把函数加回来也选不到人 |
| `data/enchantplus/advancement/armor/chestplate/magnet.json` | criteria 改成 `minecraft:impossible` ⇒ 不再引用已删除的附魔（否则报"缺失引用"） |
| `data/minecraft/tags/enchantment/tradeable.json` | **`replace: true` + 合并全部来源、剔除 magnet**（57 项） |
| `data/minecraft/tags/enchantment/on_traded_equipment.json` | 同上（24 项） |
| `META-INF/neoforge.mods.toml` | 声明对 `mr_neoenchant` 的 **`ordering = "AFTER"`** 依赖 —— 数据包优先级按模组加载顺序，不声明 AFTER 的话我们的覆盖会被 NeoEnchant 压过去、形同没写 |

### 为什么必须连标签一起覆盖（两个差点踩进去的坑）

1. **原版 `TagLoader` 里有这句**：`Couldn't load tag {} as it is missing following references: {}`
   —— 缺失引用会让**整个标签加载失败**。NeoEnchant 把 magnet 塞进了 `#minecraft:tradeable`
   和 `#minecraft:on_traded_equipment`，所以删掉附魔后**必须同时替换这两个标签**，
   否则村民的附魔交易会**整标签失效**。
2. **标签条目会跨包合并**：`"replace": false` 的覆盖**去不掉** magnet（会与 NeoEnchant 的条目合并），
   必须 `"replace": true` + 提供**完整列表**。
3. **`replace: true` 会丢弃其它包对同一标签的贡献**，所以完整列表必须是
   **vanilla + 全部模组**的并集 —— 我直接用脚本扫描实例生成，并逐源打印贡献作为证据
   （`tradeable`：8 个来源 58 项 → 剔除 magnet 后 57 项；`on_traded_equipment`：4 个来源 → 25 项）。
4. **`#minecraft:non_treasure` 这类标签引用必须保留 `#`**：第一版我用正则抽取，
   把 `#` 丢了 → 会变成"引用一个不存在的附魔"→ 同样整标签加载失败。改成真 JSON 解析后校验器会
   专门检查这一项。

## 验证（离线）
- 20 项静态校验：6 个覆盖文件都在、附魔带 `neoforge:false`、函数为空、
  predicate 恒假、advancement 不引用已删附魔、两个标签 `replace:true` + 剔除 magnet +
  **保留 `#minecraft:non_treasure`** + 保留原版 4 个附魔 + 保留其它模组条目、
  `AFTER` 依赖已声明、NeoEnchant 的 tick 函数确实只有 1 条命令（覆盖它安全）

## 需要你知道的三件事

1. **已经附了 Magnet 的旧装备**：条目不再注册后，那些物品上的附魔引用会变成"未知"，
   游戏会记警告（**不影响物品本身与其它的附魔**）。保险起见可以先用铁砧/砂轮把 Magnet 洗掉。
2. **合并出来的两个标签是"快照"**：以后**加/删模组**（尤其带附魔的模组）后，
   需要重新生成这两份文件；`verify.ps1` 会重新扫描实例并对照，能立刻发现不一致。
3. **扫描时有一个资源包读不了**（`!江原绘景 森罗物语：厨房 PBR扩展 1.4.1.zip`，特殊分卷 zip），
   脚本已**显式告警**而非静默跳过。它是纯贴图包，不参与附魔标签，所以不影响本次结果；
   若将来有**带数据的包**出现同样问题，需要先修好那个包再重新生成。