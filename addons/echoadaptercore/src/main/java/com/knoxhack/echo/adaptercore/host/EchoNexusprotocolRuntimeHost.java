package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echonexusprotocol} capabilities in the AdapterCore truth layer.
 */
public final class EchoNexusprotocolRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echonexusprotocol:runtime_host";
    private static final EchoNexusprotocolRuntimeHost HOST = new EchoNexusprotocolRuntimeHost();

    private EchoNexusprotocolRuntimeHost() {
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
                "echonexusprotocol.item_crafted",
                "echonexusprotocol.item_used",
                "echonexusprotocol.block_placed",
                "echonexusprotocol.block_broken",
                "echonexusprotocol.machine_use",
                "echonexusprotocol.machine_state_changed",
                "echonexusprotocol.mission_start",
                "echonexusprotocol.mission_complete",
                "echonexusprotocol.save_write",
                "echonexusprotocol.save_read",
                "echonexusprotocol.ui_open",
                "echonexusprotocol.ui_close",
                "echonexusprotocol.button_click",
                "echonexusprotocol.packet_send",
                "echonexusprotocol.packet_receive",
                "echonexusprotocol.region_enter",
                "echonexusprotocol.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
