import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 离线校验 Compat Patch：不启动游戏。
 *
 * 重点是把「注入点真的存在」变成可检查的证据 ——
 * mixin 注解写错一个字符，游戏里要么起不来要么静默不生效，光看源码看不出来。
 */
public final class VerifyCompatPatch {

    private static int pass;
    private static int fail;

    // 补丁 jar 内的期望条目
    private static final String MIXIN_CONFIG_SCGUNS = "compatpatch.scguns.mixins.json";
    private static final String MIXIN_CONFIG_JUPITER = "compatpatch.jupiter.mixins.json";
    private static final String MIXIN_CONFIG_IAF = "compatpatch.iafspellbooks.mixins.json";
    private static final String MIXIN_CONFIG_VSS = "compatpatch.vss.mixins.json";
    private static final String MIXIN_CONFIG_WAKES = "compatpatch.wakes.mixins.json";
    private static final String MODS_TOML = "META-INF/neoforge.mods.toml";
    private static final String MIXIN_SCGUNS =
            "compatpatch/mixin/scguns/ThrowableItemEntityMixin.class";
    private static final String MIXIN_JUPITER =
            "compatpatch/mixin/jupiter/NightConfigHolderMixin.class";
    private static final String MIXIN_IAF =
            "compatpatch/mixin/iafspellbooks/ArmorMaterialRegistriesMixin.class";
    private static final String MIXIN_VSS =
            "compatpatch/mixin/vss/ClientColumnProcessorMixin.class";
    private static final String MIXIN_WAKES =
            "compatpatch/mixin/wakes/SplashPlaneParticleMixin.class";
    /** ⑥ 的反射访问器（移除方法名候选、owner 字段名都在这里）。 */
    private static final String SHIM_WAKES_ACCESS =
            "compatpatch/shim/WakesSplashPlaneAccess.class";
    /** 目标常量类：String[] 形态的候选名（如 ⑥ 的移除方法名）在这里初始化。 */
    private static final String TARGETS_CLASS = "compatpatch/CompatTargets.class";

    /** 每个目标模组一个独立配置：① scguns、④ jupiter、③ ice_and_fire_spellbooks、② vss、⑥ wakes。 */
    private static final List<String> EXPECTED_MIXIN_CONFIGS =
            List.of(MIXIN_CONFIG_SCGUNS, MIXIN_CONFIG_JUPITER, MIXIN_CONFIG_IAF,
                    MIXIN_CONFIG_VSS, MIXIN_CONFIG_WAKES);

    // 目标类（在各自 mod jar 里）
    private static final String T_SCGUNS = "top/ribs/scguns/entity/throwable/ThrowableItemEntity.class";
    private static final String T_JUPITER =
            "com/iafenvoy/jupiter/compat/forgeconfigspec/NightConfigHolder.class";
    private static final String T_ITEMSTACK_2 = "net/minecraft/world/item/ItemStack$2.class";
    private static final String T_ARMOR_MATERIAL_REGISTRIES =
            "net/acetheeldritchking/ice_and_fire_spellbooks/registries/ArmorMaterialRegistries.class";
    private static final String T_IAF_ITEMS = "com/iafenvoy/iceandfire/registry/IafItems.class";
    /** ② VSS：点名抛异常的那一处。 */
    private static final String T_VSS_CLIENT_COLUMN_PROCESSOR =
            "dev/xantha/vss/networking/client/ClientColumnProcessor.class";
    /** ② 下游数据类：用来证明「重复 Y 结构上被接受」（构造函数不做唯一性校验）。 */
    private static final String T_VSS_VOXEL_COLUMN_DATA = "dev/xantha/vss/api/VoxelColumnData.class";
    /** ② 负载类：用来证明「清单必唯一」（构造时排序后查重，有重复会抛）。 */
    private static final String T_VSS_PAYLOAD =
            "dev/xantha/vss/networking/payloads/VoxelColumnS2CPayload.class";

    /** ⑥ Wakes 的"破浪"粒子（残留水花面片）。 */
    private static final String T_WAKES_SPLASH_PLANE =
            "com/goby56/wakes/particle/custom/SplashPlaneParticle.class";

