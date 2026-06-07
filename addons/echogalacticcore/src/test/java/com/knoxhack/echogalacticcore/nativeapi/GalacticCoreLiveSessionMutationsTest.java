package com.knoxhack.echogalacticcore.nativeapi;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostBindingContracts;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostCallbacks;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostExecutionBridge;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreLiveHostAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreLiveHostEntrypoints;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreLiveSessionMutations;
import com.knoxhack.echogalacticcore.runtime.GalacticCorePlatformExecutors;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeGateway;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalacticCoreLiveSessionMutationsTest {
    private final GalacticCoreRuntimeService runtime = new GalacticCoreRuntimeService();
    private final GalacticCoreRuntimeGateway gateway = new GalacticCoreRuntimeGateway(runtime);
    private final GalacticCoreRuntimeAdapters runtimeAdapters = new GalacticCoreRuntimeAdapters(runtime, gateway);
    private final GalacticCoreHostCallbacks callbacks = new GalacticCoreHostCallbacks(runtime, gateway, runtimeAdapters);
    private final GalacticCoreHostExecutionBridge executionBridge = new GalacticCoreHostExecutionBridge(callbacks);
    private final GalacticCoreHostBindingContracts bindings = new GalacticCoreHostBindingContracts(executionBridge);
    private final GalacticCoreLiveHostAdapters liveAdapters = new GalacticCoreLiveHostAdapters(bindings);
    private final GalacticCoreLiveHostEntrypoints entrypoints = new GalacticCoreLiveHostEntrypoints(liveAdapters);
    private final GalacticCorePlatformExecutors platformExecutors = new GalacticCorePlatformExecutors(entrypoints);

    @Test
    void liveSessionBridgeDispatchesNonDryRunMutationRequestsToHostSink() {
        List<GalacticCoreLiveSessionMutations.LiveSessionMutationRequest> requests = new ArrayList<>();
        GalacticCoreLiveSessionMutations.HostMutationSink sink = new RecordingHostMutationSink(requests);
        GalacticCoreLiveSessionMutations liveSessionMutations = new GalacticCoreLiveSessionMutations(platformExecutors, sink);

        GalacticCoreLiveSessionMutations.LiveSessionMutationResult world = liveSessionMutations.commitWorldTransfer(
                new GalacticCoreLiveSessionMutations.LiveSessionMutationContext("session/world-test", "executor/world-test", "player/test", "server")
        );
        GalacticCoreLiveSessionMutations.LiveSessionMutationResult entity = liveSessionMutations.commitBossSpawn(
                new GalacticCoreLiveSessionMutations.LiveSessionMutationContext("session/entity-test", "executor/entity-test", "boss/test", "server")
        );
        GalacticCoreLiveSessionMutations.LiveSessionMutationResult screen = liveSessionMutations.openScreenHost(
                "holomap_routes",
                new GalacticCoreLiveSessionMutations.LiveSessionMutationContext("session/screen-test", "executor/screen-test", "player/test", "client")
        );

        assertEquals(3, requests.size());
        assertTrue(world.accepted());
        assertEquals("live_session_mutation_accepted", world.status());
        assertEquals(GalacticCoreIds.id("live_session/world_dimension_transfer"), world.action().target());
        assertEquals(GalacticCoreIds.id("live_session/entity_boss_spawn"), entity.action().target());
        assertEquals(GalacticCoreIds.id("live_session/screen_holomap_routes"), screen.action().target());
        assertFalse((boolean) world.action().evidence().get("dryRun"));
        assertTrue((boolean) world.action().evidence().get("mutatesMinecraftObjects"));
        assertTrue(world.action().evidence().get("platformTarget").toString().contains("platform_executor/world_dimension_transfer"));
        assertEquals("loadDestinationAndPlacePlayer", world.request().payload().hostApi());
        assertTrue(world.request().payload().stateKeys().contains("landing_anchor"));
        assertTrue(world.request().payload().safetyChecks().contains("chunk_ticketed"));
        assertEquals("spawnBossEntityAndAttachEncounter", entity.request().payload().hostApi());
        assertTrue(entity.request().payload().stateKeys().contains("encounter_state"));
        assertEquals("openScreenAndMountState", screen.request().payload().hostApi());
        assertTrue(screen.request().payload().safetyChecks().contains("widget_actions_bound"));
        assertTrue(entity.hostReceipt().committedSteps().contains("instantiate_boss_entity"));
        assertTrue(screen.hostReceipt().committedSteps().contains("mount_widgets"));
    }

    @Test
    void contractOnlySinkPublishesHonestSmokeEvidence() {
        GalacticCoreLiveSessionMutations liveSessionMutations = new GalacticCoreLiveSessionMutations(
                platformExecutors,
                GalacticCoreLiveSessionMutations.contractOnlyHostSink()
        );

        List<GalacticCoreLiveSessionMutations.LiveSessionMutationResult> results = liveSessionMutations.releaseLiveSessionMutationSmokeResults();

        assertEquals(5, results.size());
        assertTrue(results.stream().allMatch(result ->
                result.accepted()
                        && "galacticraft_legacy_live_session_mutation_bridge".equals(result.action().evidence().get("source"))
                        && Boolean.TRUE.equals(result.action().evidence().get("typedReceiptsOnly"))
                        && Boolean.TRUE.equals(result.action().evidence().get("hostOwnedMutationBoundary"))
                        && Boolean.FALSE.equals(result.action().evidence().get("mutatesMinecraftObjects"))
                        && result.action().evidence().containsKey("platformTarget")
                        && result.action().evidence().containsKey("entrypointTarget")
                        && result.action().evidence().containsKey("hostReceiptId")
                        && result.action().evidence().containsKey("payloadRequiredSteps")
                        && result.action().evidence().containsKey("payloadSafetyChecks")
                        && !result.request().payload().requiredSteps().isEmpty()
                        && !result.request().payload().stateKeys().isEmpty()
        ));
        assertTrue(liveSessionMutations.evidence().get("replaces").toString().contains("legacy direct teleport"));
    }

    private static final class RecordingHostMutationSink implements GalacticCoreLiveSessionMutations.HostMutationSink {
        private final List<GalacticCoreLiveSessionMutations.LiveSessionMutationRequest> requests;

        private RecordingHostMutationSink(List<GalacticCoreLiveSessionMutations.LiveSessionMutationRequest> requests) {
            this.requests = requests;
        }

        @Override
        public String sinkId() {
            return GalacticCoreIds.id("recording_live_session_sink");
        }

        @Override
        public GalacticCoreLiveSessionMutations.HostMutationReceipt commit(
                GalacticCoreLiveSessionMutations.LiveSessionMutationRequest request
        ) {
            requests.add(request);
            return new GalacticCoreLiveSessionMutations.HostMutationReceipt(
                    "recording/" + request.context().sessionId(),
                    sinkId(),
                    request.platformResult().accepted(),
                    true,
                    request.platformResult().entrypointResult().completedSteps(),
                    Map.of(
                            "source", "recording_live_session_sink",
                            "hostRuntimeObserved", true,
                            "platformTarget", request.platformResult().action().target()
                    )
            );
        }
    }
}
