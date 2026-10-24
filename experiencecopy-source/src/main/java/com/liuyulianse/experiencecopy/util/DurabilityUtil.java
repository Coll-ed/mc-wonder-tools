package com.liuyulianse.experiencecopy.util;

/**
 * Durability calculations for experience copy stages.
 * <p>
 * Stage mapping: 0 → 1, 1→20%, 2→40%, 3→60%, 4→80%, 5→100%
 */
public final class DurabilityUtil {

    private DurabilityUtil() {}

    /**
     * Calculates the max durability for a given stage.
     * Stage 0 always returns 1. Stages 1-4 return the proportional value.
     * Stage 5+ returns the original max.
     */
    public static int calculateStageMaxDamage(int originalMax, int stage) {
        if (stage <= 0) {
            return 1;
        }
        if (stage >= 5) {
            return originalMax;
        }
        return Math.max(1, (originalMax * stage + 4) / 5);
    }
}