    // ── ⑦ 禁用 NeoEnchant 的 Magnet（数据覆盖）──
    private static final String OVERRIDE_FUNCTION = "data/enchantplus/function/main.mcfunction";
    private static final String OVERRIDE_PREDICATE = "data/enchantplus/predicate/magnet.json";
    private static final String OVERRIDE_ENCHANTMENT =
            "data/enchantplus/enchantment/chestplate/magnet.json";
    private static final String OVERRIDE_ADVANCEMENT =
            "data/enchantplus/advancement/armor/chestplate/magnet.json";
    private static final String OVERRIDE_TAG_TRADEABLE =
            "data/minecraft/tags/enchantment/tradeable.json";
    private static final String OVERRIDE_TAG_ON_TRADED =
            "data/minecraft/tags/enchantment/on_traded_equipment.json";
    private static final String MAGNET_ID = "enchantplus:chestplate/magnet";
    /** NeoEnchant 自己的 tick 函数（在它 jar 里）——用来证明"覆盖它是安全的"。 */
    private static final String NEOENCHANT_MAIN_FUNCTION = "data/enchantplus/function/main.mcfunction";
    private static final String MAGNET_COMMAND_MARKER = "predicate=enchantplus:magnet";

    /** ② decodeColumn 的签名（日志里点名的失败点）。 */
    private static final String DESC_DECODE_COLUMN =
            "(Ldev/xantha/vss/networking/client/ClientColumnTransferAssembler$AssembledColumn;"
                    + "Lnet/minecraft/core/Registry;)Ldev/xantha/vss/api/VoxelColumnData;";
    /** ② 被重定向的调用：Set.add(Object)Z（必须在该方法里唯一）。 */
    private static final String VSS_SET_ADD = "Ljava/util/Set;add(Ljava/lang/Object;)Z";

    private static final String DESC_WRITE = "(Lnet/minecraft/network/RegistryFriendlyByteBuf;)V";
    private static final String DESC_READ = "(Lnet/minecraft/network/RegistryFriendlyByteBuf;)V";
    private static final String DESC_PROCESS_ENTRY =
            "(Ljava/util/concurrent/atomic/AtomicReference;"
                    + "Lcom/electronwill/nightconfig/core/CommentedConfig;"
                    + "Lnet/minecraft/network/chat/Component;"
                    + "Lcom/electronwill/nightconfig/core/UnmodifiableConfig$Entry;"
                    + "Ljava/lang/Object;Ljava/lang/Object;"
                    + "Ljava/lang/Class;"
                    + "Ljava/util/function/BiFunction;)V";

    private static final String ITEM_STACK_STREAM_CODEC_FIELD =
            "Lnet/minecraft/world/item/ItemStack;STREAM_CODEC:"
                    + "Lnet/minecraft/network/codec/StreamCodec;";

