package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {@code echonetcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoNetCoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echonetcore:runtime_host";
    private static final EchoNetCoreRuntimeHost HOST = new EchoNetCoreRuntimeHost();

    private EchoNetCoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of("EchoNativeRuntimeHost.Packets"),
                Set.of(
                        "netcore.packet_send",
                        "netcore.packet_receive",
                        "netcore.sync_payload"),
                Set.of(),
                false,
                false,
                true));
    }
}
