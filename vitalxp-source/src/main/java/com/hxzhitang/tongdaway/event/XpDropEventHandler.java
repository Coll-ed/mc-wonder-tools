package com.hxzhitang.tongdaway.event;

import com.hxzhitang.tongdaway.VitalXpMod;
import com.hxzhitang.tongdaway.config.VitalXpConfig;
import com.hxzhitang.tongdaway.util.XpCalculator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

@EventBusSubscriber(modid = VitalXpMod.MODID)
public final class XpDropEventHandler {
    private XpDropEventHandler() {}

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (!VitalXpConfig.ENABLED.get()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        Player player = event.getAttackingPlayer();

        if (VitalXpConfig.PLAYER_KILL_ONLY.get() && player == null) {
            return;
        }

        int baseXp = event.getDroppedExperience();

        if (baseXp <= 0) {
            if (!VitalXpConfig.ALLOW_ZERO_BASE_XP_BONUS.get()) {
                return;
            }
            baseXp = VitalXpConfig.ZERO_BASE_XP_VALUE.get();
        }

        ResourceLocation entityId = entity.getType().builtInRegistryHolder().key().location();

        if (isBlockedByFilter(entityId)) {
            return;
        }

        float maxHealth = entity.getMaxHealth();

        int finalXp = XpCalculator.calculateXp(
                baseXp,
                maxHealth,
                VitalXpConfig.HEALTH_MULTIPLIER.get(),
                VitalXpConfig.getRoundingMode(),
                VitalXpConfig.ENABLE_MULTIPLIER_CAP.get(),
                VitalXpConfig.MAX_MULTIPLIER.get(),
                VitalXpConfig.ENABLE_FINAL_XP_CAP.get(),
                VitalXpConfig.MAX_FINAL_XP.get()
        );

        event.setDroppedExperience(finalXp);
    }

    private static boolean isBlockedByFilter(ResourceLocation entityId) {
        String id = entityId.toString();
        String mode = VitalXpConfig.FILTER_MODE.get();

        if ("WHITELIST".equalsIgnoreCase(mode)) {
            return !VitalXpConfig.WHITELIST.get().contains(id);
        }
        return VitalXpConfig.BLACKLIST.get().contains(id);
    }
}
