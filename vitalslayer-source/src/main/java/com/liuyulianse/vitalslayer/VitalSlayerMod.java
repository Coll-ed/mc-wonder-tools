package com.liuyulianse.vitalslayer;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Percent Slayer — a lightweight NeoForge mod that adds the
 * "百分比杀伤" (Percent Slayer) enchantment.
 * <p>
 * 每击必触发的百分比伤害：附加伤害 = 等级 × 目标最大生命 × 2%
 * （1级2% → 5级10%）。
 */
@Mod(VitalSlayerMod.MODID)
public class VitalSlayerMod {

    public static final String MODID = "vitalslayer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VitalSlayerMod() {
        LOGGER.info("VitalSlayerMod loaded — Percent Slayer enchantment ready.");
    }
}
