package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echopowergrid} capabilities in the AdapterCore truth layer.
 */
public final class EchoPowergridRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echopowergrid:runtime_host";
    private static final EchoPowergridRuntimeHost HOST = new EchoPowergridRuntimeHost();

    private EchoPowergridRuntimeHost() {
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
                "echopowergrid.item_crafted",
                "echopowergrid.item_used",
                "echopowergrid.block_placed",
                "echopowergrid.block_broken",
                "echopowergrid.machine_use",
                "echopowergrid.machine_state_changed",
                "echopowergrid.mission_start",
                "echopowergrid.mission_complete",
                "echopowergrid.save_write",
                "echopowergrid.save_read",
                "echopowergrid.ui_open",
                "echopowergrid.ui_close",
                "echopowergrid.button_click",
                "echopowergrid.packet_send",
                "echopowergrid.packet_receive",
                "echopowergrid.region_enter",
                "echopowergrid.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
