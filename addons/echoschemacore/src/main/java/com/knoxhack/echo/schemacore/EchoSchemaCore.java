package com.knoxhack.echo.schemacore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoSchemaCore {
    public static final String MODID = EchoSchemaConstants.MOD_ID;
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoSchemaCore() {
        EchoSchemaConstants.registerBuiltIns(EchoSchemaConstants.GLOBAL_REGISTRY);
        LOGGER.info(
                "ECHO: SchemaCore loaded {} built-in schema descriptors.",
                EchoSchemaConstants.GLOBAL_REGISTRY.descriptors().size()
        );
    }
}
