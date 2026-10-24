package com.liuyulianse.experiencecopy.util;

import com.liuyulianse.experiencecopy.component.ExperienceCopyData;
import com.liuyulianse.experiencecopy.registry.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Central utility for experience copy logic.
 */
public final class ExpCopyUtil {

    public static final TagKey<Item> EXPERIENCE_COPYABLE =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("experiencecopy", "experience_copyable"));

    private ExpCopyUtil() {}

    // ── Tag helpers ──

    public static boolean hasExperienceCopyTag(ItemStack stack) {
        return stack.has(ModDataComponents.EXPERIENCE_COPY.get());
    }

    public static ExperienceCopyData getExperienceCopyData(ItemStack stack) {
        return stack.get(ModDataComponents.EXPERIENCE_COPY.get());
    }

    public static boolean isCopyableToolOrWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isDamageableItem()) return false;
        if (stack.getMaxDamage() <= 1) return false;
        if (hasExperienceCopyTag(stack)) return false;
        return stack.is(EXPERIENCE_COPYABLE);
    }

    // ── Bottle cost calculation ──

    /**
     * Returns the total enchantment level sum on the item.
     */
    public static int getTotalEnchantmentLevels(ItemStack stack) {
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int total = 0;
        for (var entry : enchants.entrySet()) {
            total += entry.getIntValue();
        }
        return total;
    }

    /**
     * Bottles required for initial copy: 1 (base) + sum of enchantment levels.
     * Capped at 64 (max stack size).
     */
    public static int bottlesForCopy(ItemStack tool) {
        return Math.min(1 + getTotalEnchantmentLevels(tool), 64);
    }

    /**
     * Bottles required for each durability upgrade step: sum of enchantment levels
     * on the copy-tagged item. Capped at 64 (max stack size).
     */
    public static int bottlesForUpgrade(ItemStack copyItem) {
        return Math.min(getTotalEnchantmentLevels(copyItem), 64);
    }

    // ── Create initial copy ──

    /**
     * Creates the initial copy. Only enchantments are preserved;
     * all other data components (name, lore, attributes, modded modifiers) are stripped.
     */
    public static ItemStack createInitialCopy(ItemStack source) {
        int originalMax = source.getMaxDamage();
        boolean hadCustomMaxDamage = source.has(DataComponents.MAX_DAMAGE);

        // Start with a clean stack of the same item type
        ItemStack copy = new ItemStack(source.getItem(), 1);

        // Only keep enchantments — strip everything else
        ItemEnchantments enchants = source.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!enchants.isEmpty()) {
            copy.set(DataComponents.ENCHANTMENTS, enchants);
        }

        copy.set(DataComponents.MAX_DAMAGE, 1);
        copy.set(DataComponents.DAMAGE, 0);

        copy.set(ModDataComponents.EXPERIENCE_COPY.get(),
                new ExperienceCopyData(originalMax, 0, hadCustomMaxDamage));

        return copy;
    }

    // ── Upgrade an existing copy ──

    public static ItemStack upgradeCopy(ItemStack stack) {
        ExperienceCopyData data = getExperienceCopyData(stack);
        if (data == null) return stack;

        ItemStack result = stack.copyWithCount(1);

        int originalMax = data.originalMaxDamage();
        int nextStage = data.stage() + 1;
        int targetMax = DurabilityUtil.calculateStageMaxDamage(originalMax, nextStage);

        result.set(DataComponents.MAX_DAMAGE, targetMax);
        result.set(DataComponents.DAMAGE, 0);

        if (nextStage >= 5 || targetMax >= originalMax) {
            finishExperienceCopy(result, data);
        } else {
            result.set(ModDataComponents.EXPERIENCE_COPY.get(),
                    new ExperienceCopyData(originalMax, nextStage, data.originalHadCustomMaxDamage()));
        }

        return result;
    }

    // ── Finish the copy ──

    public static void finishExperienceCopy(ItemStack stack, ExperienceCopyData data) {
        int originalMax = data.originalMaxDamage();

        if (data.originalHadCustomMaxDamage()) {
            stack.set(DataComponents.MAX_DAMAGE, originalMax);
        } else {
            stack.remove(DataComponents.MAX_DAMAGE);
        }

        stack.set(DataComponents.DAMAGE, 0);
        stack.remove(ModDataComponents.EXPERIENCE_COPY.get());
    }
}
