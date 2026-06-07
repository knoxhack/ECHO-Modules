package com.knoxhack.echogalacticcore.nativeapi;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostBindingContracts;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostCallbacks;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostExecutionBridge;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreLiveHostAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreLiveHostEntrypoints;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeGateway;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalacticCoreLiveHostEntrypointsTest {
    private final GalacticCoreRuntimeService runtime = new GalacticCoreRuntimeService();
    private final GalacticCoreRuntimeGateway gateway = new GalacticCoreRuntimeGateway(runtime);
    private final GalacticCoreRuntimeAdapters runtimeAdapters = new GalacticCoreRuntimeAdapters(runtime, gateway);
    private final GalacticCoreHostCallbacks callbacks = new GalacticCoreHostCallbacks(runtime, gateway, runtimeAdapters);
    private final GalacticCoreHostExecutionBridge executionBridge = new GalacticCoreHostExecutionBridge(callbacks);
    private final GalacticCoreHostBindingContracts bindings = new GalacticCoreHostBindingContracts(executionBridge);
    private final GalacticCoreLiveHostAdapters liveAdapters = new GalacticCoreLiveHostAdapters(bindings);
    private final GalacticCoreLiveHostEntrypoints entrypoints = new GalacticCoreLiveHostEntrypoints(liveAdapters);

    @Test
    void entrypointsExposeCallableWorldEntityAndScreenResults() {
        GalacticCoreLiveHostEntrypoints.LiveHostInvocationResult world = entrypoints.executeWorldTransfer(
                new GalacticCoreLiveHostEntrypoints.LiveHostInvocation("world/test", "player/test", "server")
        );
        GalacticCoreLiveHostEntrypoints.LiveHostInvocationResult entity = entrypoints.executeBossSpawn(
                new GalacticCoreLiveHostEntrypoints.LiveHostInvocation("entity/test", "boss/test", "server")
        );
        GalacticCoreLiveHostEntrypoints.LiveHostInvocationResult screen = entrypoints.openScreenHost(
                "holomap_routes",
                new GalacticCoreLiveHostEntrypoints.LiveHostInvocation("screen/test", "player/test", "client")
        );

        assertTrue(world.accepted());
        assertEquals(GalacticCoreIds.id("live_callback/world_dimension_transfer"), world.action().target());
        assertTrue(world.completedSteps().contains("place_player_at_anchor"));
        assertTrue(entity.completedSteps().contains("instantiate_boss_entity"));
        assertEquals(GalacticCoreIds.id("live_callback/entity_boss_spawn"), entity.action().target());
        assertTrue(screen.completedSteps().contains("mount_widgets"));
        assertEquals(GalacticCoreIds.id("live_callback/screen_holomap_routes"), screen.action().target());
    }

    @Test
    void smokeResultsPreserveAdapterAndBindingEvidence() {
        List<GalacticCoreLiveHostEntrypoints.LiveHostInvocationResult> results = entrypoints.releaseLiveHostEntrypointSmokeResults();

        assertEquals(5, results.size());
        assertTrue(results.stream().allMatch(result ->
                result.accepted()
                        && "galacticraft_legacy_live_host_entrypoints".equals(result.action().evidence().get("source"))
                        && Boolean.TRUE.equals(result.action().evidence().get("typedReceiptsOnly"))
                        && result.action().evidence().containsKey("adapterTarget")
                        && result.action().evidence().containsKey("bindingTarget")
                        && result.action().evidence().containsKey("hostEntrypoint")
        ));
        assertTrue(entrypoints.evidence().get("replaces").toString().contains("WorldProvider"));
    }
}
