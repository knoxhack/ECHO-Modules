package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {@code echoworldcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoWorldCoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoworldcore:runtime_host";
    private static final EchoWorldCoreRuntimeHost HOST = new EchoWorldCoreRuntimeHost();

    private EchoWorldCoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of("EchoNativeRuntimeHost.WorldState", "EchoNativeRuntimeHost.Events"),
                Set.of(
                        "worldcore.region_enter",
                        "worldcore.region_exit",
                        "worldcore.hazard_enter",
                        "worldcore.hazard_exit",
                        "worldcore.marker_place",
                        "worldcore.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
