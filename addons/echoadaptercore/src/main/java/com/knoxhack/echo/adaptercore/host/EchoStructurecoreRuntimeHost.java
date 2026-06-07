package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echostructurecore} capabilities in the AdapterCore truth layer.
 */
public final class EchoStructurecoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echostructurecore:runtime_host";
    private static final EchoStructurecoreRuntimeHost HOST = new EchoStructurecoreRuntimeHost();

    private EchoStructurecoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echostructurecore.mission_start",
                "echostructurecore.mission_complete",
                "echostructurecore.packet_send",
                "echostructurecore.packet_receive",
                "echostructurecore.region_enter",
                "echostructurecore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
