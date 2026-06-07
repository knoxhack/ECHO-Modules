package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoeconomycore} capabilities in the AdapterCore truth layer.
 */
public final class EchoEconomycoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoeconomycore:runtime_host";
    private static final EchoEconomycoreRuntimeHost HOST = new EchoEconomycoreRuntimeHost();

    private EchoEconomycoreRuntimeHost() {
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
                "echoeconomycore.mission_start",
                "echoeconomycore.mission_complete",
                "echoeconomycore.region_enter",
                "echoeconomycore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
