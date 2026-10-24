package com.liuyulianse.vitalslayer.event;

import com.liuyulianse.vitalslayer.VitalSlayerMod;
import com.liuyulianse.vitalslayer.registry.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import java.util.Map;

/**
 * Lets ANY item receive the Percent Slayer enchantment via an anvil
 * when combined with a Percent Slayer enchanted book.
 * <p>
 * Left  = any item   →  Right = Percent Slayer book  →  Output = item + enchantment
 */
@EventBusSubscriber(modid = VitalSlayerMod.MODID)
public final class AnvilEventHandler {

    private AnvilEventHandler() {}

    @SubscribeEvent
    public static void onAnvilUpdate(final AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty() || right.isEmpty()) {
            return;
        }

        // Right slot must be an enchanted book
        if (!right.is(Items.ENCHANTED_BOOK)) {
            return;
        }

        // Extract the Percent Slayer level from the book
        int bookLevel = getPercentSlayerFromBook(right, event.getPlayer());
        if (bookLevel <= 0) {
            return;
        }

        // Build the output: copy of the left item with Percent Slayer applied
        ItemStack output = left.copy();

        Holder<Enchantment> holder = getHolder(event.getPlayer());
        int existingLevel = output.getEnchantmentLevel(holder);
        int finalLevel = Math.max(existingLevel, bookLevel);

        // Use DataComponents directly to bypass supported_items tag check,
        // so ANY item (stick, apple, dirt, etc.) can receive the enchantment.
        ItemEnchantments existing = output.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(existing);
        mutable.set(holder, finalLevel);
        output.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

        event.setOutput(output);
        event.setCost(5 + finalLevel * 2);
        event.setMaterialCost(1);
    }

    /**
     * Reads the Percent Slayer level from an enchanted book ItemStack.
     */
    private static int getPercentSlayerFromBook(ItemStack book, Player player) {
        ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        Holder<Enchantment> holder = getHolder(player);

        for (Map.Entry<Holder<Enchantment>, Integer> entry : stored.entrySet()) {
            if (entry.getKey().equals(holder)) {
                return entry.getValue();
            }
        }
        return 0;
    }

    private static Holder<Enchantment> getHolder(Player player) {
        return player.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(ModEnchantments.PERCENT_SLAYER);
    }
}
