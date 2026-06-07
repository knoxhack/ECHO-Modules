package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echonotificationcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoNotificationcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echonotificationcore:runtime_host";
    private static final EchoNotificationcoreRuntimeHost HOST = new EchoNotificationcoreRuntimeHost();

    private EchoNotificationcoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Hud",
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echonotificationcore.mission_start",
                "echonotificationcore.mission_complete",
                "echonotificationcore.ui_open",
                "echonotificationcore.ui_close",
                "echonotificationcore.button_click",
                "echonotificationcore.packet_send",
                "echonotificationcore.packet_receive",
                "echonotificationcore.region_enter",
                "echonotificationcore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
