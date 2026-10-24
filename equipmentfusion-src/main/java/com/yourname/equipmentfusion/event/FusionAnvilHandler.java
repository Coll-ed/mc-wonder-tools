package com.yourname.equipmentfusion.event;

import com.yourname.equipmentfusion.EquipmentFusionMod;
import com.yourname.equipmentfusion.fusion.FusionCostHelper;
import com.yourname.equipmentfusion.fusion.FusionAttributeHelper;
import com.yourname.equipmentfusion.fusion.FusionLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Listens for AnvilUpdateEvent and ItemTooltipEvent.
 */
public class FusionAnvilHandler {

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (!FusionLogic.canFuse(left, right)) {
            return;
        }

        Holder<Attribute> attribute = FusionLogic.getTargetAttribute(left);
        if (attribute == null) {
            return;
        }

        double leftDisplay = FusionAttributeHelper.getDisplayValue(left, attribute);
        double rightDisplay = FusionAttributeHelper.getDisplayValue(right, attribute);
        double gain = FusionLogic.calculateNewValue(leftDisplay, rightDisplay) - leftDisplay;

        ItemStack result = FusionLogic.performFusion(left, right);
        if (result.isEmpty()) {
            return;
        }

        // 经验需求 = 修复经验(0→满) × (融合次数÷10向下取整 + 结果附魔数) + 结果附魔等级总和
        int cost = FusionCostHelper.calculateCost(left, right, result);

        event.setOutput(result);
        event.setCost(cost);
        event.setMaterialCost(1);

        EquipmentFusionMod.LOGGER.debug(
            "[EquipmentFusion] Anvil fusion: {} + {} → cost={}, display {}→{}",
            left.getItem(), right.getItem(), cost, leftDisplay, leftDisplay + gain
        );
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Integer count = stack.get(EquipmentFusionMod.FUSION_COUNT.get());
        if (count != null && count > 0) {
            event.getToolTip().add(
                Component.translatable("tooltip.equipmentfusion.fusion_count", count)
                    .withStyle(ChatFormatting.DARK_PURPLE)
            );
        }
        Boolean imperfect = stack.get(EquipmentFusionMod.IMPERFECT.get());
        if (imperfect != null && imperfect) {
            event.getToolTip().add(
                Component.translatable("tooltip.equipmentfusion.imperfect")
                    .withStyle(ChatFormatting.RED)
            );
        }
    }
}
