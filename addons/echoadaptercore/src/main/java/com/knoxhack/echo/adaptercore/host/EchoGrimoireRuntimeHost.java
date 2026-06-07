package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echogrimoire} capabilities in the AdapterCore truth layer.
 */
public final class EchoGrimoireRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echogrimoire:runtime_host";
    private static final EchoGrimoireRuntimeHost HOST = new EchoGrimoireRuntimeHost();

    private EchoGrimoireRuntimeHost() {
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
                "echogrimoire.item_crafted",
                "echogrimoire.item_used",
                "echogrimoire.machine_use",
                "echogrimoire.machine_state_changed",
                "echogrimoire.mission_start",
                "echogrimoire.mission_complete",
                "echogrimoire.ui_open",
                "echogrimoire.ui_close",
                "echogrimoire.button_click",
                "echogrimoire.packet_send",
                "echogrimoire.packet_receive",
                "echogrimoire.region_enter",
                "echogrimoire.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
