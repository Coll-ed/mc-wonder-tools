package com.yourname.equipmentfusion.fusion;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Merges enchantments from the sacrifice onto the result.
 * Uses vanilla Enchantment APIs so mods altering enchant rules (mixin) are respected.
 */
public class FusionEnchantHelper {

    /**
     * Cases:
     * 1. Both empty → nothing
     * 2. Left only → already inherited via copy
     * 3. Right only → inherit all
     * 4. Both → selective upgrade/merge with compatibility checks
     */
    public static void mergeEnchantments(ItemStack result, ItemStack sacrifice) {
        ItemEnchantments rightEnch = sacrifice.getEnchantments();
        if (rightEnch.isEmpty()) return;

        ItemEnchantments leftEnch = result.getEnchantments();
        if (leftEnch.isEmpty()) {
            EnchantmentHelper.setEnchantments(result, rightEnch);
            return;
        }

        // Both: selectively merge
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(leftEnch);

        for (var entry : rightEnch.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int rlvl = entry.getIntValue();

            if (leftEnch.keySet().contains(holder)) {
                int llvl = mutable.getLevel(holder);
                if (rlvl > llvl) {
                    mutable.set(holder, rlvl);
                } else if (rlvl == llvl) {
                    mutable.set(holder, llvl + 1);
                }
            } else {
                if (EnchantmentHelper.isEnchantmentCompatible(mutable.keySet(), holder)) {
                    mutable.set(holder, rlvl);
                }
            }
        }

        EnchantmentHelper.setEnchantments(result, mutable.toImmutable());
    }
}
