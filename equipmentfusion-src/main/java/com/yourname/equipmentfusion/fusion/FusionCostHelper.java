package com.yourname.equipmentfusion.fusion;

import com.yourname.equipmentfusion.EquipmentFusionMod;
import net.minecraft.world.item.ItemStack;

/**
 * Calculates the experience level cost for a fusion operation.
 *
 * <p>公式（按需求）：融合需要经验 =（武器从 0 修复到满耐久需要的经验）
 * ×（融合次数当前 10 的倍数（向下取整）+ 当前存在附魔数量）
 * + 附魔等级总数（融合结果上所有附魔的等级之和）。</p>
 *
 * <ul>
 *   <li><b>修复经验</b>：按原版铁砧规则等价计算——每件修复材料恢复
 *       「最大耐久 ÷ 4」点耐久、每材料 1 级；从 0 修满所需材料数即修复经验
 *       （多数武器 4~5 级，随武器最大耐久微变）；</li>
 *   <li><b>10 的倍数</b>：融合次数 ÷ 10 向下取整
 *       （5→0、8→0、10→1、19→1、20→2、25→2、30→3、40→4）；</li>
 *   <li><b>附魔数量</b>：融合结果（左右合并后）上的附魔总数；</li>
 *   <li><b>附魔等级总数</b>：融合结果上所有附魔的等级之和
 *       （如 锋利III + 耐久III + 火焰附加II = 8），在乘法之后加在末尾。</li>
 * </ul>
 */
public class FusionCostHelper {

    /**
     * Computes the anvil level cost for a fusion operation.
     *
     * @param left   主装备（被融合的武器，取其融合次数）
     * @param right  祭品装备（预留，当前公式未使用）
     * @param result 融合结果（用于统计合并后的附魔数量）
     * @return the experience level cost（最低 1 级，避免铁砧 0 费用）
     */
    public static int calculateCost(ItemStack left, ItemStack right, ItemStack result) {
        int repairLevels = getRepairLevelsFromZero(left);
        int fusionCount = left.getOrDefault(EquipmentFusionMod.FUSION_COUNT.get(), 0);
        int tens = fusionCount / 10; // 10 的倍数（向下取整）
        int enchantCount = result.getEnchantments().size();

        // 附魔等级总数：融合结果上所有附魔的等级之和（如 锋利III+耐久III = 5）
        int enchantLevels = 0;
        for (var entry : result.getEnchantments().entrySet()) {
            enchantLevels += entry.getIntValue();
        }

        // 经验 = 修复经验 × (10的倍数 + 附魔数量) + 附魔等级总数
        return Math.max(1, repairLevels * (tens + enchantCount) + enchantLevels);
    }

    /**
     * 原版铁砧等价：从 0 耐久修复到满耐久需要的经验（等级）。
     *
     * <p>每件修复材料恢复 {@code 最大耐久 ÷ 4} 点耐久（不足 4 时按 1），
     * 每消耗一件材料计 1 级；从 0 修满所需材料数即返回值。</p>
     */
    public static int getRepairLevelsFromZero(ItemStack stack) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 0;
        }
        int perMaterial = Math.max(1, maxDamage / 4);
        return (maxDamage + perMaterial - 1) / perMaterial;
    }
}
