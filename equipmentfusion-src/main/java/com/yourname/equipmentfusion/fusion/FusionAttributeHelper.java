package com.yourname.equipmentfusion.fusion;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes equipment attribute modifiers on ItemStacks.
 */
public class FusionAttributeHelper {

    static final ResourceLocation ATTACK_SPEED_ID =
        ResourceLocation.fromNamespaceAndPath("minecraft", "base_attack_speed");

    static final ResourceLocation ATTACK_DAMAGE_ID =
        ResourceLocation.fromNamespaceAndPath("minecraft", "base_attack_damage");

    static final ResourceLocation ARMOR_ID =
        ResourceLocation.fromNamespaceAndPath("minecraft", "base_armor");

    static final ResourceLocation ARMOR_TOUGHNESS_ID =
        ResourceLocation.fromNamespaceAndPath("minecraft", "armor_toughness");

    static final ResourceLocation KNOCKBACK_RESISTANCE_ID =
        ResourceLocation.fromNamespaceAndPath("minecraft", "knockback_resistance");

    public static ResourceLocation getModifierId(Holder<Attribute> attribute) {
        if (attribute.equals(Attributes.ATTACK_DAMAGE)) return ATTACK_DAMAGE_ID;
        if (attribute.equals(Attributes.ATTACK_SPEED)) return ATTACK_SPEED_ID;
        if (attribute.equals(Attributes.ARMOR)) return ARMOR_ID;
        if (attribute.equals(Attributes.ARMOR_TOUGHNESS)) return ARMOR_TOUGHNESS_ID;
        if (attribute.equals(Attributes.KNOCKBACK_RESISTANCE)) return KNOCKBACK_RESISTANCE_ID;
        return null;
    }

    /**
     * Modifier sum. Falls back to defaults if component has none.
     */
    public static double getAttributeValue(ItemStack stack, Holder<Attribute> attribute) {
        boolean found = false;
        double value = 0.0;
        for (var entry : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
            if (entry.attribute().equals(attribute)) { value += entry.modifier().amount(); found = true; }
        }
        if (!found) {
            for (var entry : stack.getItem().getDefaultAttributeModifiers().modifiers()) {
                if (entry.attribute().equals(attribute)) value += entry.modifier().amount();
            }
        }
        return value;
    }

    /**
     * In-game display value.
     * ATTACK_DAMAGE → +1.0, ATTACK_SPEED → +4.0, KNOCKBACK_RESISTANCE → ×100.
     */
    public static double getDisplayValue(ItemStack stack, Holder<Attribute> attribute) {
        double mod = getAttributeValue(stack, attribute);
        if (attribute.equals(Attributes.ATTACK_DAMAGE))      return mod + 1.0D;
        if (attribute.equals(Attributes.ATTACK_SPEED))        return mod + 4.0D;
        if (attribute.equals(Attributes.KNOCKBACK_RESISTANCE)) return mod * 100.0D;
        return mod;
    }

    /**
     * Reverse of getDisplayValue.
     */
    public static double toModifierValue(Holder<Attribute> attribute, double display) {
        if (attribute.equals(Attributes.ATTACK_DAMAGE))      return display - 1.0D;
        if (attribute.equals(Attributes.ATTACK_SPEED))        return display - 4.0D;
        if (attribute.equals(Attributes.KNOCKBACK_RESISTANCE)) return display / 100.0D;
        return display;
    }

    /**
     * Writes only the target modifier. Replaces in-place to preserve order.
     */
    public static void setAttributeValue(ItemStack stack, Holder<Attribute> attribute, double value) {
        ResourceLocation modifierId = getModifierId(attribute);

        List<ItemAttributeModifiers.Entry> existing = stack.getOrDefault(
            DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers();

        List<ItemAttributeModifiers.Entry> newEntries = new ArrayList<>();
        boolean replaced = false;
        EquipmentSlotGroup slot = EquipmentSlotGroup.ANY;

        for (var entry : existing) {
            if (entry.modifier().id().equals(modifierId) && entry.attribute().equals(attribute)) {
                slot = entry.slot();
                newEntries.add(new ItemAttributeModifiers.Entry(attribute,
                    new AttributeModifier(modifierId, value, AttributeModifier.Operation.ADD_VALUE), slot));
                replaced = true;
            } else {
                newEntries.add(entry);
            }
        }

        if (!replaced) {
            for (var entry : stack.getItem().getDefaultAttributeModifiers().modifiers()) {
                if (entry.modifier().id().equals(modifierId) && entry.attribute().equals(attribute)) {
                    slot = entry.slot();
                    break;
                }
            }
            newEntries.add(new ItemAttributeModifiers.Entry(attribute,
                new AttributeModifier(modifierId, value, AttributeModifier.Operation.ADD_VALUE), slot));
        }

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(newEntries, true));
    }
}
