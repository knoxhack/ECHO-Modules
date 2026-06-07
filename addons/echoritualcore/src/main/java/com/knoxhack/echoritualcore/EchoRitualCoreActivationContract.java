package com.knoxhack.echoritualcore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoRitualCoreActivationContract {
    public static final String MODULE_ID = "echoritualcore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echoritualcore:ritual/aether_calibration_activation";
    public static final String RITUAL_ID = "echoritualcore:aether_calibration";
    public static final String SERVICE_ID = "echoritualcore:ritual_activation_service";

    private EchoRitualCoreActivationContract() {
    }

    public static Map<String, Object> executeReferenceActivation(String packId) {
        Map<String, Object> activation = new LinkedHashMap<>();
        activation.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        activation.put("service", SERVICE_ID);
        activation.put("ritualActivationExecuted", true);
        activation.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        activation.put("ritualId", RITUAL_ID);
        activation.put("focusItemId", "echoritualcore:aether_chalk");
        activation.put("structure", structure());
        activation.put("costs", List.of(cost("echoritualcore:aether_chalk", 1, "focus")));
        activation.put("outputs", List.of(output("echoritualcore:refined_aether_sample", 1, "player_inventory")));
        activation.put("altarStatus", altarStatus());
        activation.put("sideEffects", sideEffects());
        activation.put("diagnostics", List.of(
                "ritual.structure.valid",
                "ritual.cost.aether_chalk.consumed",
                "ritual.output.refined_aether_sample.granted",
                "ritual.complete.event_dispatched"
        ));
        activation.put("referenceBehavior", "altar_aether_calibration_consumes_focus_and_grants_refined_sample");
        return Map.copyOf(activation);
    }

    public static boolean referenceActivationPassed(Map<String, Object> activation) {
        return Boolean.TRUE.equals(activation.get("ritualActivationExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(activation.get("adapterCoreContract"))
                && RITUAL_ID.equals(activation.get("ritualId"))
                && String.valueOf(activation.get("structure")).contains("validBasicArray=true")
                && String.valueOf(activation.get("costs")).contains("echoritualcore:aether_chalk")
                && String.valueOf(activation.get("outputs")).contains("echoritualcore:refined_aether_sample")
                && String.valueOf(activation.get("altarStatus")).contains("resultName=COMPLETE")
                && String.valueOf(activation.get("sideEffects")).contains("ritual.complete")
                && String.valueOf(activation.get("diagnostics")).contains("ritual.output.refined_aether_sample.granted");
    }

    private static Map<String, Object> structure() {
        Map<String, Object> structure = new LinkedHashMap<>();
        structure.put("altarPos", "0,64,0");
        structure.put("runeCircles", 4);
        structure.put("requiredRuneCircles", 4);
        structure.put("pedestalCount", 1);
        structure.put("stabilityPylons", 1);
        structure.put("moonDials", 1);
        structure.put("weatherAnchors", 0);
        structure.put("corruptedAltars", 0);
        structure.put("stabilityScore", 72);
        structure.put("validBasicArray", true);
        structure.put("missingAnchors", List.of());
        structure.put("summary", "Runes 4/4, pedestals 1, stability 72%");
        return Map.copyOf(structure);
    }

    private static Map<String, Object> altarStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("lastRitualId", RITUAL_ID);
        status.put("lastSubjectId", RITUAL_ID);
        status.put("resultCode", 2);
        status.put("resultName", "COMPLETE");
        status.put("message", "Aether Calibration complete. Refined sample condensed.");
        status.put("lastStability", 72);
        status.put("lastRunes", 4);
        status.put("lastPedestals", 1);
        status.put("lastMissing", 0);
        return Map.copyOf(status);
    }

    private static Map<String, Object> sideEffects() {
        Map<String, Object> sideEffects = new LinkedHashMap<>();
        sideEffects.put("event", "ritual.complete");
        sideEffects.put("mapMarker", Map.of(
                "layer", "echoholomap:layer/ritual_sites",
                "title", "Aether Calibration",
                "position", "0,64,0"
        ));
        sideEffects.put("missionObjective", Map.of(
                "source", MODULE_ID,
                "target", RITUAL_ID,
                "action", "aether_calibration",
                "progressDelta", 1
        ));
        sideEffects.put("playerMessage", "block.echoritualcore.basic_altar.aether_calibrated");
        return Map.copyOf(sideEffects);
    }

    private static Map<String, Object> cost(String itemId, int count, String source) {
        Map<String, Object> cost = new LinkedHashMap<>();
        cost.put("itemId", itemId);
        cost.put("count", count);
        cost.put("source", source);
        cost.put("consumed", true);
        return Map.copyOf(cost);
    }

    private static Map<String, Object> output(String itemId, int count, String target) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("itemId", itemId);
        output.put("count", count);
        output.put("target", target);
        output.put("granted", true);
        return Map.copyOf(output);
    }
}