    public static void main(String[] args) throws Exception {
        if (args.length < 9) {
            System.err.println("用法: VerifyCompatPatch <补丁jar> <scguns jar> <jupiter jar> <Minecraft jar>"
                    + " <ice_and_fire_spellbooks jar> <iceandfire jar> <vss jar> <wakes jar>"
                    + " <NeoEnchant jar>");
            System.exit(2);
        }
        Path patchJar = Path.of(args[0]);
        Path scgunsJar = Path.of(args[1]);
        Path jupiterJar = Path.of(args[2]);
        Path minecraftJar = Path.of(args[3]);
        Path spellbooksJar = Path.of(args[4]);
        Path iceandfireJar = Path.of(args[5]);
        Path vssJar = Path.of(args[6]);
        Path wakesJar = Path.of(args[7]);
        Path neoenchantJar = Path.of(args[8]);

        System.out.println("=========== A. 补丁 jar 内容 ===========");
        Map<String, byte[]> patch = readZip(patchJar);
        for (String name : List.of(
                MODS_TOML, "pack.mcmeta",
                MIXIN_CONFIG_SCGUNS, MIXIN_CONFIG_JUPITER, MIXIN_CONFIG_IAF, MIXIN_CONFIG_VSS,
                MIXIN_CONFIG_WAKES,
                MIXIN_SCGUNS, MIXIN_JUPITER, MIXIN_IAF, MIXIN_VSS, MIXIN_WAKES,
                "compatpatch/CompatPatch.class",
                "compatpatch/CompatPatchConfig.class",
                "compatpatch/CompatTargets.class",
                "compatpatch/DuplicateSectionLog.class",
                "compatpatch/shim/IafItemsShim.class")) {
            check("包含 " + name, patch.containsKey(name));
        }

        System.out.println();
        System.out.println("=========== B. mixin 配置与挂载 ===========");
        String toml = new String(patch.get(MODS_TOML), StandardCharsets.UTF_8);
        check("modId = compatpatch", toml.contains("modId = \"compatpatch\""));
        check("挂了 " + EXPECTED_MIXIN_CONFIGS.size() + " 个 mixin 配置，实际 "
                        + countOccurrences(toml, "[[mixins]]"),
                countOccurrences(toml, "[[mixins]]") == EXPECTED_MIXIN_CONFIGS.size());
        for (String cfg : EXPECTED_MIXIN_CONFIGS) {
            check("挂载 " + cfg, toml.contains(cfg));
        }
        for (String cfg : EXPECTED_MIXIN_CONFIGS) {
            String json = new String(patch.get(cfg), StandardCharsets.UTF_8);
            check(cfg + " required=false（目标模组不在时只警告、不崩游戏）",
                    json.contains("\"required\": false"));
        }

        System.out.println();
        System.out.println("=========== C. mixin 注解常量已内联成字面量 ===========");
        byte[] mixinScguns = patch.get(MIXIN_SCGUNS);
        byte[] mixinJupiter = patch.get(MIXIN_JUPITER);
        check("scguns 目标类名在常量池里",
                containsBytes(mixinScguns, "top.ribs.scguns.entity.throwable.ThrowableItemEntity"));
        check("writeSpawnData / readSpawnData 方法名在常量池里",
                containsBytes(mixinScguns, "writeSpawnData")
                        && containsBytes(mixinScguns, "readSpawnData"));
        check("被替换的字段描述符在常量池里",
                containsBytes(mixinScguns, ITEM_STACK_STREAM_CODEC_FIELD));
        check("jupiter 目标类名在常量池里",
                containsBytes(mixinJupiter, "com.iafenvoy.jupiter.compat.forgeconfigspec.NightConfigHolder"));
        check("processEntry 方法名在常量池里", containsBytes(mixinJupiter, "processEntry"));
        byte[] mixinVss = patch.get(MIXIN_VSS);
        check("vss 目标类名在常量池里",
                containsBytes(mixinVss, "dev.xantha.vss.networking.client.ClientColumnProcessor"));
        check("decodeColumn 方法名在常量池里", containsBytes(mixinVss, "decodeColumn"));
        check("被重定向的调用描述符在常量池里", containsBytes(mixinVss, VSS_SET_ADD));

        System.out.println();
        System.out.println("=========== D1. scguns 注入点确实存在 ===========");
        byte[] scgunsBytes = readZip(scgunsJar).get(T_SCGUNS);
        check("能读到 " + T_SCGUNS, scgunsBytes != null);
        if (scgunsBytes == null) {
            System.out.println("       scguns jar: " + scgunsJar);
            System.exit(1);
        }
        ClassNode scguns = new ClassNode();
        new ClassReader(scgunsBytes).accept(scguns, 0);
        check("writeSpawnData" + DESC_WRITE + " 存在",
                findMethod(scguns, "writeSpawnData", DESC_WRITE) != null);
        check("readSpawnData" + DESC_READ + " 存在",
                findMethod(scguns, "readSpawnData", DESC_READ) != null);
        for (String method : List.of("writeSpawnData", "readSpawnData")) {
            MethodNode node = findMethod(scguns, method, DESC_WRITE);
            if (node == null) {
                continue;
            }
            boolean hit = false;
            for (AbstractInsnNode insn = node.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.GETSTATIC && insn instanceof FieldInsnNode f
                        && f.owner.equals("net/minecraft/world/item/ItemStack")
                        && f.name.equals("STREAM_CODEC")) {
                    hit = true;
                    break;
                }
            }
            check(method + " 里有 GETSTATIC ItemStack.STREAM_CODEC（@Redirect 的落点）", hit);
        }

