package com.knoxhack.echo.validationcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoValidationCore {
    public static final String MODID = EchoValidationConstants.MOD_ID;
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoValidationCore() {
        LOGGER.info("ECHO: ValidationCore loaded diagnostic and validation contracts.");
    }
}
