package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echologisticsnetwork} capabilities in the AdapterCore truth layer.
 */
public final class EchoLogisticsnetworkRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echologisticsnetwork:runtime_host";
    private static final EchoLogisticsnetworkRuntimeHost HOST = new EchoLogisticsnetworkRuntimeHost();

    private EchoLogisticsnetworkRuntimeHost() {
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
                "echologisticsnetwork.item_crafted",
                "echologisticsnetwork.item_used",
                "echologisticsnetwork.block_placed",
                "echologisticsnetwork.block_broken",
                "echologisticsnetwork.machine_use",
                "echologisticsnetwork.machine_state_changed",
                "echologisticsnetwork.mission_start",
                "echologisticsnetwork.mission_complete",
                "echologisticsnetwork.save_write",
                "echologisticsnetwork.save_read",
                "echologisticsnetwork.ui_open",
                "echologisticsnetwork.ui_close",
                "echologisticsnetwork.button_click",
                "echologisticsnetwork.packet_send",
                "echologisticsnetwork.packet_receive",
                "echologisticsnetwork.region_enter",
                "echologisticsnetwork.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
