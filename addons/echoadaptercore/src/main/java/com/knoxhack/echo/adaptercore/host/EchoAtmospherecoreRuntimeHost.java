package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoatmospherecore} capabilities in the AdapterCore truth layer.
 */
public final class EchoAtmospherecoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoatmospherecore:runtime_host";
    private static final EchoAtmospherecoreRuntimeHost HOST = new EchoAtmospherecoreRuntimeHost();

    private EchoAtmospherecoreRuntimeHost() {
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
                "echoatmospherecore.mission_start",
                "echoatmospherecore.mission_complete",
                "echoatmospherecore.ui_open",
                "echoatmospherecore.ui_close",
                "echoatmospherecore.button_click",
                "echoatmospherecore.packet_send",
                "echoatmospherecore.packet_receive",
                "echoatmospherecore.region_enter",
                "echoatmospherecore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
