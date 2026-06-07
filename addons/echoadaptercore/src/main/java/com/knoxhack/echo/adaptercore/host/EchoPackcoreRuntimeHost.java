package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echopackcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoPackcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echopackcore:runtime_host";
    private static final EchoPackcoreRuntimeHost HOST = new EchoPackcoreRuntimeHost();

    private EchoPackcoreRuntimeHost() {
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
                "echopackcore.mission_start",
                "echopackcore.mission_complete",
                "echopackcore.packet_send",
                "echopackcore.packet_receive",
                "echopackcore.region_enter",
                "echopackcore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
