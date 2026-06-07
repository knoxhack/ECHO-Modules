package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echocodexcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoCodexcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echocodexcore:runtime_host";
    private static final EchoCodexcoreRuntimeHost HOST = new EchoCodexcoreRuntimeHost();

    private EchoCodexcoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Hud",
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echocodexcore.mission_start",
                "echocodexcore.mission_complete",
                "echocodexcore.ui_open",
                "echocodexcore.ui_close",
                "echocodexcore.button_click",
                "echocodexcore.packet_send",
                "echocodexcore.packet_receive",
                "echocodexcore.region_enter",
                "echocodexcore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
