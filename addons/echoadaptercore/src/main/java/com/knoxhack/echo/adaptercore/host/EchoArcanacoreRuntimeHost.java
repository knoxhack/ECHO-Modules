package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoarcanacore} capabilities in the AdapterCore truth layer.
 */
public final class EchoArcanacoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoarcanacore:runtime_host";
    private static final EchoArcanacoreRuntimeHost HOST = new EchoArcanacoreRuntimeHost();

    private EchoArcanacoreRuntimeHost() {
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
                "echoarcanacore.item_crafted",
                "echoarcanacore.item_used",
                "echoarcanacore.block_placed",
                "echoarcanacore.block_broken",
                "echoarcanacore.machine_use",
                "echoarcanacore.machine_state_changed",
                "echoarcanacore.mission_start",
                "echoarcanacore.mission_complete",
                "echoarcanacore.save_write",
                "echoarcanacore.save_read",
                "echoarcanacore.ui_open",
                "echoarcanacore.ui_close",
                "echoarcanacore.button_click",
                "echoarcanacore.packet_send",
                "echoarcanacore.packet_receive",
                "echoarcanacore.region_enter",
                "echoarcanacore.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
