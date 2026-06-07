package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echocinematiccore} capabilities in the AdapterCore truth layer.
 */
public final class EchoCinematiccoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echocinematiccore:runtime_host";
    private static final EchoCinematiccoreRuntimeHost HOST = new EchoCinematiccoreRuntimeHost();

    private EchoCinematiccoreRuntimeHost() {
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
                "echocinematiccore.mission_start",
                "echocinematiccore.mission_complete",
                "echocinematiccore.ui_open",
                "echocinematiccore.ui_close",
                "echocinematiccore.button_click",
                "echocinematiccore.region_enter",
                "echocinematiccore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
