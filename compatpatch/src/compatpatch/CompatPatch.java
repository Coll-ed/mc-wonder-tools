package compatpatch;

import net.neoforged.fml.common.Mod;

/**
 * Compat Patch —— 独立兼容补丁模组。
 *
 * 设计原则：**不改任何原模组 jar**。
 * 全部修复通过 mixin 挂到目标模组的类上，并且：
 *   · 用 {@code @Mixin(targets = "字符串类名")}，不对被修模组产生任何编译期依赖；
 *   · 每个目标模组一个独立的 mixin 配置（{@code required=false}），
 *     目标模组不存在时只记一条警告，不会让游戏起不来；
 *   · 每项修复都有独立开关（见 {@link CompatPatchConfig}）。
 */
@Mod(CompatPatch.MOD_ID)
public class CompatPatch {

    public static final String MOD_ID = "compatpatch";

    public CompatPatch() {
        CompatPatchConfig.load();
        CompatPatchConfig.LOGGER.info("Compat Patch 已加载（{}）", CompatPatchConfig.describe());
    }
}