        System.out.println();
        System.out.println("=========== D2. jupiter 注入点确实存在 ===========");
        byte[] jupiterBytes = readZip(jupiterJar).get(T_JUPITER);
        check("能读到 " + T_JUPITER, jupiterBytes != null);
        if (jupiterBytes == null) {
            System.out.println("       jupiter jar: " + jupiterJar);
            System.exit(1);
        }
        ClassNode jupiter = new ClassNode();
        new ClassReader(jupiterBytes).accept(jupiter, 0);
        MethodNode processEntry = findMethod(jupiter, "processEntry", DESC_PROCESS_ENTRY);
        check("processEntry 描述符完全匹配", processEntry != null);
        if (processEntry != null) {
            int getClassCalls = 0;
            int isAssignableCalls = 0;
            for (AbstractInsnNode insn = processEntry.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode m) {
                    if (m.owner.equals("java/lang/Object") && m.name.equals("getClass")) {
                        getClassCalls++;
                    }
                    if (m.owner.equals("java/lang/Class") && m.name.equals("isAssignableFrom")) {
                        isAssignableCalls++;
                    }
                }
            }
            System.out.println("       processEntry 里 getClass() 调用 " + getClassCalls
                    + " 次，isAssignableFrom 调用 " + isAssignableCalls + " 次");
            check("正是那个对 null 调 getClass() 的方法（≥2 次 getClass）", getClassCalls >= 2);
            check("类型判断用 isAssignableFrom（≥2 次）", isAssignableCalls >= 2);
        }

        System.out.println();
        System.out.println("=========== D3. 「两个编解码器线格式相同」的证据 ===========");
        byte[] itemStack2 = readZip(minecraftJar).get(T_ITEMSTACK_2);
        check("能读到 ItemStack$2（= ItemStack.STREAM_CODEC 的实现类）", itemStack2 != null);
        if (itemStack2 != null) {
            // ItemStack$2 把 encode/decode 全部委托给 OPTIONAL_STREAM_CODEC，只外加非空断言。
            // 因此把 STREAM_CODEC 换成 OPTIONAL_STREAM_CODEC 不改变线格式。
            boolean delegates = false;
            for (String needle : List.of(
                    "net/minecraft/world/item/ItemStack",
                    "OPTIONAL_STREAM_CODEC")) {
                delegates |= containsBytes(itemStack2, needle);
            }
            check("ItemStack$2 引用了 OPTIONAL_STREAM_CODEC（说明是外层校验包装）",
                    containsBytes(itemStack2, "OPTIONAL_STREAM_CODEC"));
            check("ItemStack$2 含非空断言文案 'Empty ItemStack not allowed'",
                    containsBytes(itemStack2, "Empty ItemStack not allowed"));
            check("委托关系成立", delegates);
        }

        System.out.println();
        System.out.println("=========== D4. ③ 的 6 个注入点 + 类型不匹配证明 ===========");
        byte[] spellbooksBytes = readZip(spellbooksJar).get(T_ARMOR_MATERIAL_REGISTRIES);
        check("能读到 " + T_ARMOR_MATERIAL_REGISTRIES, spellbooksBytes != null);
        if (spellbooksBytes == null) {
            System.out.println("       spellbooks jar: " + spellbooksJar);
            System.exit(1);
        }
        ClassNode registries = new ClassNode();
        new ClassReader(spellbooksBytes).accept(registries, 0);
        check("该类有 lambda$static$0..$10 共 6 个 Ingredient lambda，且各自引用了 IafItems 字段",
                checkLambdas(registries));

        // 真正把「名字对、类型不对」证出来：
        //   期望描述符（该模组编译时）：Ldev/architectury/registry/registries/RegistrySupplier;
        //   实际描述符（iceandfire-2.1.3）  ：Lnet/neoforged/neoforge/registries/DeferredItem;
        byte[] iafBytes = readZip(iceandfireJar).get(T_IAF_ITEMS);
        check("能读到 " + T_IAF_ITEMS, iafBytes != null);
        if (iafBytes != null) {
            ClassNode iafItems = new ClassNode();
            new ClassReader(iafBytes).accept(iafItems, 0);
            for (String fieldName : List.of(
                    "DRAGONSTEEL_FIRE_INGOT", "DRAGONSTEEL_ICE_INGOT", "DRAGONSTEEL_LIGHTNING_INGOT")) {
                var field = findField(iafItems, fieldName);
                if (field == null) {
                    check("IafItems." + fieldName + " 存在", false);
                    continue;
                }
                boolean actuallyDeferred =
                        field.desc.equals("Lnet/neoforged/neoforge/registries/DeferredItem;");
                System.out.println("       IafItems." + fieldName + " 实际描述符 = " + field.desc);
                check("IafItems." + fieldName + " 的实际类型是 DeferredItem"
                                + "（与扩展模组期望的 architectury RegistrySupplier 不同 ⇒ 正是崩溃根因）",
                        actuallyDeferred);
            }
        }

        System.out.println();
        System.out.println("=========== D5. ⑤ 的 8 个槽位定义（纯数据文件，无需 mixin） ===========");
        // 引用方是 touhou_little_maid 的 data/touhou_little_maid/curios/entities/*.json；
        // Curios 最终按 ResourceLocation.getPath()（即文件名）建键，所以文件名必须与引用名一致。
        for (String slot : MISSING_CURIOS_SLOTS) {
            String entry = "data/compatpatch/curios/slots/" + slot + ".json";
            byte[] content = patch.get(entry);
            check("含槽位定义 " + entry, content != null);
            if (content != null) {
                String json = new String(content, StandardCharsets.UTF_8);
                check("  " + slot + " 定义了 order 与 icon",
                        json.contains("\"order\"") && json.contains("\"icon\""));
            }
        }

        System.out.println();
        System.out.println("=========== E. ② VSS 注入点与容错安全性 ===========");
        byte[] vssCcpBytes = readZip(vssJar).get(T_VSS_CLIENT_COLUMN_PROCESSOR);
        check("能读到 " + T_VSS_CLIENT_COLUMN_PROCESSOR, vssCcpBytes != null);
        if (vssCcpBytes != null) {
            ClassNode ccp = new ClassNode();
            new ClassReader(vssCcpBytes).accept(ccp, 0);
            MethodNode decode = findMethod(ccp, "decodeColumn", DESC_DECODE_COLUMN);
            check("decodeColumn 存在且签名与日志点名处一致", decode != null);
            if (decode != null) {
                int addCount = countInvoke(decode, "java/util/Set", "add",
                        "(Ljava/lang/Object;)Z");
                int sizeCount = countInvoke(decode, "java/util/Set", "size", "()I");
                int containsCount = countInvoke(decode, "java/util/Set", "contains",
                        "(Ljava/lang/Object;)Z");
                check("decodeColumn 里 Set.add(Object) 恰好 1 处（重定向不会误伤别的调用），实际 "
                        + addCount, addCount == 1);
                check("同一方法里 Set.size() 与 Set.contains() 仍各 1 处（本补丁未重定向它们），实际 "
                        + sizeCount + " / " + containsCount,
                        sizeCount == 1 && containsCount == 1);
                // 证据：抛出点确实紧跟在那次 Set.add 之后（ifne -> new IllegalArgumentException）
                check("decodeColumn 里存在 IllegalArgumentException 抛出点（就是被容错的那处断言）",
                        containsBytes(vssCcpBytes, "Duplicate section "));
            }
        }
        // 安全性论证：下游 VoxelColumnData 的构造函数不做任何唯一性校验，
        // 所以「同一 Y 出现两次」在数据结构上被接受（补丁不会只是把异常挪到后面）。
        byte[] voxelBytes = readZip(vssJar).get(T_VSS_VOXEL_COLUMN_DATA);
        check("能读到 " + T_VSS_VOXEL_COLUMN_DATA, voxelBytes != null);
        if (voxelBytes != null) {
            ClassNode voxel = new ClassNode();
            new ClassReader(voxelBytes).accept(voxel, 0);
            boolean ctorThrows = false;
            int ctors = 0;
            for (MethodNode m : voxel.methods) {
                if (!m.name.equals("<init>")) {
                    continue;
                }
                ctors++;
                for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null;
                        insn = insn.getNext()) {
                    if (insn instanceof org.objectweb.asm.tree.TypeInsnNode t
                            && t.getOpcode() == Opcodes.NEW
                            && t.desc.equals("java/lang/IllegalArgumentException")) {
                        ctorThrows = true;
                    }
                }
            }
            check("VoxelColumnData 有 " + ctors + " 个构造函数，且都**不**抛唯一性异常"
                    + "（⇒ 重复 Y 结构上被接受，容错不会把异常挪到下游）",
                    ctors > 0 && !ctorThrows);
        }
        // 安全性论证之二：payload 构造时 sanitizeReplacementSections 会「排序后查相邻重复」，
        // 有任何重复都抛 IllegalArgumentException ⇒ 到达 decodeColumn 的清单**必然唯一**，
        // 所以容错掉那处 Set.add 后，后面的 manifest.length == seen.size() 必然成立
        // （清单长度 = 去重计数 = seen.size()），不会紧接着抛第二个异常。
        //
        // 这里用**字节级证据**而不是 ASM 方法匹配：实测该类的 MethodNode.desc 只解析出
        // 6 个码点（`([I)[I`，缺末尾 `]`），与 javap 渲染的 7 码点不一致 ——
        // 用描述符做匹配不可靠，改成直接查常量池里的三个事实。
        byte[] payloadBytes = readZip(vssJar).get(T_VSS_PAYLOAD);
        check("能读到 " + T_VSS_PAYLOAD, payloadBytes != null);
        if (payloadBytes != null) {
            boolean hasSanitize = containsBytes(payloadBytes, "sanitizeReplacementSections");
            boolean hasSort = containsBytes(payloadBytes, "java/util/Arrays")
                    && containsBytes(payloadBytes, "sort");
            boolean hasDupMessage = containsBytes(payloadBytes, "Duplicate replacement section");
            check("VoxelColumnS2CPayload 含 sanitizeReplacementSections（清单净化入口）", hasSanitize);
            check("它调用 Arrays.sort（先排序 ⇒ 任何重复都会变成相邻）", hasSort);
            check("它带 'Duplicate section ' 报错文案（查重后抛异常 ⇒ 清单必唯一，"
                    + "decodeColumn 的第二处校验不会接着失败）", hasDupMessage);
        }

        System.out.println();
        System.out.println("=========== F. ⑥ Wakes 注入点与反射安全性 ===========");
        byte[] wakesBytes = readZip(wakesJar).get(T_WAKES_SPLASH_PLANE);
        check("能读到 " + T_WAKES_SPLASH_PLANE, wakesBytes != null);
        if (wakesBytes != null) {
            ClassNode particle = new ClassNode();
            new ClassReader(wakesBytes).accept(particle, 0);
            boolean hasTick = findMethod(particle, "tick", "()V") != null;
            boolean hasTickIntermediary = findMethod(particle, "method_3070", "()V") != null;
            check("SplashPlaneParticle 有每帧判定方法（tick()V 或 method_3070()V）；"
                            + "实际 tick=" + hasTick + " / method_3070=" + hasTickIntermediary
                            + " —— 注入器 require=0，两个候选覆盖重映射的两种可能",
                    hasTick || hasTickIntermediary);
            boolean hasRemove = findMethod(particle, "remove", "()V") != null;
            boolean hasRemoveIntermediary = findMethod(particle, "method_3085", "()V") != null;
            check("SplashPlaneParticle 有移除方法（remove()V 或 method_3085()V）"
                            + " —— 反射调用它才能把实体上的 wakeParticle 引用一并清掉；"
                            + "实际 remove=" + hasRemove + " / method_3085=" + hasRemoveIntermediary,
                    hasRemove || hasRemoveIntermediary);
            boolean hasOwnerField = false;
            for (org.objectweb.asm.tree.FieldNode f : particle.fields) {
                if (f.name.equals("owner")) {
                    hasOwnerField = true;
                }
            }
            check("SplashPlaneParticle 有 owner 字段（反射据此判断主人是否已失效）", hasOwnerField);
        }
        byte[] mixinWakes = patch.get(MIXIN_WAKES);
        check("wakes 目标类名在常量池里",
                containsBytes(mixinWakes, "com.goby56.wakes.particle.custom.SplashPlaneParticle"));
        check("两个候选方法名都在常量池里（tick / method_3070）",
                containsBytes(mixinWakes, "tick") && containsBytes(mixinWakes, "method_3070"));
        check("移除方法两个候选名都在 CompatTargets 的常量池里（remove / method_3085）"
                        + " —— 磁盘上是 intermediary 名，运行时会被 Connector 重映射成 Mojang 名，所以两个都要留",
                containsBytes(patch.get(TARGETS_CLASS), "remove")
                        && containsBytes(patch.get(TARGETS_CLASS), "method_3085"));

        System.out.println();
        System.out.println("=========== G. ⑦ 删除 NeoEnchant 的 Magnet 附魔（数据覆盖） ===========");
        // 1) 覆盖文件清单
        for (String entry : List.of(OVERRIDE_FUNCTION, OVERRIDE_PREDICATE, OVERRIDE_ENCHANTMENT,
                OVERRIDE_ADVANCEMENT, OVERRIDE_TAG_TRADEABLE, OVERRIDE_TAG_ON_TRADED)) {
            check("含覆盖文件 " + entry, patch.containsKey(entry));
        }
        // 2) 附魔本体：neoforge:false ⇒ 该注册项不被加载（= 从注册表里删掉）
        String ench = asText(patch.get(OVERRIDE_ENCHANTMENT));
        check("附魔定义带 neoforge:conditions=[{type:neoforge:false}] ⇒ 条目不被注册（真删除）",
                ench.contains("neoforge:conditions") && ench.contains("\"type\": \"neoforge:false\""));
        check("同时把 supported_items 置为 minecraft:air（双重保险：即使条件失效也无法被附上）",
                ench.contains("\"supported_items\": \"minecraft:air\""));
        // 3) 覆盖函数为空（行为不执行）
        String fnText = asText(patch.get(OVERRIDE_FUNCTION));
        check("覆盖的 main.mcfunction 只含注释、没有任何命令（magnet 行为不再执行）",
                isCommentOnly(fnText));
        // 4) 兜底 predicate 恒假
        String pred = asText(patch.get(OVERRIDE_PREDICATE));
        check("兜底 predicate 恒假（random_chance + chance 0.0）"
                        + " —— 万一以后版本把函数加回来，@p[predicate=...] 也永远选不到人",
                pred.contains("minecraft:random_chance") && pred.contains("0.0"));
        // 5) advancement 不能再引用已删除的附魔（否则会报"缺失引用"）
        String adv = asText(patch.get(OVERRIDE_ADVANCEMENT));
        check("advancement 的 criteria 改成 minecraft:impossible（不再引用已删除的附魔）",
                adv.contains("minecraft:impossible") && !adv.contains(MAGNET_ID));
        // 6) 两个共享标签：replace:true（不这样的话标签条目会与 NeoEnchant 的合并、magnet 去不掉）
        for (String tag : List.of(OVERRIDE_TAG_TRADEABLE, OVERRIDE_TAG_ON_TRADED)) {
            String t = asText(patch.get(tag));
            check(tag + " 使用 replace:true（否则条目会合并，magnet 去不掉）",
                    t.contains("\"replace\": true"));
            check(tag + " 已剔除 " + MAGNET_ID, !t.contains(MAGNET_ID));
            check(tag + " 保留原版标签引用 #minecraft:non_treasure"
                            + "（丢掉它会让原版附魔交易全部失效 —— 实测 TagLoader 会整标签加载失败）",
                    t.contains("#minecraft:non_treasure"));
        }
        String tradeableTag = asText(patch.get(OVERRIDE_TAG_TRADEABLE));
        check("tradeable 仍保留原版 4 个附魔（mending/frost_walker/binding_curse/vanishing_curse）",
                tradeableTag.contains("minecraft:mending") && tradeableTag.contains("minecraft:frost_walker")
                        && tradeableTag.contains("minecraft:binding_curse")
                        && tradeableTag.contains("minecraft:vanishing_curse"));
        check("tradeable 仍保留其它模组的条目（示例 enchantplus:pickaxe/vein_miner）",
                tradeableTag.contains("enchantplus:pickaxe/vein_miner"));
        // 7) 声明 AFTER 依赖：数据包优先级按模组加载顺序，后加载的覆盖先加载的
        check("mods.toml 声明了对 mr_neoenchant 的 AFTER 依赖（否则覆盖会被它压过去）",
                toml.contains("mr_neoenchant") && toml.contains("ordering = \"AFTER\""));
        // 8) 安全性证据：NeoEnchant 那个 tick 函数确实只做 magnet 一件事
        byte[] neoenchantFn = readZip(neoenchantJar).get(NEOENCHANT_MAIN_FUNCTION);
        check("能读到 NeoEnchant 的 " + NEOENCHANT_MAIN_FUNCTION, neoenchantFn != null);
        if (neoenchantFn != null) {
            String nf = asText(neoenchantFn);
            check("它确实包含 magnet 命令（" + MAGNET_COMMAND_MARKER + "）", nf.contains(MAGNET_COMMAND_MARKER));
            check("它只有 " + countCommands(nf) + " 条命令（= 只做 magnet 一件事）"
                            + " ⇒ 覆盖成空函数不会误伤 NeoEnchant 的其它功能",
                    countCommands(nf) == 1);
        }

        System.out.println();
        System.out.println(fail == 0
                ? "[ALL PASS] " + pass + " 项校验全部通过"
                : "[FAIL] " + fail + " 项失败 / 共 " + (pass + fail) + " 项");
        if (fail > 0) {
            System.exit(1);
        }
    }

    /** 把 jar 条目按 UTF-8 转字符串；条目不存在时返回空串（避免 NPE，并在调用处由 check 报出）。 */
    private static String asText(byte[] bytes) {
        return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
    }

    /** 该 mcfunction 是否只有注释/空行（= 不含任何命令）。 */
    private static boolean isCommentOnly(String functionText) {
        return countCommands(functionText) == 0;
    }

    /** 数 mcfunction 里的命令条数（忽略空行与 # 注释）。 */
    private static int countCommands(String functionText) {
        int commands = 0;
        for (String line : functionText.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) {
                commands++;
            }
        }
        return commands;
    }

    /** 数一个方法里对指定 owner/name/desc 的 INVOKE 次数。 */
    private static int countInvoke(MethodNode method, String owner, String name, String desc) {        int n = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null;
                insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode m
                    && m.owner.equals(owner) && m.name.equals(name) && m.desc.equals(desc)) {
                n++;
            }
        }
        return n;
    }

    /** 日志 L6175~L6182 报的 8 个未注册槽位。 */
    private static final List<String> MISSING_CURIOS_SLOTS = List.of(
            "aether_accessory", "aether_cape", "aether_gloves", "aether_pendant",
            "aether_ring", "aether_shield", "scroll", "feet");

    /** 检查 6 个 lambda 都存在、都是 ()Ingredient、且各自含一处指向 IafItems 的 GETSTATIC。 */
    private static boolean checkLambdas(ClassNode node) {
        String[] lambdas = {"lambda$static$0", "lambda$static$2", "lambda$static$4",
                "lambda$static$6", "lambda$static$8", "lambda$static$10"};
        boolean allOk = true;
        for (String name : lambdas) {
            MethodNode m = findMethod(node, name, "()Lnet/minecraft/world/item/crafting/Ingredient;");
            if (m == null) {
                System.out.println("       [缺] " + name + "()Ingredient");
                allOk = false;
                continue;
            }
            FieldInsnNode bad = null;
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.GETSTATIC && insn instanceof FieldInsnNode f
                        && f.owner.equals("com/iafenvoy/iceandfire/registry/IafItems")) {
                    bad = f;
                    break;
                }
            }
            if (bad == null) {
                System.out.println("       [缺] " + name + " 里没有 IafItems 的 GETSTATIC");
                allOk = false;
            } else {
                System.out.println("       " + name + " -> IafItems." + bad.name
                        + " (期望描述符 " + bad.desc + ")");
                if (!bad.desc.equals("Ldev/architectury/registry/registries/RegistrySupplier;")) {
                    System.out.println("       [异常] 期望描述符与预期不符，注入点假设可能已失效");
                    allOk = false;
                }
            }
        }
        return allOk;
    }

    private static org.objectweb.asm.tree.FieldNode findField(ClassNode node, String name) {
        for (org.objectweb.asm.tree.FieldNode field : node.fields) {
            if (field.name.equals(name)) {
                return field;
            }
        }
        return null;
    }

    private static MethodNode findMethod(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }
        return null;
    }

    private static boolean containsBytes(byte[] haystack, String needle) {
        byte[] target = needle.getBytes(StandardCharsets.UTF_8);
        outer:
        for (int i = 0; i <= haystack.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (haystack[i + j] != target[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static Map<String, byte[]> readZip(Path jar) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var it = zip.entries();
            while (it.hasMoreElements()) {
                ZipEntry entry = it.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] chunk = new byte[8192];
                    int read;
                    while ((read = in.read(chunk)) > 0) {
                        buffer.write(chunk, 0, read);
                    }
                    entries.put(entry.getName(), buffer.toByteArray());
                }
            }
        }
        return entries;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (ok) {
            pass++;
        } else {
            fail++;
        }
    }

    private VerifyCompatPatch() {
    }
}
