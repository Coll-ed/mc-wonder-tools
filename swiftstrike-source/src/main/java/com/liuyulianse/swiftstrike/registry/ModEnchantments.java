package com.liuyulianse.swiftstrike.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantments {

    public static final ResourceKey<Enchantment> SWIFT_STRIKE =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath("swiftstrike", "swift_strike")
            );

    private ModEnchantments() {}
}
