package com.hxzhitang.tongdaway;

import com.hxzhitang.tongdaway.config.VitalXpConfig;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(VitalXpMod.MODID)
public class VitalXpMod {
    public static final String MODID = "vitalxp";
    private static final Logger LOGGER = LogUtils.getLogger();

    public VitalXpMod(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, VitalXpConfig.SERVER_SPEC);
        LOGGER.info("Vital XP loaded. Stronger mobs → more XP!");
    }
}
