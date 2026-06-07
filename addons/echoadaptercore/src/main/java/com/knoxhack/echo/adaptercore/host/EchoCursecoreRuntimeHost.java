package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echocursecore} capabilities in the AdapterCore truth layer.
 */
public final class EchoCursecoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echocursecore:runtime_host";
    private static final EchoCursecoreRuntimeHost HOST = new EchoCursecoreRuntimeHost();

    private EchoCursecoreRuntimeHost() {
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
                "echocursecore.item_crafted",
                "echocursecore.item_used",
                "echocursecore.machine_use",
                "echocursecore.machine_state_changed",
                "echocursecore.mission_start",
                "echocursecore.mission_complete",
                "echocursecore.save_write",
                "echocursecore.save_read",
                "echocursecore.ui_open",
                "echocursecore.ui_close",
                "echocursecore.button_click",
                "echocursecore.packet_send",
                "echocursecore.packet_receive",
                "echocursecore.region_enter",
                "echocursecore.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
