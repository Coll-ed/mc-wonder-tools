package compatpatch.mixin.jupiter;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import compatpatch.CompatPatchConfig;
import compatpatch.CompatTargets;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

/**
 * ④ 修 Jupiter 配置项值为 null 时的 NPE。
 *
 * ── 现象（logs/latest.log L3430~L3451，13 个键 × 客户端/服务端各一次 = 26 行）──
 *   [Jupiter/ERROR] Cannot load key=Music Volume (default: 1.0), type=java.lang.Double
 *                   in config=witherreincarnated:CLIENT
 *   java.lang.NullPointerException: Cannot invoke "Object.getClass()" because "value" is null
 *     at NightConfigHolder.processEntry(NightConfigHolder.java:157)
 *
 * ── 原因 ──
 * processEntry 一开头就是：
 *   if (type.isAssignableFrom(value.getClass()) && type.isAssignableFrom(defaultValue.getClass())) { ... }
 * 而 value 是 **配置文件里的实际值**。同一份日志更早处可以看到根由：
 *   [ModConfigSpec/CORE]: Configuration ... is not correct. Correcting
 *   [ModConfigSpec/CORE]: Incorrect key Music Volume (default: 1 was corrected from ... to its default, null
 * 即 NeoForge 认为这些键的类型不对，把它们「校正」成了 null。Jupiter 拿到 null 后直接 getClass() → NPE。
 *
 * ── 为什么这样修是零语义变更 ──
 * 原实现里「条件不成立」的分支（字节码偏移 70）就是**什么都不做、直接返回**
 * （既不建 builder，也不 set 引用）。所以值为 null 时跳过 ≡ 作者本意，
 * 只是把「抛 NPE」换成「按类型不符处理」。
 */
@Mixin(targets = CompatTargets.JUPITER_NIGHT_CONFIG_HOLDER)
public abstract class NightConfigHolderMixin {

    /**
     * 参数必须与 {@code processEntry} 完全对齐（末尾追加 CallbackInfo）：
     * (AtomicReference, CommentedConfig, Component, UnmodifiableConfig$Entry,
     *  Object value, Object defaultValue, Class type, BiFunction factory)
     */
    @Inject(method = CompatTargets.JUPITER_METHOD_PROCESS_ENTRY, at = @At("HEAD"), cancellable = true)
    private void compatpatch$skipNullValues(AtomicReference<?> builderRef,
                                            CommentedConfig config,
                                            Component name,
                                            UnmodifiableConfig.Entry entry,
                                            Object value,
                                            Object defaultValue,
                                            Class<?> type,
                                            BiFunction<?, ?, ?> builderFactory,
                                            CallbackInfo callback) {
        if (!CompatPatchConfig.fixJupiterNullConfigEntry()) {
            return;
        }
        if (type == null || value == null || defaultValue == null) {
            // 与「类型不符」同样的处理：不建 builder、不设置引用，安静跳过这一项
            callback.cancel();
        }
    }
}
