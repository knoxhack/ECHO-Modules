package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echocore} capabilities in the AdapterCore truth layer.
 */
public final class EchoCoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echocore:runtime_host";
    private static final EchoCoreRuntimeHost HOST = new EchoCoreRuntimeHost();

    private EchoCoreRuntimeHost() {
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
                "echocore.item_crafted",
                "echocore.item_used",
                "echocore.block_placed",
                "echocore.block_broken",
                "echocore.machine_use",
                "echocore.machine_state_changed",
                "echocore.mission_start",
                "echocore.mission_complete",
                "echocore.save_write",
                "echocore.save_read",
                "echocore.ui_open",
                "echocore.ui_close",
                "echocore.button_click",
                "echocore.packet_send",
                "echocore.packet_receive",
                "echocore.region_enter",
                "echocore.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
