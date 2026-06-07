package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoarcaneindex} capabilities in the AdapterCore truth layer.
 */
public final class EchoArcaneindexRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoarcaneindex:runtime_host";
    private static final EchoArcaneindexRuntimeHost HOST = new EchoArcaneindexRuntimeHost();

    private EchoArcaneindexRuntimeHost() {
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
                "echoarcaneindex.item_crafted",
                "echoarcaneindex.item_used",
                "echoarcaneindex.machine_use",
                "echoarcaneindex.machine_state_changed",
                "echoarcaneindex.mission_start",
                "echoarcaneindex.mission_complete",
                "echoarcaneindex.ui_open",
                "echoarcaneindex.ui_close",
                "echoarcaneindex.button_click",
                "echoarcaneindex.packet_send",
                "echoarcaneindex.packet_receive",
                "echoarcaneindex.region_enter",
                "echoarcaneindex.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
