package com.knoxhack.echogalacticcore.nativeapi;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostBindingContracts;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostCallbacks;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostExecutionBridge;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreLiveHostAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreLiveHostEntrypoints;
import com.knoxhack.echogalacticcore.runtime.GalacticCorePlatformExecutors;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeGateway;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalacticCorePlatformExecutorsTest {
    private final GalacticCoreRuntimeService runtime = new GalacticCoreRuntimeService();
    private final GalacticCoreRuntimeGateway gateway = new GalacticCoreRuntimeGateway(runtime);
    private final GalacticCoreRuntimeAdapters runtimeAdapters = new GalacticCoreRuntimeAdapters(runtime, gateway);
    private final GalacticCoreHostCallbacks callbacks = new GalacticCoreHostCallbacks(runtime, gateway, runtimeAdapters);
    private final GalacticCoreHostExecutionBridge executionBridge = new GalacticCoreHostExecutionBridge(callbacks);
    private final GalacticCoreHostBindingContracts bindings = new GalacticCoreHostBindingContracts(executionBridge);
    private final GalacticCoreLiveHostAdapters liveAdapters = new GalacticCoreLiveHostAdapters(bindings);
    private final GalacticCoreLiveHostEntrypoints entrypoints = new GalacticCoreLiveHostEntrypoints(liveAdapters);
    private final GalacticCorePlatformExecutors executors = new GalacticCorePlatformExecutors(entrypoints);

    @Test
    void executorFacadeProducesWorldEntityAndScreenReceipts() {
        GalacticCorePlatformExecutors.PlatformExecutionResult world = executors.executeWorldTransfer(
                new GalacticCorePlatformExecutors.PlatformExecutionContext("executor/world-test", "player/test", "server", true)
        );
        GalacticCorePlatformExecutors.PlatformExecutionResult entity = executors.executeBossSpawn(
                new GalacticCorePlatformExecutors.PlatformExecutionContext("executor/entity-test", "boss/test", "server", true)
        );
        GalacticCorePlatformExecutors.PlatformExecutionResult screen = executors.openScreenHost(
                "holomap_routes",
                new GalacticCorePlatformExecutors.PlatformExecutionContext("executor/screen-test", "player/test", "client", true)
        );

        assertTrue(world.accepted());
        assertEquals("platform_executor_dry_run_ready", world.status());
        assertEquals(GalacticCoreIds.id("platform_executor/world_dimension_transfer"), world.action().target());
        assertTrue(world.action().evidence().get("entrypointTarget").toString().contains("live_callback/world_dimension_transfer"));
        assertEquals(GalacticCoreIds.id("platform_executor/entity_boss_spawn"), entity.action().target());
        assertTrue(entity.entrypointResult().completedSteps().contains("instantiate_boss_entity"));
        assertEquals(GalacticCoreIds.id("platform_executor/screen_holomap_routes"), screen.action().target());
        assertTrue(screen.executedSteps().contains("queue_platform_mutation_receipt"));
    }

    @Test
    void smokeResultsStayAsdkSafeUntilTheHostMutationBoundary() {
        List<GalacticCorePlatformExecutors.PlatformExecutionResult> results = executors.releasePlatformExecutorSmokeResults();

        assertEquals(5, results.size());
        assertTrue(results.stream().allMatch(result ->
                result.accepted()
                        && "galacticraft_legacy_platform_executor_facade".equals(result.action().evidence().get("source"))
                        && Boolean.TRUE.equals(result.action().evidence().get("typedReceiptsOnly"))
                        && Boolean.TRUE.equals(result.action().evidence().get("platformMutationDeferred"))
                        && Boolean.FALSE.equals(result.action().evidence().get("mutatesMinecraftObjects"))
                        && result.action().evidence().containsKey("entrypointTarget")
                        && result.action().evidence().containsKey("adapterTarget")
                        && result.action().evidence().containsKey("bindingTarget")
        ));
        assertFalse((boolean) executors.evidence().get("mutatesMinecraftObjects"));
        assertTrue(executors.evidence().get("replaces").toString().contains("executor boundary"));
    }
}
