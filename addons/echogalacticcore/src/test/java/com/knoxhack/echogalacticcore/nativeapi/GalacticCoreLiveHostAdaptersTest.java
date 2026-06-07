package com.knoxhack.echogalacticcore.nativeapi;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostBindingContracts;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostCallbacks;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostExecutionBridge;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreLiveHostAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeGateway;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalacticCoreLiveHostAdaptersTest {
    private final GalacticCoreRuntimeService runtime = new GalacticCoreRuntimeService();
    private final GalacticCoreRuntimeGateway gateway = new GalacticCoreRuntimeGateway(runtime);
    private final GalacticCoreRuntimeAdapters runtimeAdapters = new GalacticCoreRuntimeAdapters(runtime, gateway);
    private final GalacticCoreHostCallbacks callbacks = new GalacticCoreHostCallbacks(runtime, gateway, runtimeAdapters);
    private final GalacticCoreHostExecutionBridge executionBridge = new GalacticCoreHostExecutionBridge(callbacks);
    private final GalacticCoreHostBindingContracts bindings = new GalacticCoreHostBindingContracts(executionBridge);
    private final GalacticCoreLiveHostAdapters liveAdapters = new GalacticCoreLiveHostAdapters(bindings);

    @Test
    void liveAdaptersExposeWorldEntityAndScreenExecutionPlans() {
        List<GalacticCoreLiveHostAdapters.LiveHostAdapterPlan> plans = liveAdapters.releaseLiveHostAdapterSmokePlans();

        assertEquals(5, plans.size());
        assertTrue(plans.stream().anyMatch(plan ->
                GalacticCoreIds.id("live_host/world_dimension_transfer").equals(plan.target())
                        && "echo.native.worldgen".equals(plan.serviceId())
                        && plan.executorSteps().contains("place_player_at_anchor")
        ));
        assertTrue(plans.stream().anyMatch(plan ->
                GalacticCoreIds.id("live_host/entity_boss_spawn").equals(plan.target())
                        && "echo.native.capabilities".equals(plan.serviceId())
                        && plan.executorSteps().contains("instantiate_boss_entity")
        ));
        assertEquals(3, plans.stream()
                .filter(plan -> "echo.native.screens".equals(plan.serviceId()))
                .filter(plan -> "open".equals(plan.action()))
                .filter(plan -> plan.executorSteps().contains("mount_widgets"))
                .count());
    }

    @Test
    void liveAdapterEvidencePointsBackToBindingContracts() {
        List<GalacticCoreLiveHostAdapters.LiveHostAdapterPlan> plans = liveAdapters.releaseLiveHostAdapterSmokePlans();

        assertTrue(plans.stream().allMatch(plan ->
                "galacticraft_legacy_live_host_adapters".equals(plan.evidence().get("source"))
                        && Boolean.TRUE.equals(plan.evidence().get("typedReceiptsOnly"))
                        && plan.evidence().containsKey("bindingTarget")
                        && plan.evidence().containsKey("bindingOwnerService")
                        && !plan.saveDataTarget().isBlank()
        ));
        assertTrue(liveAdapters.evidence().get("replaces").toString().contains("WorldProvider"));
    }
}
