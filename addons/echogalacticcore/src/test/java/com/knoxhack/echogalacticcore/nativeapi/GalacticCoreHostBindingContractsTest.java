package com.knoxhack.echogalacticcore.nativeapi;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostBindingContracts;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostCallbacks;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostExecutionBridge;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeGateway;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalacticCoreHostBindingContractsTest {
    private final GalacticCoreRuntimeService runtime = new GalacticCoreRuntimeService();
    private final GalacticCoreRuntimeGateway gateway = new GalacticCoreRuntimeGateway(runtime);
    private final GalacticCoreRuntimeAdapters adapters = new GalacticCoreRuntimeAdapters(runtime, gateway);
    private final GalacticCoreHostCallbacks callbacks = new GalacticCoreHostCallbacks(runtime, gateway, adapters);
    private final GalacticCoreHostExecutionBridge executionBridge = new GalacticCoreHostExecutionBridge(callbacks);
    private final GalacticCoreHostBindingContracts contracts = new GalacticCoreHostBindingContracts(executionBridge);

    @Test
    void bindingContractsAssignLiveHostOperationsToTypedAsdkOwners() {
        List<GalacticCoreHostBindingContracts.HostBindingContract> bindings = contracts.releaseHostBindingSmokeContracts();

        assertEquals(5, bindings.size());
        assertTrue(bindings.stream().anyMatch(binding ->
                GalacticCoreIds.id("host_binding/world_dimension_transfer").equals(binding.target())
                        && "echo.native.worldgen".equals(binding.serviceId())
                        && "placeStructure".equals(binding.action())
                        && "world".equals(binding.evidence().get("hostSurface"))
        ));
        assertTrue(bindings.stream().anyMatch(binding ->
                GalacticCoreIds.id("host_binding/entity_boss_spawn").equals(binding.target())
                        && "echo.native.capabilities".equals(binding.serviceId())
                        && "registerIntegration".equals(binding.action())
                        && "entity".equals(binding.evidence().get("hostSurface"))
        ));
        assertEquals(3, bindings.stream()
                .filter(binding -> "echo.native.screens".equals(binding.serviceId()))
                .filter(binding -> "registerMenu".equals(binding.action()))
                .filter(binding -> "screen".equals(binding.evidence().get("hostSurface")))
                .count());
    }

    @Test
    void bindingEvidencePreservesExecutionTargetsAndTypedReceiptRequirement() {
        List<GalacticCoreHostBindingContracts.HostBindingContract> bindings = contracts.releaseHostBindingSmokeContracts();

        assertTrue(bindings.stream().allMatch(binding ->
                "galacticraft_legacy_concrete_host_bindings".equals(binding.evidence().get("source"))
                        && Boolean.TRUE.equals(binding.evidence().get("typedReceiptsOnly"))
                        && binding.evidence().containsKey("executionTarget")
                        && !binding.requiredHostActions().isEmpty()
        ));
        assertTrue(contracts.evidence().get("replaces").toString().contains("WorldProvider"));
    }
}
