package com.knoxhack.echo.combatcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoCombatCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoCombatCore(Object modEventBus) {
        LOGGER.info("{} loaded neutral combat contracts.", EchoCombatConstants.MOD_NAME);
    }
}
