package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoweathercore} capabilities in the AdapterCore truth layer.
 */
public final class EchoWeathercoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoweathercore:runtime_host";
    private static final EchoWeathercoreRuntimeHost HOST = new EchoWeathercoreRuntimeHost();

    private EchoWeathercoreRuntimeHost() {
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
                "echoweathercore.item_crafted",
                "echoweathercore.item_used",
                "echoweathercore.block_placed",
                "echoweathercore.block_broken",
                "echoweathercore.machine_use",
                "echoweathercore.machine_state_changed",
                "echoweathercore.mission_start",
                "echoweathercore.mission_complete",
                "echoweathercore.ui_open",
                "echoweathercore.ui_close",
                "echoweathercore.button_click",
                "echoweathercore.packet_send",
                "echoweathercore.packet_receive",
                "echoweathercore.region_enter",
                "echoweathercore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
