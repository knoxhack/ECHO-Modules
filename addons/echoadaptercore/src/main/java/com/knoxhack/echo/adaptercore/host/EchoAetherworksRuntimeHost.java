package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoaetherworks} capabilities in the AdapterCore truth layer.
 */
public final class EchoAetherworksRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoaetherworks:runtime_host";
    private static final EchoAetherworksRuntimeHost HOST = new EchoAetherworksRuntimeHost();

    private EchoAetherworksRuntimeHost() {
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
                "echoaetherworks.item_crafted",
                "echoaetherworks.item_used",
                "echoaetherworks.block_placed",
                "echoaetherworks.block_broken",
                "echoaetherworks.machine_use",
                "echoaetherworks.machine_state_changed",
                "echoaetherworks.mission_start",
                "echoaetherworks.mission_complete",
                "echoaetherworks.ui_open",
                "echoaetherworks.ui_close",
                "echoaetherworks.button_click",
                "echoaetherworks.packet_send",
                "echoaetherworks.packet_receive",
                "echoaetherworks.region_enter",
                "echoaetherworks.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
