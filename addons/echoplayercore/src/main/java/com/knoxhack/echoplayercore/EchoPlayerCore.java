package com.knoxhack.echoplayercore;

import com.knoxhack.echoplayercore.command.PlayerCoreCommands;
import com.knoxhack.echoplayercore.integration.PlayerCoreIntegrations;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoPlayerCore {
    public static final String MODID = "echoplayercore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoPlayerCore() {
        PlayerCoreCommands.registerEchoSubcommands();
        PlayerCoreIntegrations.logIntegrationStatus();
        LOGGER.info("ECHO PlayerCore initialized.");
    }
}
