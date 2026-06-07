package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echopowercore} capabilities in the AdapterCore truth layer.
 */
public final class EchoPowercoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echopowercore:runtime_host";
    private static final EchoPowercoreRuntimeHost HOST = new EchoPowercoreRuntimeHost();

    private EchoPowercoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echopowercore.packet_send",
                "echopowercore.packet_receive",
                "echopowercore.region_enter",
                "echopowercore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
