package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoinputcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoInputcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoinputcore:runtime_host";
    private static final EchoInputcoreRuntimeHost HOST = new EchoInputcoreRuntimeHost();

    private EchoInputcoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Hud",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echoinputcore.ui_open",
                "echoinputcore.ui_close",
                "echoinputcore.button_click",
                "echoinputcore.region_enter",
                "echoinputcore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
