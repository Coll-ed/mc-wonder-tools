package compatpatch;

import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 补丁开关配置。
 *
 * 每一项修复都能单独关掉 —— 万一某个补丁在你的环境里引起新问题，
 * 改一行配置就能退回「原模组行为」，不必卸载模组。
 *
 * 路径取自 {@link FMLPaths#CONFIGDIR}（不写死）。
 * 改动文件后下一次用到该项时生效（每次读值比对一次文件修改时间）。
 */
public final class CompatPatchConfig {

    public static final Logger LOGGER = LoggerFactory.getLogger(CompatPatch.MOD_ID);

    /** 配置文件名（位于 config 目录下）。 */
    private static final String FILE_NAME = "compatpatch.properties";

    // ── 默认值：单一来源（模板与回退共用）──
    private static final boolean DEFAULT_FIX_SCGUNS_EMPTY_ITEM_STACK_SPAWN_DATA = true;
    private static final boolean DEFAULT_FIX_JUPITER_NULL_CONFIG_ENTRY = true;
    private static final boolean DEFAULT_FIX_IAF_SPELLBOOKS_IAF_ITEMS = true;
    private static final boolean DEFAULT_FIX_VSS_DUPLICATE_SECTION = true;
    private static final boolean DEFAULT_FIX_WAKES_SPLASH_PLANE = true;

    private static final String KEY_FIX_SCGUNS_EMPTY_ITEM_STACK_SPAWN_DATA =
            "fixScgunsEmptyItemStackSpawnData";
    private static final String KEY_FIX_JUPITER_NULL_CONFIG_ENTRY = "fixJupiterNullConfigEntry";
    private static final String KEY_FIX_IAF_SPELLBOOKS_IAF_ITEMS = "fixIceAndFireSpellbooksIafItems";
    private static final String KEY_FIX_VSS_DUPLICATE_SECTION = "fixVssDuplicateSection";
    private static final String KEY_FIX_WAKES_SPLASH_PLANE = "fixWakesSplashPlane";

    private static Path configFile;
    private static long lastSeenModified = Long.MIN_VALUE;
    private static boolean loaded;

    private static boolean fixScgunsEmptyItemStackSpawnData =
            DEFAULT_FIX_SCGUNS_EMPTY_ITEM_STACK_SPAWN_DATA;
    private static boolean fixJupiterNullConfigEntry = DEFAULT_FIX_JUPITER_NULL_CONFIG_ENTRY;
    private static boolean fixIceAndFireSpellbooksIafItems = DEFAULT_FIX_IAF_SPELLBOOKS_IAF_ITEMS;
    private static boolean fixVssDuplicateSection = DEFAULT_FIX_VSS_DUPLICATE_SECTION;
    private static boolean fixWakesSplashPlane = DEFAULT_FIX_WAKES_SPLASH_PLANE;

    /** 首次用到配置时读一次；之后每次比对文件修改时间，变了就重读。 */
    public static synchronized void load() {
        if (!loaded) {
            loaded = true;
            try {
                configFile = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
            } catch (Exception e) {
                LOGGER.warn("无法解析配置目录，将只使用内置默认值", e);
                configFile = null;
            }
            read();
            return;
        }
        if (configFile == null) {
            return;
        }
        try {
            long modified = Files.exists(configFile)
                    ? Files.getLastModifiedTime(configFile).toMillis() : -1L;
            if (modified != lastSeenModified) {
                read();
            }
        } catch (Exception e) {
            // 不静默吞：读不到修改时间就明说，并保持上次的值继续跑
            LOGGER.warn("读取配置文件修改时间失败，继续使用当前值: {}", configFile, e);
        }
    }

    public static boolean fixScgunsEmptyItemStackSpawnData() {
        return fixScgunsEmptyItemStackSpawnData;
    }

    public static boolean fixJupiterNullConfigEntry() {
        return fixJupiterNullConfigEntry;
    }

    public static boolean fixIceAndFireSpellbooksIafItems() {
        return fixIceAndFireSpellbooksIafItems;
    }

    public static boolean fixVssDuplicateSection() {
        return fixVssDuplicateSection;
    }

    public static boolean fixWakesSplashPlane() {
        return fixWakesSplashPlane;
    }

    public static Path configFile() {
        return configFile;
    }

    /** 日志里用的摘要。 */
    public static String describe() {
        return "fixScgunsEmptyItemStackSpawnData=" + fixScgunsEmptyItemStackSpawnData
                + ", fixJupiterNullConfigEntry=" + fixJupiterNullConfigEntry
                + ", fixIceAndFireSpellbooksIafItems=" + fixIceAndFireSpellbooksIafItems
                + ", fixVssDuplicateSection=" + fixVssDuplicateSection
                + ", fixWakesSplashPlane=" + fixWakesSplashPlane;
    }

    private static void read() {
        if (configFile == null) {
            return;
        }
        try {
            if (!Files.exists(configFile)) {
                writeTemplate();
            }
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(configFile)) {
                properties.load(in);
            }
            fixScgunsEmptyItemStackSpawnData = parseBoolean(properties,
                    KEY_FIX_SCGUNS_EMPTY_ITEM_STACK_SPAWN_DATA,
                    DEFAULT_FIX_SCGUNS_EMPTY_ITEM_STACK_SPAWN_DATA);
            fixJupiterNullConfigEntry = parseBoolean(properties,
                    KEY_FIX_JUPITER_NULL_CONFIG_ENTRY,
                    DEFAULT_FIX_JUPITER_NULL_CONFIG_ENTRY);
            fixIceAndFireSpellbooksIafItems = parseBoolean(properties,
                    KEY_FIX_IAF_SPELLBOOKS_IAF_ITEMS,
                    DEFAULT_FIX_IAF_SPELLBOOKS_IAF_ITEMS);
            fixVssDuplicateSection = parseBoolean(properties,
                    KEY_FIX_VSS_DUPLICATE_SECTION,
                    DEFAULT_FIX_VSS_DUPLICATE_SECTION);
            fixWakesSplashPlane = parseBoolean(properties,
                    KEY_FIX_WAKES_SPLASH_PLANE,
                    DEFAULT_FIX_WAKES_SPLASH_PLANE);
            lastSeenModified = Files.getLastModifiedTime(configFile).toMillis();
            LOGGER.info("配置已加载: {} ({})", configFile, describe());
        } catch (Exception e) {
            // 显式失败：回退默认值并记日志，不装作什么都没发生
            fixScgunsEmptyItemStackSpawnData = DEFAULT_FIX_SCGUNS_EMPTY_ITEM_STACK_SPAWN_DATA;
            fixJupiterNullConfigEntry = DEFAULT_FIX_JUPITER_NULL_CONFIG_ENTRY;
            fixIceAndFireSpellbooksIafItems = DEFAULT_FIX_IAF_SPELLBOOKS_IAF_ITEMS;
            fixVssDuplicateSection = DEFAULT_FIX_VSS_DUPLICATE_SECTION;
            fixWakesSplashPlane = DEFAULT_FIX_WAKES_SPLASH_PLANE;
            LOGGER.warn("读取配置失败，回退到默认值 ({}) : {}", describe(), configFile, e);
        }
    }

    /**
     * 只认 true/false（忽略大小写）。写错时记 WARN 并用默认值 ——
     * 拼错不该被悄悄当成 false 而让补丁静默失效。
     */
    private static boolean parseBoolean(Properties properties, String key, boolean fallback) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        String value = raw.trim();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        LOGGER.warn("配置项 {} 的值 '{}' 不是 true/false，使用默认值 {}（请改正，避免误解）",
                key, raw, fallback);
        return fallback;
    }

    private static void writeTemplate() throws Exception {
        Path parent = configFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String template = ""
                + "# Compat Patch 补丁开关\n"
                + "# 每项修复都能单独关掉：改成 false 即退回「原模组行为」（不需要卸载模组）。\n"
                + "# 改完保存即生效。本文件只在不存在时生成，之后不会被覆盖。\n"
                + "\n"
                + "# ① 修 scguns 投掷物生成数据的空 ItemStack 崩服\n"
                + "#    症状：io.netty.handler.codec.EncoderException: Empty ItemStack not allowed 导致崩服。\n"
                + "#    原因：ThrowableItemEntity 的 item 允许为空，但写生成数据时用了带非空断言的编解码器；\n"
                + "#          第三方（如 gytrinket 反射投掷物）用 EntityType.create 造实例后 item 仍是空。\n"
                + "#    修法：换成容空编解码器。两者线格式完全相同（非空版只是外层多一层断言），\n"
                + "#          所以协议不变、无兼容风险。\n"
                + "# 默认: " + DEFAULT_FIX_SCGUNS_EMPTY_ITEM_STACK_SPAWN_DATA + "\n"
                + KEY_FIX_SCGUNS_EMPTY_ITEM_STACK_SPAWN_DATA + "="
                + DEFAULT_FIX_SCGUNS_EMPTY_ITEM_STACK_SPAWN_DATA + "\n"
                + "\n"
                + "# ④ 修 Jupiter 配置项值为 null 时的 NPE\n"
                + "#    症状：日志刷 “Cannot load key=... type=java.lang.Double in config=...” + NullPointerException。\n"
                + "#    原因：NeoForge 会把类型不符的配置键「校正」为 null，Jupiter 随后对 null 调 getClass()。\n"
                + "#    修法：值为 null 时跳过该项（原实现在条件不成立时本来就是直接返回，语义不变）。\n"
                + "# 默认: " + DEFAULT_FIX_JUPITER_NULL_CONFIG_ENTRY + "\n"
                + KEY_FIX_JUPITER_NULL_CONFIG_ENTRY + "=" + DEFAULT_FIX_JUPITER_NULL_CONFIG_ENTRY + "\n"
                + "\n"
                + "# ③ 修 ice_and_fire_spellbooks 引用 Ice and Fire 字段时的类型不匹配\n"
                + "#    症状：日志刷 “NoSuchFieldError: ... IafItems does not have member field\n"
                + "#          'dev.architectury.registry.registries.RegistrySupplier DRAGONSTEEL_FIRE_INGOT'”\n"
                + "#          （由 EMI 注册修复配方时触发，共 15 次）。\n"
                + "#    原因：该扩展模组是对着 Architectury 版 Ice and Fire 编译的；\n"
                + "#          你装的 iceandfire-2.1.3（iafenvoy NeoForge 移植）里同名字段是 DeferredItem。\n"
                + "#          NoSuchFieldError 按「名字 + 描述符」解析，名字对、类型不对，所以失败。\n"
                + "#    修法：在该模组那 6 个 lambda 的 HEAD 直接给出 Ingredient，物品由反射从真实字段取。\n"
                + "# 默认: " + DEFAULT_FIX_IAF_SPELLBOOKS_IAF_ITEMS + "\n"
                + KEY_FIX_IAF_SPELLBOOKS_IAF_ITEMS + "=" + DEFAULT_FIX_IAF_SPELLBOOKS_IAF_ITEMS + "\n"
                + "\n"
                + "# ② 修 VSS 客户端把「同一 section Y 出现两次」当致命错误\n"
                + "#    症状：日志刷 “IllegalArgumentException: Duplicate section 0”\n"
                + "#          at ClientColumnProcessor.decodeColumn(ClientColumnProcessor.java:208)\n"
                + "#          （实测 445 次），每次都会**丢弃整根柱子** → 远景点缺失。\n"
                + "#    原因：客户端逐段读 section 时有一处自建守卫，重复 Y 直接抛异常。\n"
                + "#          服务端各环节（LoadedColumnData 排序后查重、EncodedColumnData 严格递增、\n"
                + "#          分包器正确划分）都自洽，且日志里服务端的相关报错一次都没出现 →\n"
                + "#          重复是在「存储 → 线路 → 多段拼装」之间引入的，服务端自己没察觉。\n"
                + "#    修法：把这处守卫改成容错（重复段按「后到者覆盖」继续解码，柱子被救回），\n"
                + "#          并**按次数节流记 WARN**，不静默吞掉。\n"
                + "# 默认: " + DEFAULT_FIX_VSS_DUPLICATE_SECTION + "\n"
                + KEY_FIX_VSS_DUPLICATE_SECTION + "=" + DEFAULT_FIX_VSS_DUPLICATE_SECTION + "\n"
                + "\n"
                + "# ⑥ 修 Wakes 水花面片（破浪）残留不消失\n"
                + "#    症状：水源消失后，若当时有实体在触发游泳动画，草地上会永久留下几片扁平的\n"
                + "#          \"破浪\"面片（贴图 splash_plane_animation.png），周围已经没有水。\n"
                + "#    原因：SplashPlaneParticle 每帧只根据**缓存的 onWaterSurface 标志**和水平速度决定生死，\n"
                + "#          从不复查自己脚下还是不是水；而且它从不调用基类 tick 做 aging，**没有任何寿命上限**。\n"
                + "#          水没了之后该标志与真实地形脱节（主人不再 tick 时会冻结在旧值）⇒ 面片永久冻结。\n"
                + "#    修法：加两道独立于缓存标志的保险 —— ①主人已移除/死亡即刻清理；\n"
                + "#          ②用世界真实流体状态复查主人附近还有没有水，连续 20 tick 没水就清理残留面片。\n"
                + "#          正常游玩（脚下就是水）行为完全不变。\n"
                + "# 默认: " + DEFAULT_FIX_WAKES_SPLASH_PLANE + "\n"
                + KEY_FIX_WAKES_SPLASH_PLANE + "=" + DEFAULT_FIX_WAKES_SPLASH_PLANE + "\n";

        try (OutputStream out = Files.newOutputStream(configFile)) {
            out.write(template.getBytes(StandardCharsets.UTF_8));
        }
    }

    private CompatPatchConfig() {
    }
}
