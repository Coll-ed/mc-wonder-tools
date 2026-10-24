package compatpatch.mixin.scguns;

import compatpatch.CompatPatchConfig;
import compatpatch.CompatTargets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ① 修 scguns 投掷物生成数据的空 ItemStack 崩服。
 *
 * ── 崩溃链（logs/latest.log L12297、L12459~L12485）──
 *   scguns 的 ThrowableChokeBombEntity 命中玩家 → Player.hurt
 *     → gytrinket DamageManager → ShieldHandler → ReflectShieldType.reflectProjectile
 *     → 用 EntityType.create(level) 新建同类型投掷物（**不复制 item**，item 保持 ItemStack.EMPTY）
 *     → ServerLevel.addFreshEntity → ServerEntity.sendPairingData
 *     → AdvancedAddEntityPayload → ThrowableItemEntity.writeSpawnData
 *     → ItemStack$2.encode 抛 EncoderException: Empty ItemStack not allowed → 崩服
 *
 * ── 为什么这样修是对的 ──
 * {@code ItemStack.STREAM_CODEC} 就是 {@code ItemStack$2}：
 * 它把 encode/decode 全部委托给 {@code OPTIONAL_STREAM_CODEC}，只在外面多一层非空断言
 * （encode 抛 EncoderException、decode 抛 DecoderException）。
 *
 * 因此把这里的字段换成 {@code OPTIONAL_STREAM_CODEC}：
 *   · 线格式完全不变 → 协议兼容、两端混用也不会错位；
 *   · 只是去掉那条断言 —— 而 ThrowableItemEntity 的 item 字段本来就允许为空
 *     （两个构造器都写 {@code this.item = ItemStack.EMPTY}，getItem 也不做非空保证）。
 *
 * 这是「让写入方接受自己字段本来就可能取到的值」，不是绕过检查。
 */
@Mixin(targets = CompatTargets.SCGUNS_THROWABLE_ITEM_ENTITY)
public abstract class ThrowableItemEntityMixin {

    /** 写生成数据时的编解码器。 */
    @Redirect(
            method = CompatTargets.SCGUNS_METHOD_WRITE_SPAWN_DATA,
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = CompatTargets.ITEM_STACK_STREAM_CODEC))
    private StreamCodec<RegistryFriendlyByteBuf, ItemStack> compatpatch$tolerantWriteCodec() {
        return compatpatch$codec();
    }

    /** 读生成数据时的编解码器 —— 必须与写入侧一致，否则格式错位。 */
    @Redirect(
            method = CompatTargets.SCGUNS_METHOD_READ_SPAWN_DATA,
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = CompatTargets.ITEM_STACK_STREAM_CODEC))
    private StreamCodec<RegistryFriendlyByteBuf, ItemStack> compatpatch$tolerantReadCodec() {
        return compatpatch$codec();
    }

    /** 关掉开关就返回原来的字段值（退回原模组行为）。 */
    @Unique
    private static StreamCodec<RegistryFriendlyByteBuf, ItemStack> compatpatch$codec() {
        return CompatPatchConfig.fixScgunsEmptyItemStackSpawnData()
                ? ItemStack.OPTIONAL_STREAM_CODEC
                : ItemStack.STREAM_CODEC;
    }
}
