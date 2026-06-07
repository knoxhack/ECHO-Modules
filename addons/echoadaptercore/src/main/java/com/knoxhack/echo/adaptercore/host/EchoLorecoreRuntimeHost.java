package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echolorecore} capabilities in the AdapterCore truth layer.
 */
public final class EchoLorecoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echolorecore:runtime_host";
    private static final EchoLorecoreRuntimeHost HOST = new EchoLorecoreRuntimeHost();

    private EchoLorecoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echolorecore.mission_start",
                "echolorecore.mission_complete",
                "echolorecore.region_enter",
                "echolorecore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
