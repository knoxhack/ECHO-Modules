package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoblockworks} capabilities in the AdapterCore truth layer.
 */
public final class EchoBlockworksRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoblockworks:runtime_host";
    private static final EchoBlockworksRuntimeHost HOST = new EchoBlockworksRuntimeHost();

    private EchoBlockworksRuntimeHost() {
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
                "echoblockworks.item_crafted",
                "echoblockworks.item_used",
                "echoblockworks.block_placed",
                "echoblockworks.block_broken",
                "echoblockworks.machine_use",
                "echoblockworks.machine_state_changed",
                "echoblockworks.mission_start",
                "echoblockworks.mission_complete",
                "echoblockworks.ui_open",
                "echoblockworks.ui_close",
                "echoblockworks.button_click",
                "echoblockworks.packet_send",
                "echoblockworks.packet_receive",
                "echoblockworks.region_enter",
                "echoblockworks.discovery"),
                Set.of(),
                true,
                false,
                true));

        try {
            Class<?> handlerClass = Class.forName("com.knoxhack.echoblockworks.integration.BlockworksActionHandler");
            handlerClass.getMethod("register").invoke(null);
        } catch (Exception ignored) {
            // Addon action handler not present
        }
    }
}
