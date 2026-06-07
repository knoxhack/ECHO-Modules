package com.knoxhack.echo.adaptercore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoAdapterCore {
    public static final String MODID = EchoAdapterConstants.MOD_ID;
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoAdapterCore() {
        EchoPlatformAdapter compatibility = EchoNeoForgeAdapterDescriptor.adapter();
        EchoPlatformAdapter nativeLoader = EchoNativeAdapterDescriptor.adapter();
        EchoPlatformAdapter standalone = EchoRuntimeStandaloneAdapterDescriptor.adapter();
        LOGGER.info(
                "ECHO: AdapterCore loaded. Compatibility adapter={} status={}; Native Loader adapter={} status={}; Standalone Runtime adapter={} status={}.",
                compatibility.runtime().serializedName(),
                compatibility.status().serializedName(),
                nativeLoader.runtime().serializedName(),
                nativeLoader.status().serializedName(),
                standalone.runtime().serializedName(),
                standalone.status().serializedName()
        );
    }
}
