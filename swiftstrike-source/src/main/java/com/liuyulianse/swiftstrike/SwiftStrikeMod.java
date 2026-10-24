package com.liuyulianse.swiftstrike;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SwiftStrikeMod.MODID)
public class SwiftStrikeMod {

    public static final String MODID = "swiftstrike";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SwiftStrikeMod() {
        LOGGER.info("SwiftStrike loaded — faster movement = harder hits.");
    }
}
