package com.yourname.equipmentfusion.fusion;

import com.yourname.equipmentfusion.EquipmentFusionMod;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;

/**
 * Core fusion logic.
 *
 * Detection priority:
 * 1. Item tags (#minecraft:swords, #axes, #*_armor) → mod-compatible
 * 2. instanceof TridentItem / MaceItem → no vanilla tag, subclass detection
 * 3. isArmorLike() attribute fallback → horse armour etc.
 */
public class FusionLogic {

    public static boolean canFuse(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return false;
        if (left.getItem() != right.getItem()) return false;
        return getTargetAttribute(left) != null;
    }

    public static Holder<Attribute> getTargetAttribute(ItemStack stack) {
        if (isArmorLike(stack)) return Attributes.ARMOR;

        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES))
            return Attributes.ATTACK_DAMAGE;

        if (stack.getItem() instanceof TridentItem)
            return Attributes.ATTACK_DAMAGE;

        // Mace: only attack speed
        if (stack.getItem() instanceof MaceItem)
            return Attributes.ATTACK_SPEED;

        return null;
    }

    public static double calculateNewValue(double left, double right) {
        double gain = Math.max(1.0D, Math.floor(right * 0.5D));
        return left + gain;
    }

    private static boolean isArmorLike(ItemStack stack) {
        if (stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR)
            || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR))
            return true;
        for (var e : stack.getItem().getDefaultAttributeModifiers().modifiers()) {
            if (e.attribute().equals(Attributes.ARMOR) && e.modifier().amount() > 0) return true;
        }
        return false;
    }

    public static ItemStack performFusion(ItemStack left, ItemStack right) {
        Holder<Attribute> attribute = getTargetAttribute(left);
        if (attribute == null) return ItemStack.EMPTY;

        double ld = FusionAttributeHelper.getDisplayValue(left, attribute);
        double rd = FusionAttributeHelper.getDisplayValue(right, attribute);
        double nd = calculateNewValue(ld, rd);
        double nm = FusionAttributeHelper.toModifierValue(attribute, nd);

        ItemStack result = left.copy();
        result.setDamageValue(0);
        FusionAttributeHelper.setAttributeValue(result, attribute, nm);

        int lc = left.getOrDefault(EquipmentFusionMod.FUSION_COUNT.get(), 0);
        int rc = right.getOrDefault(EquipmentFusionMod.FUSION_COUNT.get(), 0);
        int nc = lc + rc + 1;
        result.set(EquipmentFusionMod.FUSION_COUNT.get(), nc);
        if (rc > 0) result.set(EquipmentFusionMod.IMPERFECT.get(), true);

        // Trident: secondary speed boost
        if (left.getItem() instanceof TridentItem) {
            double ls = FusionAttributeHelper.getDisplayValue(left, Attributes.ATTACK_SPEED);
            double rs = FusionAttributeHelper.getDisplayValue(right, Attributes.ATTACK_SPEED);
            FusionAttributeHelper.setAttributeValue(result, Attributes.ATTACK_SPEED,
                FusionAttributeHelper.toModifierValue(Attributes.ATTACK_SPEED, ls + rs * 0.5D));
        }

        // Axe: full attack inheritance + halved speed + halved durability
        if (left.is(ItemTags.AXES)) {
            // Full attack: left + right (no 0.5 coefficient)
            double axeDmg = FusionAttributeHelper.toModifierValue(Attributes.ATTACK_DAMAGE, ld + rd);
            FusionAttributeHelper.setAttributeValue(result, Attributes.ATTACK_DAMAGE, axeDmg);
            // Halve speed, min 0.1
            double ls = FusionAttributeHelper.getDisplayValue(left, Attributes.ATTACK_SPEED);
            double newSpeed = Math.max(0.1D, ls * 0.5D);
            FusionAttributeHelper.setAttributeValue(result, Attributes.ATTACK_SPEED,
                FusionAttributeHelper.toModifierValue(Attributes.ATTACK_SPEED, newSpeed));
            // Halve max durability, min 1
            int oldMax = left.getOrDefault(DataComponents.MAX_DAMAGE, 0);
            int newMax = oldMax > 0 ? Math.max(1, oldMax / 2) : 0;
            if (newMax > 0) result.set(DataComponents.MAX_DAMAGE, newMax);
            result.setDamageValue(0); // repair
        }

        // Armor toughness / knockback (gated)
        if (isArmorLike(left)) {
            double lt = FusionAttributeHelper.getDisplayValue(left, Attributes.ARMOR_TOUGHNESS);
            if (lt > 0.0D) {
                int tg = rc == 0 ? 1 : rc;
                FusionAttributeHelper.setAttributeValue(result, Attributes.ARMOR_TOUGHNESS,
                    FusionAttributeHelper.toModifierValue(Attributes.ARMOR_TOUGHNESS, lt + (double) tg));
            }
            double lk = FusionAttributeHelper.getDisplayValue(left, Attributes.KNOCKBACK_RESISTANCE);
            if (lk > 0.0D) {
                double rk = FusionAttributeHelper.getDisplayValue(right, Attributes.KNOCKBACK_RESISTANCE);
                FusionAttributeHelper.setAttributeValue(result, Attributes.KNOCKBACK_RESISTANCE,
                    FusionAttributeHelper.toModifierValue(Attributes.KNOCKBACK_RESISTANCE, calculateNewValue(lk, rk)));
            }
        }

        // Enchantment merge (uses vanilla APIs → mod-compatible)
        FusionEnchantHelper.mergeEnchantments(result, right);

        EquipmentFusionMod.LOGGER.debug("Fusion: {} + {} → display {}→{} count {}",
            left.getItem(), right.getItem(), ld, nd, nc);
        return result;
    }
}
