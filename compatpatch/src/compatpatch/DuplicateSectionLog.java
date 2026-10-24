package compatpatch;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ② 的计数与节流日志。
 *
 * 客户端解码柱子时被容错掉的「重复 section Y」在这里计数。
 * 目的有两个：
 * <ol>
 *   <li><b>不静默吞掉</b>：这是上游真的产生了重复数据，必须留下可查的痕迹；</li>
 *   <li><b>不刷屏</b>：实测一条会话里这条曾出现 445 次（每次都带完整堆栈的 ERROR），
 *       全量打印反而把其它信息埋掉。</li>
 * </ol>
 *
 * 解码跑在线程池上（{@code ClientColumnProcessor} 的 executor），所以计数必须是原子的。
 */
public final class DuplicateSectionLog {

    /**
     * 第一次一定打印；之后每 {@value #LOG_EVERY} 次打印一条带累计数的摘要。
     *
     * 取 64 的理由：实测单次会话约 445 次，64 能把日志压到 ~7 条
     * （既看得出「一直在发生」，又不至于淹没日志）；同时 64 是 2 的幂，
     * 用位与判定的成本可以忽略。
     */
    private static final long LOG_EVERY = 64L;

    private static final AtomicLong TOLERATED = new AtomicLong();

    private DuplicateSectionLog() {
    }

    /**
     * 记一次「重复 section Y 被容错」。
     *
     * @param sectionY 重复出现的 section Y
     */
    public static void record(int sectionY) {
        long total = TOLERATED.incrementAndGet();
        if (total == 1L || total % LOG_EVERY == 0L) {
            CompatPatchConfig.LOGGER.warn(
                    "VSS 柱子里有重复的 section Y={}：已容错继续解码（不再丢弃整根柱子）。"
                            + "累计 {} 次。修法见 CompatPatchConfig 里 fixVssDuplicateSection 的说明；"
                            + "若想恢复原行为把它设为 false。",
                    sectionY, total);
        }
    }

    /** 当前累计次数（供启动横幅/诊断用）。 */
    public static long toleratedCount() {
        return TOLERATED.get();
    }
}
