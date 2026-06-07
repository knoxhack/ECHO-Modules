package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echorendercore} capabilities in the AdapterCore truth layer.
 */
public final class EchoRendercoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echorendercore:runtime_host";
    private static final EchoRendercoreRuntimeHost HOST = new EchoRendercoreRuntimeHost();

    private EchoRendercoreRuntimeHost() {
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
                "echorendercore.block_placed",
                "echorendercore.block_broken",
                "echorendercore.machine_use",
                "echorendercore.machine_state_changed",
                "echorendercore.mission_start",
                "echorendercore.mission_complete",
                "echorendercore.ui_open",
                "echorendercore.ui_close",
                "echorendercore.button_click",
                "echorendercore.packet_send",
                "echorendercore.packet_receive",
                "echorendercore.region_enter",
                "echorendercore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
