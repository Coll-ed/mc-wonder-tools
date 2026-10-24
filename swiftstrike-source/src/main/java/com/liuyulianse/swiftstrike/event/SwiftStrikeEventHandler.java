package com.liuyulianse.swiftstrike.event;

import com.liuyulianse.swiftstrike.SwiftStrikeMod;
import com.liuyulianse.swiftstrike.registry.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = SwiftStrikeMod.MODID)
public final class SwiftStrikeEventHandler {

    private static final float DAMAGE_PER_SPEED_PER_LEVEL = 8.0F;

    private SwiftStrikeEventHandler() {}

    @SubscribeEvent
    public static void onLivingDamage(final LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();

        // Trace back to the real attacker
        LivingEntity attacker = getAttacker(event.getSource().getEntity());
        if (attacker == null) return;

        // Read Swift Strike level from attacker's main hand
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty()) return;

        int level = getSwiftStrikeLevel(weapon, attacker);
        if (level <= 0) return;

        // Read attacker's current movement speed
        double speed = attacker.getAttributeValue(Attributes.MOVEMENT_SPEED);

        float bonusDamage = (float) (speed * level * DAMAGE_PER_SPEED_PER_LEVEL);
        event.setNewDamage(event.getNewDamage() + bonusDamage);
    }

    /**
     * Resolves the real attacker: Player for direct attacks, or the projectile's owner.
     */
    private static LivingEntity getAttacker(Entity sourceEntity) {
        if (sourceEntity instanceof LivingEntity living) {
            return living;
        }
        if (sourceEntity instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            return owner;
        }
        return null;
    }

    private static int getSwiftStrikeLevel(ItemStack stack, LivingEntity entity) {
        Holder<Enchantment> holder = entity.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(ModEnchantments.SWIFT_STRIKE);
        return stack.getEnchantmentLevel(holder);
    }
}
