package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echodifficultycore} capabilities in the AdapterCore truth layer.
 */
public final class EchoDifficultycoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echodifficultycore:runtime_host";
    private static final EchoDifficultycoreRuntimeHost HOST = new EchoDifficultycoreRuntimeHost();

    private EchoDifficultycoreRuntimeHost() {
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
                "echodifficultycore.mission_start",
                "echodifficultycore.mission_complete",
                "echodifficultycore.packet_send",
                "echodifficultycore.packet_receive",
                "echodifficultycore.region_enter",
                "echodifficultycore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
