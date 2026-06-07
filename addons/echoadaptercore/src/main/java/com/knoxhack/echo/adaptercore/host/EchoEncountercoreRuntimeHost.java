package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoencountercore} capabilities in the AdapterCore truth layer.
 */
public final class EchoEncountercoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoencountercore:runtime_host";
    private static final EchoEncountercoreRuntimeHost HOST = new EchoEncountercoreRuntimeHost();

    private EchoEncountercoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echoencountercore.mission_start",
                "echoencountercore.mission_complete",
                "echoencountercore.region_enter",
                "echoencountercore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
