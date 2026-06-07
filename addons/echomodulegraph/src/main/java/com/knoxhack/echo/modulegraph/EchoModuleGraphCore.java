package com.knoxhack.echo.modulegraph;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoModuleGraphCore {
    public static final String MODID = EchoModuleGraphConstants.MOD_ID;
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoModuleGraphCore(Object ignoredModEventBus) {
        LOGGER.info(
                "ECHO: ModuleGraph loaded {} diagnostic-ready graph issue kinds.",
                EchoModuleGraphConstants.DIAGNOSTIC_READY_ISSUES.size()
        );
    }
}
