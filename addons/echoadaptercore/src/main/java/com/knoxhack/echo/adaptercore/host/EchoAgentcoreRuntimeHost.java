package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoagentcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoAgentcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoagentcore:runtime_host";
    private static final EchoAgentcoreRuntimeHost HOST = new EchoAgentcoreRuntimeHost();

    private EchoAgentcoreRuntimeHost() {
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
                "echoagentcore.mission_start",
                "echoagentcore.mission_complete",
                "echoagentcore.ui_open",
                "echoagentcore.ui_close",
                "echoagentcore.button_click",
                "echoagentcore.packet_send",
                "echoagentcore.packet_receive",
                "echoagentcore.region_enter",
                "echoagentcore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
