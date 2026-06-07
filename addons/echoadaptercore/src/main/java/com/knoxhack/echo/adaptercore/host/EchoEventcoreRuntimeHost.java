package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoeventcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoEventcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoeventcore:runtime_host";
    private static final EchoEventcoreRuntimeHost HOST = new EchoEventcoreRuntimeHost();

    private EchoEventcoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echoeventcore.mission_start",
                "echoeventcore.mission_complete",
                "echoeventcore.region_enter",
                "echoeventcore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
