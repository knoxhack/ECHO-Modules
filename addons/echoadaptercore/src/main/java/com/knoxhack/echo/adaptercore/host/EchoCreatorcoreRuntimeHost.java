package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echocreatorcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoCreatorcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echocreatorcore:runtime_host";
    private static final EchoCreatorcoreRuntimeHost HOST = new EchoCreatorcoreRuntimeHost();

    private EchoCreatorcoreRuntimeHost() {
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
                "echocreatorcore.item_crafted",
                "echocreatorcore.item_used",
                "echocreatorcore.block_placed",
                "echocreatorcore.block_broken",
                "echocreatorcore.machine_use",
                "echocreatorcore.machine_state_changed",
                "echocreatorcore.mission_start",
                "echocreatorcore.mission_complete",
                "echocreatorcore.ui_open",
                "echocreatorcore.ui_close",
                "echocreatorcore.button_click",
                "echocreatorcore.packet_send",
                "echocreatorcore.packet_receive",
                "echocreatorcore.region_enter",
                "echocreatorcore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
