package compatpatch.shim;

import compatpatch.CompatPatchConfig;
import compatpatch.CompatTargets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Wakes 的 {@code SplashPlaneParticle} 反射访问器。
 *
 * ── 为什么用反射 ──
 * Wakes 是 **Fabric 模组**（经 Sinytra Connector 加载）：
 * 磁盘上它的方法名是 intermediary（{@code method_3070}/{@code method_3085}），
 * 运行时被 Connector 重映射成 Mojang 名。到底哪个名字在运行时生效**编译期无法确定**，
 * 所以这里两个候选都试，并**显式记录**用的是哪个 —— 不静默吞掉。
 *
 * 只用反射不写死内部名，也避免了对 Fabric 内部 API 的编译期依赖。
 */
public final class WakesSplashPlaneAccess {

    /** 粒子持有主人实体用的私有字段名（该字段是模组自己的，不参与重映射）。 */
    private static final String FIELD_OWNER = "owner";

    /** 已经确认可用的字段/方法缓存（null 表示还没找到）。 */
    private static Field ownerField;
    private static boolean ownerFieldResolved;

    private static Method removeMethod;
    private static boolean removeMethodResolved;

    private WakesSplashPlaneAccess() {
    }

    /** 读出粒子当前的主人实体；读不到（字段不存在/无值）返回 null。 */
    public static Object ownerOf(Object particle) {
        if (!ownerFieldResolved) {
            ownerFieldResolved = true;
            try {
                ownerField = particle.getClass().getDeclaredField(FIELD_OWNER);
                ownerField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                // 显式说明：字段没了说明 Wakes 改了实现，本补丁会整体失效（不再静默）
                CompatPatchConfig.LOGGER.warn(
                        "Wakes 的 SplashPlaneParticle 里找不到字段 '{}' —— 该模组实现可能已变更，本项补丁不会生效",
                        FIELD_OWNER);
                return null;
            }
        }
        if (ownerField == null) {
            return null;
        }
        try {
            return ownerField.get(particle);
        } catch (IllegalAccessException e) {
            CompatPatchConfig.LOGGER.warn("读取 Wakes 粒子的 owner 字段失败", e);
            return null;
        }
    }

    /**
     * 清理（移除）该粒子：调用它自己的移除方法，这样实体上的 {@code wakeParticle} 引用也会被一并清掉。
     *
     * @return true 表示确实调用了移除方法
     */
    public static boolean removeParticle(Object particle) {
        if (!removeMethodResolved) {
            removeMethodResolved = true;
            for (String name : CompatTargets.WAKES_REMOVE_METHOD_CANDIDATES) {
                try {
                    removeMethod = particle.getClass().getMethod(name);
                    removeMethod.setAccessible(true);
                    CompatPatchConfig.LOGGER.info(
                            "Wakes 粒子的移除方法确认为 {}()（运行时映射名）", name);
                    break;
                } catch (NoSuchMethodException ignored) {
                    // 候选名不存在是正常情况（只会有其中一个生效），继续试下一个
                }
            }
            if (removeMethod == null) {
                CompatPatchConfig.LOGGER.warn(
                        "找不到 Wakes 粒子的移除方法（试过 {}）—— 无法清理残留水花面片",
                        String.join("/", CompatTargets.WAKES_REMOVE_METHOD_CANDIDATES));
                return false;
            }
        }
        try {
            removeMethod.invoke(particle);
            return true;
        } catch (ReflectiveOperationException e) {
            CompatPatchConfig.LOGGER.warn("调用 Wakes 粒子的移除方法失败", e);
            return false;
        }
    }

    /**
     * 用**世界真实流体状态**判断该实体附近还有没有水。
     *
     * 检查实体所在格与其下方一格：游泳时实体脚下就是水，水面游泳时位置可能落在水面之上，
     * 所以两格任一有水就算"还在水里"；两格都没水 = 水已经没了（该清理残留面片）。
     */
    public static boolean hasWaterNearby(Entity entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        return isWater(level.getFluidState(pos)) || isWater(level.getFluidState(pos.below()));
    }

    private static boolean isWater(FluidState state) {
        return state.is(FluidTags.WATER);
    }
}
