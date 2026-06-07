package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoblackboxprotocol} capabilities in the AdapterCore truth layer.
 */
public final class EchoBlackboxprotocolRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoblackboxprotocol:runtime_host";
    private static final EchoBlackboxprotocolRuntimeHost HOST = new EchoBlackboxprotocolRuntimeHost();

    private EchoBlackboxprotocolRuntimeHost() {
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
                "echoblackboxprotocol.item_crafted",
                "echoblackboxprotocol.item_used",
                "echoblackboxprotocol.block_placed",
                "echoblackboxprotocol.block_broken",
                "echoblackboxprotocol.machine_use",
                "echoblackboxprotocol.machine_state_changed",
                "echoblackboxprotocol.mission_start",
                "echoblackboxprotocol.mission_complete",
                "echoblackboxprotocol.save_write",
                "echoblackboxprotocol.save_read",
                "echoblackboxprotocol.ui_open",
                "echoblackboxprotocol.ui_close",
                "echoblackboxprotocol.button_click",
                "echoblackboxprotocol.packet_send",
                "echoblackboxprotocol.packet_receive",
                "echoblackboxprotocol.region_enter",
                "echoblackboxprotocol.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
