package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {@code echodatacore} capabilities in the AdapterCore truth layer.
 */
public final class EchoDataCoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echodatacore:runtime_host";
    private static final EchoDataCoreRuntimeHost HOST = new EchoDataCoreRuntimeHost();

    private EchoDataCoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of("EchoNativeRuntimeHost.SaveData"),
                Set.of("datacore.player_save", "datacore.world_save", "datacore.team_save"),
                Set.of(),
                false,
                true,
                false));
    }
}
