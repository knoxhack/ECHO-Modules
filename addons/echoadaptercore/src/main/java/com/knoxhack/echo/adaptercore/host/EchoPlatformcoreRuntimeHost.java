package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoplatformcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoPlatformcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoplatformcore:runtime_host";
    private static final EchoPlatformcoreRuntimeHost HOST = new EchoPlatformcoreRuntimeHost();

    private EchoPlatformcoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Hud",
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echoplatformcore.machine_use",
                "echoplatformcore.machine_state_changed",
                "echoplatformcore.mission_start",
                "echoplatformcore.mission_complete",
                "echoplatformcore.ui_open",
                "echoplatformcore.ui_close",
                "echoplatformcore.button_click",
                "echoplatformcore.packet_send",
                "echoplatformcore.packet_receive",
                "echoplatformcore.region_enter",
                "echoplatformcore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
