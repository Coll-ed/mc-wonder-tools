package compatpatch.mixin.iafspellbooks;

import compatpatch.CompatPatchConfig;
import compatpatch.CompatTargets;
import compatpatch.shim.IafItemsShim;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ③ 修 {@code ice_and_fire_spellbooks} 与 Ice and Fire 之间的**字段类型不匹配**。
 *
 * ── 报错（logs/latest.log L9026~L9032，15 次）──
 *   [EMI/ERROR] Exception thrown registering repair recipes
 *   java.lang.NoSuchFieldError: Class com.iafenvoy.iceandfire.registry.IafItems does not have member field
 *     'dev.architectury.registry.registries.RegistrySupplier DRAGONSTEEL_FIRE_INGOT'
 *     at ArmorMaterialRegistries.lambda$static$0(ArmorMaterialRegistries.java:35)
 *     at emi.VanillaPlugin.addRepair(VanillaPlugin.java:497)
 *
 * ── 根因 ──
 * NoSuchFieldError 的报文里带着**期望描述符**：JVM 按「名字 + 描述符」解析字段。
 * 字段名 {@code DRAGONSTEEL_FIRE_INGOT} 存在，但类型不同：
 *   期望（该扩展模组编译时，Architectury 版 Ice and Fire）：RegistrySupplier
 *   实际（你装的 iceandfire-2.1.3，iafenvoy NeoForge 移植）：DeferredItem
 *
 * ── 精确范围（已逐字节确认）──
 * 全模组 42 个类里只有 2 个含该描述符，真正引用 {@code IafItems} 的只有
 * {@code ArmorMaterialRegistries}：**6 个 lambda**，形状完全一致：
 * <pre>
 *   return Ingredient.of(new ItemLike[]{ IafItems.&lt;字段&gt;.get() });
 * </pre>
 *   字段 DRAGONSTEEL_FIRE_INGOT      → lambda$static$0 / lambda$static$6
 *   字段 DRAGONSTEEL_ICE_INGOT       → lambda$static$2 / lambda$static$8
 *   字段 DRAGONSTEEL_LIGHTNING_INGOT → lambda$static$4 / lambda$static$10
 *
 * ── 修法 ──
 * 在这 6 个 lambda 的 HEAD 注入并直接给出返回值：
 * 用反射从真实的 {@code IafItems} 字段取物品再 {@code Ingredient.of(...)}。
 *
 * 用 {@link Inject}(HEAD + cancellable) 而不是 {@code @Overwrite}：
 * 原方法体留在那里但永远执行不到（字段引用是惰性解析的，不执行就不会抛），
 * 这样即使该模组以后重新编译、lambda 编号变了，也只是注入点找不到（required=false 只警告），
 * 不会因为"替换掉了一个形状已经变了的方法体"而产生静默错误。
 */
@Mixin(targets = CompatTargets.IAF_SPELLBOOKS_ARMOR_MATERIAL_REGISTRIES)
public abstract class ArmorMaterialRegistriesMixin {

    @Inject(method = "lambda$static$0", at = @At("HEAD"), cancellable = true)
    private static void compatpatch$fireRepairA(CallbackInfoReturnable<Ingredient> callback) {
        compatpatch$substitute(callback, CompatTargets.IAF_FIELD_DRAGONSTEEL_FIRE_INGOT);
    }

    @Inject(method = "lambda$static$6", at = @At("HEAD"), cancellable = true)
    private static void compatpatch$fireRepairB(CallbackInfoReturnable<Ingredient> callback) {
        compatpatch$substitute(callback, CompatTargets.IAF_FIELD_DRAGONSTEEL_FIRE_INGOT);
    }

    @Inject(method = "lambda$static$2", at = @At("HEAD"), cancellable = true)
    private static void compatpatch$iceRepairA(CallbackInfoReturnable<Ingredient> callback) {
        compatpatch$substitute(callback, CompatTargets.IAF_FIELD_DRAGONSTEEL_ICE_INGOT);
    }

    @Inject(method = "lambda$static$8", at = @At("HEAD"), cancellable = true)
    private static void compatpatch$iceRepairB(CallbackInfoReturnable<Ingredient> callback) {
        compatpatch$substitute(callback, CompatTargets.IAF_FIELD_DRAGONSTEEL_ICE_INGOT);
    }

    @Inject(method = "lambda$static$4", at = @At("HEAD"), cancellable = true)
    private static void compatpatch$lightningRepairA(CallbackInfoReturnable<Ingredient> callback) {
        compatpatch$substitute(callback, CompatTargets.IAF_FIELD_DRAGONSTEEL_LIGHTNING_INGOT);
    }

    @Inject(method = "lambda$static$10", at = @At("HEAD"), cancellable = true)
    private static void compatpatch$lightningRepairB(CallbackInfoReturnable<Ingredient> callback) {
        compatpatch$substitute(callback, CompatTargets.IAF_FIELD_DRAGONSTEEL_LIGHTNING_INGOT);
    }

    /** 开关关掉时不动，让原逻辑照原样跑（并照原样报错）。 */
    private static void compatpatch$substitute(CallbackInfoReturnable<Ingredient> callback, String fieldName) {
        if (!CompatPatchConfig.fixIceAndFireSpellbooksIafItems()) {
            return;
        }
        callback.setReturnValue(IafItemsShim.repairIngredient(fieldName));
    }
}
