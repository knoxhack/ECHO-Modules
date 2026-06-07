package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {@code echoruntimeguard} capabilities in the AdapterCore truth layer.
 */
public final class EchoRuntimeGuardRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoruntimeguard:runtime_host";
    private static final EchoRuntimeGuardRuntimeHost HOST = new EchoRuntimeGuardRuntimeHost();

    private EchoRuntimeGuardRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of("EchoNativeRuntimeHost.Events"),
                Set.of(
                        "runtimeguard.budget_exceeded",
                        "runtimeguard.throttle_applied",
                        "runtimeguard.work_recorded"),
                Set.of(),
                false,
                false,
                true));
    }
}
