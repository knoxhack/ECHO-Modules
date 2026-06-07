package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echovalidationcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoValidationcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echovalidationcore:runtime_host";
    private static final EchoValidationcoreRuntimeHost HOST = new EchoValidationcoreRuntimeHost();

    private EchoValidationcoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echovalidationcore.machine_use",
                "echovalidationcore.machine_state_changed",
                "echovalidationcore.mission_start",
                "echovalidationcore.mission_complete",
                "echovalidationcore.packet_send",
                "echovalidationcore.packet_receive",
                "echovalidationcore.region_enter",
                "echovalidationcore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
