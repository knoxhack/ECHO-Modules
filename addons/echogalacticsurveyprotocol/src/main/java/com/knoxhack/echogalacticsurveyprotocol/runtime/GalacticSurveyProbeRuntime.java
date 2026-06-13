package com.knoxhack.echogalacticsurveyprotocol.runtime;

import com.knoxhack.echogalacticsurveyprotocol.contract.GalacticSurveyRuntimeContracts;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GalacticSurveyProbeRuntime {
    public record ProbeProfile(
            String id,
            String itemId,
            int rangeSectors,
            String scanTier,
            String bestUse,
            String unlockProof
    ) {
        public Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("itemId", itemId);
            result.put("rangeSectors", rangeSectors);
            result.put("scanTier", scanTier);
            result.put("bestUse", bestUse);
            result.put("unlockProof", unlockProof);
            return Map.copyOf(result);
        }
    }

    private static final List<ProbeProfile> PROBES = List.of(
            new ProbeProfile("starter_probe", "starter_probe", 1, "trace", "cheap near-sector signal discovery", "item:starter_probe"),
            new ProbeProfile("long_range_probe", "long_range_probe", 3, "partial", "deep-sector route scouting", "item:long_range_probe"),
            new ProbeProfile("salvage_mapper_probe", "long_range_probe", 2, "confirmed", "derelict and debris belt classification", "salvage:derelict_relay_osprey"),
            new ProbeProfile("anomaly_lens_probe", "deep_space_lens", 3, "resolved", "rare anomaly classification", "item:deep_space_lens")
    );

    private GalacticSurveyProbeRuntime() {
    }

    public static List<ProbeProfile> probes() {
        return PROBES;
    }

    public static Optional<ProbeProfile> probeFor(String id) {
        return PROBES.stream().filter(probe -> probe.id().equals(id)).findFirst();
    }

    public static List<String> launchableProbes(Collection<String> inventoryItemIds, Collection<String> completedProofs) {
        return PROBES.stream()
                .filter(probe -> inventoryItemIds.contains(probe.itemId()) || completedProofs.contains(probe.unlockProof()))
                .map(ProbeProfile::id)
                .toList();
    }

    public static Map<String, Object> adapterManifest() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", "echo.galactic_survey.probe_runtime.v1");
        result.put("moduleId", GalacticSurveyRuntimeContracts.MODULE_ID);
        result.put("probeIds", GalacticSurveyRuntimeContracts.PROBE_IDS);
        result.put("probes", PROBES.stream().map(ProbeProfile::asMap).toList());
        result.put("scanConfidenceTiers", List.of("unknown", "trace", "partial", "confirmed", "resolved"));
        result.put("gates", List.of("itemId", "rangeSectors", "scanTier", "unlockProof"));
        return Map.copyOf(result);
    }
}
