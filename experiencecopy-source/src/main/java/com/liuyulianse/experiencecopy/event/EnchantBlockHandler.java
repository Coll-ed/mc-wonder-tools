package com.liuyulianse.experiencecopy.event;

import com.liuyulianse.experiencecopy.ExperienceCopyMod;
import com.liuyulianse.experiencecopy.util.ExpCopyUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;

/**
 * Blocks enchanting-table use for items tagged with Experience Copy.
 */
@EventBusSubscriber(modid = ExperienceCopyMod.MODID)
public final class EnchantBlockHandler {

    private EnchantBlockHandler() {}

    @SubscribeEvent
    public static void onGetEnchantmentLevel(final GetEnchantmentLevelEvent event) {
        if (ExpCopyUtil.hasExperienceCopyTag(event.getStack())) {
            // Set all enchantment levels to 0 → effectively blocks enchanting
            for (Holder<Enchantment> key : event.getEnchantments().keySet()) {
                event.getEnchantments().set(key, 0);
            }
        }
    }
}
