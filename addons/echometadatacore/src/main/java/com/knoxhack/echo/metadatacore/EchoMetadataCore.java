package com.knoxhack.echo.metadatacore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoMetadataCore {
    public static final String MODID = EchoMetadataConstants.MOD_ID;
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoMetadataCore() {
        LOGGER.info(
                "ECHO: MetadataCore loaded optional metadata contracts for {} schema descriptors.",
                EchoMetadataConstants.SCHEMA_DESCRIPTORS.size()
        );
    }
}
