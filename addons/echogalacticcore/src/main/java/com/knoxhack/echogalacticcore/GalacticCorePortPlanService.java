package com.knoxhack.echogalacticcore;

import java.util.List;
import java.util.Map;

public final class GalacticCorePortPlanService {
    public Map<String, Object> summary() {
        return Map.of(
                "moduleId", GalacticCoreIds.MOD_ID,
                "label", "Unofficial ECHO Platform port/fork of Galacticraft Legacy",
                "productionBoundary", "ASDK typed services only",
                "legacySourceRole", "reference implementation and MIT-derived content source",
                "phases", List.of(
                        "identity_legal",
                        "asdk_native_shell",
                        "typed_service_layer",
                        "content_registries",
                        "resource_migration",
                        "miccore_replacement",
                        "gameplay_slices",
                        "echo_integrations",
                        "testkit_parity",
                        "release_gate"
                )
        );
    }
}
