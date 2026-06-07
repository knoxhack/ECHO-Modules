package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoindustrialnexus} capabilities in the AdapterCore truth layer.
 */
public final class EchoIndustrialnexusRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoindustrialnexus:runtime_host";
    private static final EchoIndustrialnexusRuntimeHost HOST = new EchoIndustrialnexusRuntimeHost();

    private EchoIndustrialnexusRuntimeHost() {
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
                "echoindustrialnexus.item_crafted",
                "echoindustrialnexus.item_used",
                "echoindustrialnexus.block_placed",
                "echoindustrialnexus.block_broken",
                "echoindustrialnexus.machine_use",
                "echoindustrialnexus.machine_state_changed",
                "echoindustrialnexus.mission_start",
                "echoindustrialnexus.mission_complete",
                "echoindustrialnexus.save_write",
                "echoindustrialnexus.save_read",
                "echoindustrialnexus.ui_open",
                "echoindustrialnexus.ui_close",
                "echoindustrialnexus.button_click",
                "echoindustrialnexus.packet_send",
                "echoindustrialnexus.packet_receive",
                "echoindustrialnexus.region_enter",
                "echoindustrialnexus.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
