package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoprimecore} capabilities in the AdapterCore truth layer.
 */
public final class EchoPrimecoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoprimecore:runtime_host";
    private static final EchoPrimecoreRuntimeHost HOST = new EchoPrimecoreRuntimeHost();

    private EchoPrimecoreRuntimeHost() {
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
                "echoprimecore.item_crafted",
                "echoprimecore.item_used",
                "echoprimecore.block_placed",
                "echoprimecore.block_broken",
                "echoprimecore.machine_use",
                "echoprimecore.machine_state_changed",
                "echoprimecore.mission_start",
                "echoprimecore.mission_complete",
                "echoprimecore.save_write",
                "echoprimecore.save_read",
                "echoprimecore.ui_open",
                "echoprimecore.ui_close",
                "echoprimecore.button_click",
                "echoprimecore.packet_send",
                "echoprimecore.packet_receive",
                "echoprimecore.region_enter",
                "echoprimecore.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
