package compatpatch.mixin.vss;

import compatpatch.CompatPatchConfig;
import compatpatch.CompatTargets;
import compatpatch.DuplicateSectionLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

/**
 * ② 修 VSS 客户端因「重复 section Y」丢弃整根柱子。
 *
 * ── 现象（logs/latest.log，实测 445 次）──
 * <pre>
 *   [VSS-ColumnProcessor/ERROR] [VSS/]: Failed to process voxel column transfer N at X,Z
 *   java.lang.IllegalArgumentException: Duplicate section 0
 *     at ClientColumnProcessor.decodeColumn(ClientColumnProcessor.java:208)
 * </pre>
 *
 * ── 原因 ──
 * {@code decodeColumn} 逐段读 section 时有自建守卫：
 * <pre>
 *   byte sectionY = buf.readByte();
 *   if (!seen.add((int) sectionY)) {
 *       throw new IllegalArgumentException("Duplicate section " + sectionY);
 *   }
 * </pre>
 * 抛出的后果是**整根柱子被丢弃**（远景点缺失），而不是降级使用。
 *
 * 服务端几个环节都读字节码确认过、各自自洽（见 {@link CompatTargets} 里的说明），
 * 且日志里服务端相关报错一次都没出现 —— 重复是「存储 → 线路 → 多段拼装」之间引入的。
 *
 * ── 修法（只重定向这一处 Set.add）──
 * 让守卫认为「没有重复」，解码继续；重复的那一段按**后到者覆盖**生效
 * （两个候选属于同一根柱子、同一个 Y，所以选哪个都是这个位置的合法数据）。
 *
 * 已核实这样改是安全的：
 * <ul>
 *   <li>{@code Set.add} 在 {@code decodeColumn} 里**只出现一次**（javap 确认），
 *       所以重定向不会误伤别的集合操作；同方法内的 {@code Set.size}/{@code Set.contains} 未被改动。</li>
 *   <li>后面的清单校验是 {@code manifest.length == seen.size()}，而 {@code seen} 是**去重**集合、
 *       清单在服务端被保证唯一（{@code LoadedColumnData.sortSectionManifest} 排序后查重、
 *       {@code EncodedColumnData.validateSectionManifestOrder} 要求严格递增）——
 *       所以容错后这两个值仍然相等，不会紧接着抛第二个异常。</li>
 *   <li>下游 {@code VoxelColumnData} 的构造函数只做 null 兜底、**不做任何唯一性校验**，重复 Y 结构上被接受。</li>
 * </ul>
 */
@Mixin(targets = CompatTargets.VSS_CLIENT_COLUMN_PROCESSOR)
public abstract class ClientColumnProcessorMixin {

    /**
     * 重定向 {@code seen.add(...)}：永远返回 true（视为首次见到），从而绕过那处致命断言。
     *
     * 注意仍然**真的调用** {@code seen.add}，让 {@code seen} 保持原有的去重语义 ——
     * 后面的 {@code seen.size()} / {@code seen.contains()} 校验因此不受影响。
     *
     * @param seen  目标方法里的去重集合
     * @param value 本次读到的 section Y（以 Integer 装箱传入）
     * @return 恒为 true
     */
    @Redirect(
            method = CompatTargets.VSS_METHOD_DECODE_COLUMN,
            at = @At(value = "INVOKE", target = CompatTargets.SET_ADD),
            require = 0)
    private boolean compatpatch$tolerateDuplicateSectionY(Set<Object> seen, Object value) {
        if (!CompatPatchConfig.fixVssDuplicateSection()) {
            // 开关关掉 → 保持原模组行为（重复仍然抛异常）
            return seen.add(value);
        }
        boolean firstTime = seen.add(value);
        if (!firstTime && value instanceof Integer sectionY) {
            // 显式记录，不静默吞掉（内部按次数节流）
            DuplicateSectionLog.record(sectionY);
        }
        return true;
    }
}
