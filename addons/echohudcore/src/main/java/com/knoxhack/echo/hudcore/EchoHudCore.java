package com.knoxhack.echo.hudcore;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoHudCore.MODID)
public final class EchoHudCore {
    public static final String MODID = "echohudcore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoHudCore() {
        LOGGER.info("ECHO: HUDCore loaded neutral HUD contracts.");
    }
}
