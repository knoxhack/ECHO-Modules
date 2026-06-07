package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echosoundcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoSoundcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echosoundcore:runtime_host";
    private static final EchoSoundcoreRuntimeHost HOST = new EchoSoundcoreRuntimeHost();

    private EchoSoundcoreRuntimeHost() {
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
                "echosoundcore.mission_start",
                "echosoundcore.mission_complete",
                "echosoundcore.ui_open",
                "echosoundcore.ui_close",
                "echosoundcore.button_click",
                "echosoundcore.packet_send",
                "echosoundcore.packet_receive",
                "echosoundcore.region_enter",
                "echosoundcore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
