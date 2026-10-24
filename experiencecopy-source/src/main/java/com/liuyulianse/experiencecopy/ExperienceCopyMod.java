package com.liuyulianse.experiencecopy;

import com.liuyulianse.experiencecopy.registry.ModDataComponents;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Experience Copy — Duplicate tools/weapons via anvil using Experience Bottles.
 */
@Mod(ExperienceCopyMod.MODID)
public class ExperienceCopyMod {

    public static final String MODID = "experiencecopy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExperienceCopyMod(IEventBus modEventBus) {
        ModDataComponents.register(modEventBus);
        LOGGER.info("ExperienceCopyMod loaded — tools can now be duplicated via anvil.");
    }
}
