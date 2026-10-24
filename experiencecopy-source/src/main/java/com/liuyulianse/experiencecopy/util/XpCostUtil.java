package com.liuyulianse.experiencecopy.util;

/**
 * Experience cost calculation for the experience copy process.
 * <p>
 * Uses level-based mode: every 100 durability points cost 1 level.
 */
public final class XpCostUtil {

    /** How many durability points are covered by 1 level. */
    private static final int DURABILITY_PER_LEVEL = 100;

    private XpCostUtil() {}

    /**
     * Calculates the level cost to restore durability from {@code fromMax} to {@code toMax}.
     */
    public static int calculateLevelCost(int fromMax, int toMax) {
        int delta = Math.max(0, toMax - fromMax);
        return Math.max(1, (delta + DURABILITY_PER_LEVEL - 1) / DURABILITY_PER_LEVEL);
    }
}
