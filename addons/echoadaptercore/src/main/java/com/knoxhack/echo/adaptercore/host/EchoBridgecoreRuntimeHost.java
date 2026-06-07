package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echobridgecore} capabilities in the AdapterCore truth layer.
 */
public final class EchoBridgecoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echobridgecore:runtime_host";
    private static final EchoBridgecoreRuntimeHost HOST = new EchoBridgecoreRuntimeHost();

    private EchoBridgecoreRuntimeHost() {
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
                "echobridgecore.mission_start",
                "echobridgecore.mission_complete",
                "echobridgecore.packet_send",
                "echobridgecore.packet_receive",
                "echobridgecore.region_enter",
                "echobridgecore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
