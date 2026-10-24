package compatpatch.shim;

import compatpatch.CompatPatchConfig;
import compatpatch.CompatTargets;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ③ 的转发层：把「该扩展模组期望的 architectury {@code RegistrySupplier}」
 * 换成「你装的那版 Ice and Fire 里真实的 {@code DeferredItem}」。
 *
 * ── 为什么要反射 ──
 * 本补丁模组刻意**不对被修模组产生编译期依赖**：
 * 直接在代码里写 {@code IafItems.DRAGONSTEEL_FIRE_INGOT} 会让本模组在没有 Ice and Fire 时
 * 连类都加载不了。反射 + 逐字段缓存既保持零依赖，又只付出一次解析开销。
 *
 * ── 失败时的行为（不静默）──
 * 解析不出来就记 ERROR 并退回 {@link Items#AIR}：
 * 修复材料会失效（盔甲修不了），但绝不会崩游戏。
 */
public final class IafItemsShim {

    /** 解析失败时的兜底对象（空气：成分匹配不到东西，但不会崩）。 */
    private static final ItemLike FALLBACK = Items.AIR;

    /** 字段名 -> 已解析的 ItemLike。解析成功才入缓存。 */
    private static final Map<String, ItemLike> CACHE = new ConcurrentHashMap<>();

    /**
     * 用指定字段对应的物品构造修复材料。
     *
     * @param fieldName {@code IafItems} 里的字段名（如 {@code DRAGONSTEEL_FIRE_INGOT}）
     */
    public static Ingredient repairIngredient(String fieldName) {
        return Ingredient.of(resolve(fieldName));
    }

    /** 解析 {@code IafItems.<fieldName>} 指向的物品。 */
    public static ItemLike resolve(String fieldName) {
        ItemLike cached = CACHE.get(fieldName);
        if (cached != null) {
            return cached;
        }
        try {
            Class<?> iafItems = Class.forName(CompatTargets.IAF_ITEMS);
            Field field = iafItems.getField(fieldName);
            Object deferred = field.get(null);
            if (deferred == null) {
                throw new IllegalStateException("字段值为 null");
            }
            // DeferredHolder#get() —— 取真实物品
            Method get = deferred.getClass().getMethod("get");
            Object value = get.invoke(deferred);
            if (value instanceof ItemLike itemLike) {
                CACHE.put(fieldName, itemLike);
                return itemLike;
            }
            throw new IllegalStateException("get() 返回的不是 ItemLike: "
                    + (value == null ? "null" : value.getClass().getName()));
        } catch (Throwable t) {
            // 显式失败：说清楚哪个字段、退回什么、后果是什么
            CompatPatchConfig.LOGGER.error(
                    "无法把 {}.{} 解析成物品（该扩展模组期望的是 architectury RegistrySupplier，"
                            + "而实际字段类型不同）。修复材料将退回 {} —— 这些盔甲会修不了，但不会崩游戏。",
                    CompatTargets.IAF_ITEMS, fieldName, ((Item) FALLBACK).getDescriptionId(), t);
            return FALLBACK;
        }
    }

    private IafItemsShim() {
    }
}
