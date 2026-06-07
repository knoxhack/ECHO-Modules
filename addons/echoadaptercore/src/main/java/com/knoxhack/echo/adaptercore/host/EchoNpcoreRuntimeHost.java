package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echonpcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoNpcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echonpcore:runtime_host";
    private static final EchoNpcoreRuntimeHost HOST = new EchoNpcoreRuntimeHost();

    private EchoNpcoreRuntimeHost() {
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
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echonpcore.item_crafted",
                "echonpcore.item_used",
                "echonpcore.mission_start",
                "echonpcore.mission_complete",
                "echonpcore.save_write",
                "echonpcore.save_read",
                "echonpcore.ui_open",
                "echonpcore.ui_close",
                "echonpcore.button_click",
                "echonpcore.packet_send",
                "echonpcore.packet_receive",
                "echonpcore.region_enter",
                "echonpcore.discovery"),
                Set.of(),
                true,
                true,
                true));

        try {
            Class<?> handlerClass = Class.forName("com.knoxhack.echonpcore.integration.NpcoreActionHandler");
            handlerClass.getMethod("register").invoke(null);
        } catch (Exception ignored) {
            // Addon action handler not present
        }
    }
}
