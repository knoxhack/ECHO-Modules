package com.knoxhack.echorelictech.integration.terminal;

import com.knoxhack.echorelictech.EchoRelicTech;

public class RelicTechTerminalIntegration {
    public static void register() {
        EchoRelicTech.LOGGER.info("ECHO Terminal integration loaded for RelicTech.");
        RelicTechTerminalCommonIntegration.register();
    }
}
