package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echostationfall} capabilities in the AdapterCore truth layer.
 */
public final class EchoStationfallRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echostationfall:runtime_host";
    private static final EchoStationfallRuntimeHost HOST = new EchoStationfallRuntimeHost();

    private EchoStationfallRuntimeHost() {
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
                "echostationfall.item_crafted",
                "echostationfall.item_used",
                "echostationfall.block_placed",
                "echostationfall.block_broken",
                "echostationfall.machine_use",
                "echostationfall.machine_state_changed",
                "echostationfall.mission_start",
                "echostationfall.mission_complete",
                "echostationfall.save_write",
                "echostationfall.save_read",
                "echostationfall.ui_open",
                "echostationfall.ui_close",
                "echostationfall.button_click",
                "echostationfall.packet_send",
                "echostationfall.packet_receive",
                "echostationfall.region_enter",
                "echostationfall.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
