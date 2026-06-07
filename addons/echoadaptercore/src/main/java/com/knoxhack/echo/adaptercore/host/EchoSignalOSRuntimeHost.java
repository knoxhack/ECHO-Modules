package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {@code echosignalos} capabilities in the AdapterCore truth layer.
 */
public final class EchoSignalOSRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echosignalos:runtime_host";
    private static final EchoSignalOSRuntimeHost HOST = new EchoSignalOSRuntimeHost();

    private EchoSignalOSRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of("EchoNativeRuntimeHost.Events", "EchoNativeRuntimeHost.Packets"),
                Set.of(
                        "signalos.terminal_open",
                        "signalos.terminal_close",
                        "signalos.app_launch",
                        "signalos.mission_select"),
                Set.of(),
                false,
                false,
                true));
    }
}
