package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoscreencore} capabilities in the AdapterCore truth layer.
 */
public final class EchoScreencoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoscreencore:runtime_host";
    private static final EchoScreencoreRuntimeHost HOST = new EchoScreencoreRuntimeHost();

    private EchoScreencoreRuntimeHost() {
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
                "echoscreencore.item_crafted",
                "echoscreencore.item_used",
                "echoscreencore.machine_use",
                "echoscreencore.machine_state_changed",
                "echoscreencore.mission_start",
                "echoscreencore.mission_complete",
                "echoscreencore.ui_open",
                "echoscreencore.ui_close",
                "echoscreencore.button_click",
                "echoscreencore.packet_send",
                "echoscreencore.packet_receive",
                "echoscreencore.region_enter",
                "echoscreencore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
