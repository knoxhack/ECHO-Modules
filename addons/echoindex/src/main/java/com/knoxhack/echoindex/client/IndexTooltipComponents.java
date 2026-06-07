package com.knoxhack.echoindex.client;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;

public final class IndexTooltipComponents {
    private IndexTooltipComponents() {
    }

    public static void register(Object event) {
        EchoBackendClientBridge.registerTooltipComponentFactory(
                event,
                IndexBlockPreviewTooltipData.class,
                IndexBlockPreviewTooltip.class);
    }
}
