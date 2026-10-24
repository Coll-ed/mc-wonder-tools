package com.liuyulianse.vitalslayer.event;

import com.liuyulianse.vitalslayer.VitalSlayerMod;
import com.liuyulianse.vitalslayer.registry.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * 百分比杀伤核心：每击必触发的百分比伤害。
 * <p>
 * 公式：附加伤害 = 等级 × 目标最大生命 × 2%（1级2% → 5级10%）。
 * 应用于所有 LivingEntity 目标，主手武器携带本附魔即生效。
 */
@EventBusSubscriber(modid = VitalSlayerMod.MODID)
public final class PercentSlayerEventHandler {

    private static final float DAMAGE_PERCENT_PER_LEVEL = 0.02F;

    private PercentSlayerEventHandler() {}

    @SubscribeEvent
    public static void onLivingDamage(final LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();

        // Identify the attacker from the damage source
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }

        // Only main-hand items trigger the effect
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty()) {
            return;
        }

        // Read the enchantment level on the weapon
        int level = getPercentSlayerLevel(weapon, target);
        if (level <= 0) {
            return;
        }

        float bonusDamage = level * target.getMaxHealth() * DAMAGE_PERCENT_PER_LEVEL;
        event.setNewDamage(event.getNewDamage() + bonusDamage);
    }

    /**
     * Reads the Percent Slayer enchantment level from an ItemStack.
     *
     * @param stack  the item to inspect
     * @param entity any living entity (used for registry access)
     * @return enchantment level, or 0 if absent
     */
    private static int getPercentSlayerLevel(ItemStack stack, Entity entity) {
        Holder<Enchantment> holder = entity.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(ModEnchantments.PERCENT_SLAYER);
        return stack.getEnchantmentLevel(holder);
    }
}
