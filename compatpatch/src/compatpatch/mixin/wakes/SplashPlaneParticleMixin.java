package compatpatch.mixin.wakes;

import compatpatch.CompatPatchConfig;
import compatpatch.CompatTargets;
import compatpatch.shim.WakesSplashPlaneAccess;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wakes 残留水花面片的兜底清理。
 *
 * ── 现象（用户实测 + 截图确认）──
 * 水源消失后，如果当时有实体在触发游泳动画，草地上会**永久留下**几片扁平的"破浪"面片
 * （浅紫白底 + 蓝色碎块 + 一道深色斜线，即 {@code splash_plane_animation.png}），
 * 周围已经一点水都没有。
 *
 * ── 原因（读字节码确认）──
 * {@code SplashPlaneParticle} 的每帧判定是：
 * <pre>
 *   if (!config.renderSplashPlane) die();
 *   if (owner == null) die();
 *   keep = wake.onWaterSurface()                       // ① 用的是**缓存的标志位**
 *       && !(玩家 &amp;&amp; 鞘翅滑翔)
 *       && owner.getDeltaMovement().horizontalDistance() >= 0.01;
 *   if (keep) aliveTick(wake); else die();
 * </pre>
 * 两个漏洞：
 * <ol>
 *   <li><b>它从不调用基类 tick 做 aging，没有任何寿命上限</b> —— 唯一的消失途径就是那条 die()</li>
 *   <li><b>它从不复查自己/主人脚下还是不是水</b> —— 只看缓存标志位。
 *       水被挖掉后该标志与真实地形脱节（尤其主人不再 tick 时标志会冻结在旧值），
 *       判定就一直通过 ⇒ 面片冻结在最后位置永不消失</li>
 * </ol>
 *
 * ── 修法（只加保险，不改原效果）──
 * 在 tick 的最前面加两道**独立于缓存标志**的检查：
 * <ol>
 *   <li>主人已 {@code isRemoved()}/{@code !isAlive()}，或已经不是实体 → 立刻清理</li>
 *   <li>用**世界真实流体状态**复查主人所在格与其下方一格；连续
 *       {@value #MAX_TICKS_WITHOUT_WATER} tick 都没有水 → 清理</li>
 * </ol>
 * 只有"确实没水了"才会清理，所以正常游玩时（脚下就是水）行为完全不变。
 *
 * ── 关于方法名 ──
 * Wakes 是 Fabric 模组（经 Connector 加载），运行时方法名无法在编译期确定，
 * 所以对 {@code tick} 与 intermediary 名各挂一个注入器（都 {@code require = 0}，
 * 不匹配只留 Mixin 警告，不影响游戏启动）。
 */
@Mixin(targets = CompatTargets.WAKES_SPLASH_PLANE_PARTICLE)
public abstract class SplashPlaneParticleMixin {

    /**
     * 连续多少 tick 检不到水才清理残留面片。
     *
     * 取 20（= 1 秒）的理由：给"水只是短暂更新/被流动水替换"的情况留出恢复余量，
     * 同时保证真正被挖掉的水在 1 秒内就清干净，不会有肉眼可见的残留。
     */
    @Unique
    private static final int MAX_TICKS_WITHOUT_WATER = 20;

    /** 连续检不到水的 tick 计数（每个粒子各自持有）。 */
    @Unique
    private int compatpatch$ticksWithoutWater;

    /** 防重入：清理时调用的移除方法内部可能再次触发本注入器。 */
    @Unique
    private boolean compatpatch$removing;

    /** 运行时映射名（Mojang 名）。 */
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void compatpatch$guardTick(CallbackInfo callback) {
        compatpatch$guardSplashPlane();
    }

    /** intermediary 名（Connector 若未重映射则用这个）。 */
    @Inject(method = "method_3070", at = @At("HEAD"), require = 0)
    private void compatpatch$guardTickIntermediary(CallbackInfo callback) {
        compatpatch$guardSplashPlane();
    }

    /** 两个注入器共用同一套判定。 */
    @Unique
    private void compatpatch$guardSplashPlane() {
        if (!CompatPatchConfig.fixWakesSplashPlane() || compatpatch$removing) {
            return;
        }
        Object owner = WakesSplashPlaneAccess.ownerOf(this);
        if (owner == null) {
            // 没有主人时原实现自己会 die()，这里不插手
            return;
        }
        if (!(owner instanceof Entity entity) || entity.isRemoved() || !entity.isAlive()) {
            CompatPatchConfig.LOGGER.debug("Wakes 水花面片的主人已失效，清理残留面片");
            compatpatch$killSplashPlane();
            return;
        }
        if (WakesSplashPlaneAccess.hasWaterNearby(entity)) {
            compatpatch$ticksWithoutWater = 0;
            return;
        }
        if (++compatpatch$ticksWithoutWater >= MAX_TICKS_WITHOUT_WATER) {
            // 显式记录：这正是用户反馈的"水没了还留着破浪"场景
            CompatPatchConfig.LOGGER.info(
                    "Wakes 水花面片的主人周围已连续 {} tick 没有水，清理这片残留水花",
                    compatpatch$ticksWithoutWater);
            compatpatch$killSplashPlane();
        }
    }

    @Unique
    private void compatpatch$killSplashPlane() {
        compatpatch$removing = true;
        try {
            WakesSplashPlaneAccess.removeParticle(this);
        } finally {
            compatpatch$removing = false;
        }
    }
}
