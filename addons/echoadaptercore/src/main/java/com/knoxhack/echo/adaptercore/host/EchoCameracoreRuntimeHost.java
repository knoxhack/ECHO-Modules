package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echocameracore} capabilities in the AdapterCore truth layer.
 */
public final class EchoCameracoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echocameracore:runtime_host";
    private static final EchoCameracoreRuntimeHost HOST = new EchoCameracoreRuntimeHost();

    private EchoCameracoreRuntimeHost() {
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
                "echocameracore.ui_open",
                "echocameracore.ui_close",
                "echocameracore.button_click",
                "echocameracore.region_enter",
                "echocameracore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
