package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echocreaturecore} capabilities in the AdapterCore truth layer.
 */
public final class EchoCreaturecoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echocreaturecore:runtime_host";
    private static final EchoCreaturecoreRuntimeHost HOST = new EchoCreaturecoreRuntimeHost();

    private EchoCreaturecoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echocreaturecore.region_enter",
                "echocreaturecore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
