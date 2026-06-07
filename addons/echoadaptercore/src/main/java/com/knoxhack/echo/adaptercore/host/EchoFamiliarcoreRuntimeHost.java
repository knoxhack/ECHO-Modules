package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echofamiliarcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoFamiliarcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echofamiliarcore:runtime_host";
    private static final EchoFamiliarcoreRuntimeHost HOST = new EchoFamiliarcoreRuntimeHost();

    private EchoFamiliarcoreRuntimeHost() {
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
                "echofamiliarcore.item_crafted",
                "echofamiliarcore.item_used",
                "echofamiliarcore.machine_use",
                "echofamiliarcore.machine_state_changed",
                "echofamiliarcore.mission_start",
                "echofamiliarcore.mission_complete",
                "echofamiliarcore.save_write",
                "echofamiliarcore.save_read",
                "echofamiliarcore.ui_open",
                "echofamiliarcore.ui_close",
                "echofamiliarcore.button_click",
                "echofamiliarcore.packet_send",
                "echofamiliarcore.packet_receive",
                "echofamiliarcore.region_enter",
                "echofamiliarcore.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
