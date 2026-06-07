package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echomodulegraph} capabilities in the AdapterCore truth layer.
 */
public final class EchoModulegraphRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echomodulegraph:runtime_host";
    private static final EchoModulegraphRuntimeHost HOST = new EchoModulegraphRuntimeHost();

    private EchoModulegraphRuntimeHost() {
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
                "echomodulegraph.mission_start",
                "echomodulegraph.mission_complete",
                "echomodulegraph.packet_send",
                "echomodulegraph.packet_receive",
                "echomodulegraph.region_enter",
                "echomodulegraph.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
