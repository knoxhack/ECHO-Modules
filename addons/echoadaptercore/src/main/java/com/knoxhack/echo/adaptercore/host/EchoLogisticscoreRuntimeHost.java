package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echologisticscore} capabilities in the AdapterCore truth layer.
 */
public final class EchoLogisticscoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echologisticscore:runtime_host";
    private static final EchoLogisticscoreRuntimeHost HOST = new EchoLogisticscoreRuntimeHost();

    private EchoLogisticscoreRuntimeHost() {
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
                "echologisticscore.mission_start",
                "echologisticscore.mission_complete",
                "echologisticscore.packet_send",
                "echologisticscore.packet_receive",
                "echologisticscore.region_enter",
                "echologisticscore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
