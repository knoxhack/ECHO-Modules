package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echohudcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoHudcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echohudcore:runtime_host";
    private static final EchoHudcoreRuntimeHost HOST = new EchoHudcoreRuntimeHost();

    private EchoHudcoreRuntimeHost() {
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
                "echohudcore.mission_start",
                "echohudcore.mission_complete",
                "echohudcore.ui_open",
                "echohudcore.ui_close",
                "echohudcore.button_click",
                "echohudcore.packet_send",
                "echohudcore.packet_receive",
                "echohudcore.region_enter",
                "echohudcore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
