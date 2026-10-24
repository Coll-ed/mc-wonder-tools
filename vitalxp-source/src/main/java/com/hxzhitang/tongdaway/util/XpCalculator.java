package com.hxzhitang.tongdaway.util;

import com.hxzhitang.tongdaway.common.XpRoundingMode;

public final class XpCalculator {
    private XpCalculator() {}

    public static int calculateXp(
            int baseXp,
            float maxHealth,
            double healthMultiplier,
            XpRoundingMode roundingMode,
            boolean enableMultiplierCap,
            double maxMultiplier,
            boolean enableFinalXpCap,
            int maxFinalXp
    ) {
        if (baseXp <= 0) {
            return 0;
        }
        if (maxHealth <= 0) {
            return baseXp;
        }

        double multiplier = 1.0D + maxHealth * healthMultiplier;

        if (enableMultiplierCap) {
            multiplier = Math.min(multiplier, maxMultiplier);
        }

        double result = baseXp * multiplier;

        if (enableFinalXpCap) {
            result = Math.min(result, maxFinalXp);
        }

        int finalXp = switch (roundingMode) {
            case FLOOR -> (int) Math.floor(result);
            case ROUND -> (int) Math.round(result);
            case CEIL -> (int) Math.ceil(result);
        };

        return Math.max(finalXp, 0);
    }
}
