package compatpatch;

/**
 * 所有补丁目标的名字与描述符 —— **单一来源**。
 *
 * 这里全是 {@code public static final String} 编译期常量：
 * javac 会把它们直接内联进注解里，所以运行时不依赖本类，
 * 也让「目标类名/方法名/字段描述符」只出现一次。
 *
 * 命名空间：本实例运行时是 **Mojang 官方名**，所以全部按 Mojang 名书写。
 */
public final class CompatTargets {

    // ── ① scguns：投掷物生成数据里的非空 ItemStack 断言 ──
    /**
     * Scorched Guns 的投掷物基类。
     * 它的 writeSpawnData / readSpawnData 用 {@code ItemStack.STREAM_CODEC}
     * （= OPTIONAL_STREAM_CODEC + 非空断言）写 item 字段，而该字段允许为空
     * （两个构造器都初始化为 ItemStack.EMPTY）。
     * 第三方（gytrinket 的反射）用 EntityType.create 造出实例后就可能是空 item →
     * 编码时抛 EncoderException 崩服。
     */
    public static final String SCGUNS_THROWABLE_ITEM_ENTITY =
            "top.ribs.scguns.entity.throwable.ThrowableItemEntity";

    public static final String SCGUNS_METHOD_WRITE_SPAWN_DATA = "writeSpawnData";
    public static final String SCGUNS_METHOD_READ_SPAWN_DATA = "readSpawnData";

    /** 被替换的字段：ItemStack.STREAM_CODEC（非空断言版）。 */
    public static final String ITEM_STACK_STREAM_CODEC =
            "Lnet/minecraft/world/item/ItemStack;STREAM_CODEC:Lnet/minecraft/network/codec/StreamCodec;";

    // ── ④ Jupiter：配置项值为 null 时的 NPE ──
    /**
     * Jupiter 的 NeoForge 配置桥。
     * {@code processEntry} 里直接对可能在为 null 的值调 {@code getClass()}：
     *   {@code type.isAssignableFrom(value.getClass())}
     * 当 NeoForge 把某个键「校正」为 null（类型不符时会发生）后，
     * 这里就抛 NPE，日志刷 “Cannot load key=... type=java.lang.Double”。
     *
     * 注意：原实现在条件不成立时本来就是**直接返回什么都不做**，
     * 所以「值为 null 时跳过」与作者本意完全一致，属于零语义变更的修复。
     */
    public static final String JUPITER_NIGHT_CONFIG_HOLDER =
            "com.iafenvoy.jupiter.compat.forgeconfigspec.NightConfigHolder";

    public static final String JUPITER_METHOD_PROCESS_ENTRY = "processEntry";

    // ── ③ ice_and_fire_spellbooks 与 Ice and Fire 的字段类型不匹配 ──
    /**
     * 该扩展模组的盔甲材料注册类。它的 6 个 lambda 都会引用
     * {@code IafItems.DRAGONSTEEL_*_INGOT}，但**期望类型**是 architectury 的
     * {@code dev.architectury.registry.registries.RegistrySupplier}，
     * 而你装的 Ice and Fire 2.1.3（iafenvoy NeoForge 移植）里这些字段是
     * {@code net.neoforged.neoforge.registries.DeferredItem}。
     *
     * NoSuchFieldError 的报文里带着期望描述符 —— JVM 按「名字 + 描述符」解析，
     * 名字对、类型不对，所以解析失败。
     */
    public static final String IAF_SPELLBOOKS_ARMOR_MATERIAL_REGISTRIES =
            "net.acetheeldritchking.ice_and_fire_spellbooks.registries.ArmorMaterialRegistries";

    /** Ice and Fire 的物品注册表（字段的真实持有者）。 */
    public static final String IAF_ITEMS = "com.iafenvoy.iceandfire.registry.IafItems";

