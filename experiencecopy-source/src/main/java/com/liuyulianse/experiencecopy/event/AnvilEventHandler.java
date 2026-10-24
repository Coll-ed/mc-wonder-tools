package com.liuyulianse.experiencecopy.event;

import com.liuyulianse.experiencecopy.ExperienceCopyMod;
import com.liuyulianse.experiencecopy.component.ExperienceCopyData;
import com.liuyulianse.experiencecopy.util.ExpCopyUtil;
import com.liuyulianse.experiencecopy.util.XpCostUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * Anvil logic for experience copy.
 * <p>
 * <b>Initial copy:</b> Bottles (left) + copyable tool (right).<br>
 * Required bottles = 1 (base) + sum of all enchantment levels.<br>
 * Output: clean copy with only enchantments preserved, durability = 1.
 * <p>
 * <b>Upgrade:</b> copy-tagged item (left) + Bottles (right).<br>
 * Required bottles = sum of enchantment levels on the copy.<br>
 * Each step increases durability (1→20%→40%→…→100%).
 */
@EventBusSubscriber(modid = ExperienceCopyMod.MODID)
public final class AnvilEventHandler {

    private AnvilEventHandler() {}

    @SubscribeEvent
    public static void onAnvilUpdate(final AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        // Try each handler; use a flag so the block guard doesn't override
        boolean handled = false;
        handled |= handleInitialCopy(event, left, right);
        handled |= handleUpgrade(event, left, right);

        // Only block if neither handler took the operation and a copy-tagged item is present
        if (!handled && (ExpCopyUtil.hasExperienceCopyTag(left) || ExpCopyUtil.hasExperienceCopyTag(right))) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
        }
    }

    private static boolean handleInitialCopy(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        if (!left.is(Items.EXPERIENCE_BOTTLE)) return false;
        if (!ExpCopyUtil.isCopyableToolOrWeapon(right)) return false;

        int required = ExpCopyUtil.bottlesForCopy(right);
        if (left.getCount() < required) return false;

        ItemStack result = ExpCopyUtil.createInitialCopy(right);
        int cost = XpCostUtil.calculateLevelCost(1, right.getMaxDamage());

        event.setOutput(result);
        event.setCost(cost);
        event.setMaterialCost(0);
        return true;
    }

    private static boolean handleUpgrade(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        if (!ExpCopyUtil.hasExperienceCopyTag(left)) return false;
        if (!right.is(Items.EXPERIENCE_BOTTLE)) return false;

        ExperienceCopyData data = ExpCopyUtil.getExperienceCopyData(left);
        if (data == null || data.stage() >= 5) return false;

        int required = ExpCopyUtil.bottlesForUpgrade(left);
        if (right.getCount() < required) return false;

        ItemStack result = ExpCopyUtil.upgradeCopy(left);
        int currentMax = left.getMaxDamage();
        int targetMax = result.getMaxDamage();
        int cost = XpCostUtil.calculateLevelCost(currentMax, targetMax);

        event.setOutput(result);
        event.setCost(cost);
        event.setMaterialCost(required);
        return true;
    }
}
