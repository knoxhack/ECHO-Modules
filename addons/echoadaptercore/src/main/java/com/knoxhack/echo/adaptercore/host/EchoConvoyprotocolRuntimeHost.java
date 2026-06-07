package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoconvoyprotocol} capabilities in the AdapterCore truth layer.
 */
public final class EchoConvoyprotocolRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoconvoyprotocol:runtime_host";
    private static final EchoConvoyprotocolRuntimeHost HOST = new EchoConvoyprotocolRuntimeHost();

    private EchoConvoyprotocolRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Hud",
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.SaveData",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echoconvoyprotocol.item_crafted",
                "echoconvoyprotocol.item_used",
                "echoconvoyprotocol.block_placed",
                "echoconvoyprotocol.block_broken",
                "echoconvoyprotocol.machine_use",
                "echoconvoyprotocol.machine_state_changed",
                "echoconvoyprotocol.mission_start",
                "echoconvoyprotocol.mission_complete",
                "echoconvoyprotocol.save_write",
                "echoconvoyprotocol.save_read",
                "echoconvoyprotocol.ui_open",
                "echoconvoyprotocol.ui_close",
                "echoconvoyprotocol.button_click",
                "echoconvoyprotocol.packet_send",
                "echoconvoyprotocol.packet_receive",
                "echoconvoyprotocol.region_enter",
                "echoconvoyprotocol.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
