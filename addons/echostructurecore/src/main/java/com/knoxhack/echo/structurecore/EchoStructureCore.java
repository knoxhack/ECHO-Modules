package com.knoxhack.echo.structurecore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoStructureCore {
    public static final String MODID = "echostructurecore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoStructureCore() {
        EchoStructureCoreEvents.attach();
        LOGGER.info("ECHO: StructureCore online with structure, POI, danger, discovery, scan, and loot binding contracts.");
    }
}
