package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echospawncore} capabilities in the AdapterCore truth layer.
 */
public final class EchoSpawncoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echospawncore:runtime_host";
    private static final EchoSpawncoreRuntimeHost HOST = new EchoSpawncoreRuntimeHost();

    private EchoSpawncoreRuntimeHost() {
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
                "echospawncore.mission_start",
                "echospawncore.mission_complete",
                "echospawncore.packet_send",
                "echospawncore.packet_receive",
                "echospawncore.region_enter",
                "echospawncore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
