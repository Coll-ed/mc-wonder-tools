package com.liuyulianse.flatxp;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Flat XP — every level costs exactly 7 XP points, no exponential scaling.
 */
@Mod(FlatXpMod.MODID)
public class FlatXpMod {

    public static final String MODID = "flatxp";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FlatXpMod() {
        LOGGER.info("FlatXP loaded — all levels now cost 7 XP each.");
    }
}
