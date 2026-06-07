package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echothemecore} capabilities in the AdapterCore truth layer.
 */
public final class EchoThemecoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echothemecore:runtime_host";
    private static final EchoThemecoreRuntimeHost HOST = new EchoThemecoreRuntimeHost();

    private EchoThemecoreRuntimeHost() {
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
                "echothemecore.machine_use",
                "echothemecore.machine_state_changed",
                "echothemecore.mission_start",
                "echothemecore.mission_complete",
                "echothemecore.ui_open",
                "echothemecore.ui_close",
                "echothemecore.button_click",
                "echothemecore.packet_send",
                "echothemecore.packet_receive",
                "echothemecore.region_enter",
                "echothemecore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
