package com.liuyulianse.experiencecopy.event;

import com.liuyulianse.experiencecopy.ExperienceCopyMod;
import com.liuyulianse.experiencecopy.component.ExperienceCopyData;
import com.liuyulianse.experiencecopy.util.ExpCopyUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * Tooltip for items with the Experience Copy component.
 */
@EventBusSubscriber(modid = ExperienceCopyMod.MODID)
public final class TooltipHandler {

    private TooltipHandler() {}

    @SubscribeEvent
    public static void onItemTooltip(final ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!ExpCopyUtil.hasExperienceCopyTag(stack)) return;

        ExperienceCopyData data = ExpCopyUtil.getExperienceCopyData(stack);
        if (data == null) return;

        int needed = ExpCopyUtil.bottlesForUpgrade(stack);
        List<Component> tooltip = event.getToolTip();

        tooltip.add(Component.translatable("tooltip.experiencecopy.copy_tag")
                .withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable("tooltip.experiencecopy.original_max",
                        data.originalMaxDamage())
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("tooltip.experiencecopy.progress",
                        data.stage())
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("tooltip.experiencecopy.current_max",
                        stack.getMaxDamage(), data.originalMaxDamage())
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("tooltip.experiencecopy.bottles_needed", needed)
                .withStyle(ChatFormatting.GOLD));

        tooltip.add(Component.translatable("tooltip.experiencecopy.no_enchant")
                .withStyle(ChatFormatting.RED));
    }
}
