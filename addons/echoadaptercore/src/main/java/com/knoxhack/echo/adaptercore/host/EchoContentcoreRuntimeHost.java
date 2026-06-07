package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echocontentcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoContentcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echocontentcore:runtime_host";
    private static final EchoContentcoreRuntimeHost HOST = new EchoContentcoreRuntimeHost();

    private EchoContentcoreRuntimeHost() {
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
                "echocontentcore.machine_use",
                "echocontentcore.machine_state_changed",
                "echocontentcore.mission_start",
                "echocontentcore.mission_complete",
                "echocontentcore.ui_open",
                "echocontentcore.ui_close",
                "echocontentcore.button_click",
                "echocontentcore.packet_send",
                "echocontentcore.packet_receive",
                "echocontentcore.region_enter",
                "echocontentcore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
