package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echocommunitybridge} capabilities in the AdapterCore truth layer.
 */
public final class EchoCommunitybridgeRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echocommunitybridge:runtime_host";
    private static final EchoCommunitybridgeRuntimeHost HOST = new EchoCommunitybridgeRuntimeHost();

    private EchoCommunitybridgeRuntimeHost() {
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
                "echocommunitybridge.mission_start",
                "echocommunitybridge.mission_complete",
                "echocommunitybridge.ui_open",
                "echocommunitybridge.ui_close",
                "echocommunitybridge.button_click",
                "echocommunitybridge.packet_send",
                "echocommunitybridge.packet_receive",
                "echocommunitybridge.region_enter",
                "echocommunitybridge.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
