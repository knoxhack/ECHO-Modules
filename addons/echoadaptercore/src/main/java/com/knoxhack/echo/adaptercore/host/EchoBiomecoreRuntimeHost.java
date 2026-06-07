package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echobiomecore} capabilities in the AdapterCore truth layer.
 */
public final class EchoBiomecoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echobiomecore:runtime_host";
    private static final EchoBiomecoreRuntimeHost HOST = new EchoBiomecoreRuntimeHost();

    private EchoBiomecoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echobiomecore.mission_start",
                "echobiomecore.mission_complete",
                "echobiomecore.packet_send",
                "echobiomecore.packet_receive",
                "echobiomecore.region_enter",
                "echobiomecore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
