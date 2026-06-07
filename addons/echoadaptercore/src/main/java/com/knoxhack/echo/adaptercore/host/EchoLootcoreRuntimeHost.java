package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echolootcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoLootcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echolootcore:runtime_host";
    private static final EchoLootcoreRuntimeHost HOST = new EchoLootcoreRuntimeHost();

    private EchoLootcoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echolootcore.mission_start",
                "echolootcore.mission_complete",
                "echolootcore.region_enter",
                "echolootcore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
