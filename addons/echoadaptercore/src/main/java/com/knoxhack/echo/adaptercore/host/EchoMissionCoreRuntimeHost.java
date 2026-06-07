package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoContentAliasResolver;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {@code echomissioncore} capabilities in the AdapterCore truth layer.
 *
 * <p>MissionCore does not directly implement gameplay host services; it consumes
 * events through {@code EchoMissionCoreTruthBridge} and declares what actions
 * and content IDs it supports so the dispatcher can route mission-advancing
 * events correctly.
 */
public final class EchoMissionCoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echomissioncore:runtime_host";
    private static final EchoMissionCoreRuntimeHost HOST = new EchoMissionCoreRuntimeHost();
    private static final Set<String> ACTION_IDS = Set.of(
            "mission.start",
            "mission.complete",
            "mission.claim_reward",
            "mission.objective_progress",
            "mission.chapter_unlock");

    private EchoMissionCoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of("EchoNativeRuntimeHost.Events"),
                ACTION_IDS,
                Set.of(),
                true,
                true,
                true));
    }
}
