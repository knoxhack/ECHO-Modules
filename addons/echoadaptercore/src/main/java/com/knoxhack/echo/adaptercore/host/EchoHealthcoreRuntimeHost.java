package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echohealthcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoHealthcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echohealthcore:runtime_host";
    private static final EchoHealthcoreRuntimeHost HOST = new EchoHealthcoreRuntimeHost();

    private EchoHealthcoreRuntimeHost() {
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
                "echohealthcore.mission_start",
                "echohealthcore.mission_complete",
                "echohealthcore.ui_open",
                "echohealthcore.ui_close",
                "echohealthcore.button_click",
                "echohealthcore.packet_send",
                "echohealthcore.packet_receive",
                "echohealthcore.region_enter",
                "echohealthcore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
