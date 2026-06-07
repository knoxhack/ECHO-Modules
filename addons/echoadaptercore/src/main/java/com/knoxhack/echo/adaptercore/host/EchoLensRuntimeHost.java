package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {@code echolens} capabilities in the AdapterCore truth layer.
 */
public final class EchoLensRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echolens:runtime_host";
    private static final EchoLensRuntimeHost HOST = new EchoLensRuntimeHost();

    private EchoLensRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of("EchoNativeRuntimeHost.Events"),
                Set.of(
                        "lens.scan",
                        "lens.deep_scan",
                        "lens.discovery",
                        "lens.marker_place"),
                Set.of(),
                false,
                false,
                true));
    }
}