    // 三个被错误引用的字段名。每个在目标类里出现两次（两个 lambda 各一处）。
    public static final String IAF_FIELD_DRAGONSTEEL_FIRE_INGOT = "DRAGONSTEEL_FIRE_INGOT";
    public static final String IAF_FIELD_DRAGONSTEEL_ICE_INGOT = "DRAGONSTEEL_ICE_INGOT";
    public static final String IAF_FIELD_DRAGONSTEEL_LIGHTNING_INGOT = "DRAGONSTEEL_LIGHTNING_INGOT";

    // ── ② VSS：客户端解码时把「同一 Y 出现两次」当成致命错误 ──
    /**
     * VSS 的客户端柱子处理器。
     *
     * {@code decodeColumn} 里逐段读 section 时有一个自建守卫：
     * <pre>
     *   byte sectionY = buf.readByte();
     *   if (!seen.add((int) sectionY)) {           // ← 本补丁重定向这一处
     *       throw new IllegalArgumentException("Duplicate section " + sectionY);
     *   }
     * </pre>
     * 抛出后**整根柱子被丢弃**（远景点缺失）+ 一条 ERROR 带完整堆栈。
     * 实测日志里这条出现了 445 次。
     *
     * ── 为什么不改服务端 ──
     * 服务端几个环节自身是自洽的（都读字节码确认过）：
     *   · {@code LoadedColumnData}：先 {@code reorderSerializedSections} 重排 body，
     *     再 {@code sortSectionManifest} **排序后查相邻重复**（排序会让任何重复变相邻）→ 会抛
     *   · {@code EncodedColumnData}：{@code validateSectionManifestOrder} 要求严格递增
     *   · {@code ColumnPayloadSplitter}：分组是正确划分，每个 section 只进一组
     * 而日志里服务端的 "strictly ordered" / "Duplicate section" **一次都没出现**，
     * 说明重复是在「存储 → 线路 → 多段拼装」之间引入的，服务端自己没有察觉。
     * 客户端这处守卫是日志点名的失败点，在这里容错既能救回柱子，又不掩盖服务端问题
     * （本补丁会**记一条 WARN**说明发生了多少次）。
     */
    public static final String VSS_CLIENT_COLUMN_PROCESSOR =
            "dev.xantha.vss.networking.client.ClientColumnProcessor";

    /** 出问题的方法（唯一，无需描述符）。 */
    public static final String VSS_METHOD_DECODE_COLUMN = "decodeColumn";

    /**
     * 被重定向的那次调用：{@code java.util.Set.add(Object)}。
     * 已用 javap 确认它在 {@code decodeColumn} 里**只出现一次**
     * （同方法内的 Set.size / Set.contains 各一次，未被重定向）。
     */
    public static final String SET_ADD = "Ljava/util/Set;add(Ljava/lang/Object;)Z";

    // ── ⑥ Wakes：水花面片（splash plane）残留 ──
    /**
     * Wakes 的"破浪"粒子。它在 {@code tick()} 里只根据**缓存的 onWaterSurface 标志**和
     * 水平速度决定生死，**从不复查脚下还是不是水**，而且**从不调用基类 tick 做 aging**
     * （没有任何寿命上限）—— 所以水源消失后它会冻结在最后位置永不消失。
     */
    public static final String WAKES_SPLASH_PLANE_PARTICLE =
            "com.goby56.wakes.particle.custom.SplashPlaneParticle";

    /**
     * 该粒子"移除自己"的方法名候选（按顺序试，取第一个存在的）。
     *
     * Wakes 是 Fabric 模组（经 Connector 加载）：磁盘上是 intermediary 名
     * {@code method_3085}，运行时被重映射成 Mojang 名 {@code remove}。
     * 哪个生效在编译期无法确定，所以两个都留；命中哪个会**记进日志**。
     */
    public static final String[] WAKES_REMOVE_METHOD_CANDIDATES = {"remove", "method_3085"};

    /** 该粒子每帧判定所在的方法名候选（同上：Mojang 名 / intermediary 名）。 */
    public static final String WAKES_TICK_METHOD = "tick";
    public static final String WAKES_TICK_METHOD_INTERMEDIARY = "method_3070";

    private CompatTargets() {
    }
}
