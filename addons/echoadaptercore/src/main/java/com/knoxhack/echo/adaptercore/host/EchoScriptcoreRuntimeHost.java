package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoscriptcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoScriptcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoscriptcore:runtime_host";
    private static final EchoScriptcoreRuntimeHost HOST = new EchoScriptcoreRuntimeHost();

    private EchoScriptcoreRuntimeHost() {
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
                "echoscriptcore.item_crafted",
                "echoscriptcore.item_used",
                "echoscriptcore.machine_use",
                "echoscriptcore.machine_state_changed",
                "echoscriptcore.mission_start",
                "echoscriptcore.mission_complete",
                "echoscriptcore.save_write",
                "echoscriptcore.save_read",
                "echoscriptcore.ui_open",
                "echoscriptcore.ui_close",
                "echoscriptcore.button_click",
                "echoscriptcore.packet_send",
                "echoscriptcore.packet_receive",
                "echoscriptcore.region_enter",
                "echoscriptcore.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
