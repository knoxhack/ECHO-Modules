package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echobasegrid} capabilities in the AdapterCore truth layer.
 */
public final class EchoBasegridRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echobasegrid:runtime_host";
    private static final EchoBasegridRuntimeHost HOST = new EchoBasegridRuntimeHost();

    private EchoBasegridRuntimeHost() {
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
                "echobasegrid.block_placed",
                "echobasegrid.block_broken",
                "echobasegrid.machine_use",
                "echobasegrid.machine_state_changed",
                "echobasegrid.mission_start",
                "echobasegrid.mission_complete",
                "echobasegrid.save_write",
                "echobasegrid.save_read",
                "echobasegrid.ui_open",
                "echobasegrid.ui_close",
                "echobasegrid.button_click",
                "echobasegrid.packet_send",
                "echobasegrid.packet_receive",
                "echobasegrid.region_enter",
                "echobasegrid.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
