package com.liuyulianse.vitalslayer.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Holds ResourceKey references for custom enchantments.
 * Enchantment data is defined in data-driven JSON files.
 */
public final class ModEnchantments {

    public static final ResourceKey<Enchantment> PERCENT_SLAYER =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath("vitalslayer", "percent_slayer")
            );

    private ModEnchantments() {}
}
