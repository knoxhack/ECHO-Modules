package com.knoxhack.echo.codexcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoCodexCore {
    public static final String MODID = "echocodexcore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoCodexCore(Object eventBus) {
        LOGGER.info("ECHO: CodexCore loaded neutral codex contracts.");
    }
}
