package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echostatuscore} capabilities in the AdapterCore truth layer.
 */
public final class EchoStatuscoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echostatuscore:runtime_host";
    private static final EchoStatuscoreRuntimeHost HOST = new EchoStatuscoreRuntimeHost();

    private EchoStatuscoreRuntimeHost() {
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
                "echostatuscore.mission_start",
                "echostatuscore.mission_complete",
                "echostatuscore.packet_send",
                "echostatuscore.packet_receive",
                "echostatuscore.region_enter",
                "echostatuscore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
