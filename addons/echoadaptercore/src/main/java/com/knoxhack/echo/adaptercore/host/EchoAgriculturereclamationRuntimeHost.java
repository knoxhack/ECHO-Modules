package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoagriculturereclamation} capabilities in the AdapterCore truth layer.
 */
public final class EchoAgriculturereclamationRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoagriculturereclamation:runtime_host";
    private static final EchoAgriculturereclamationRuntimeHost HOST = new EchoAgriculturereclamationRuntimeHost();

    private EchoAgriculturereclamationRuntimeHost() {
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
                "echoagriculturereclamation.item_crafted",
                "echoagriculturereclamation.item_used",
                "echoagriculturereclamation.block_placed",
                "echoagriculturereclamation.block_broken",
                "echoagriculturereclamation.machine_use",
                "echoagriculturereclamation.machine_state_changed",
                "echoagriculturereclamation.mission_start",
                "echoagriculturereclamation.mission_complete",
                "echoagriculturereclamation.save_write",
                "echoagriculturereclamation.save_read",
                "echoagriculturereclamation.ui_open",
                "echoagriculturereclamation.ui_close",
                "echoagriculturereclamation.button_click",
                "echoagriculturereclamation.packet_send",
                "echoagriculturereclamation.packet_receive",
                "echoagriculturereclamation.region_enter",
                "echoagriculturereclamation.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
