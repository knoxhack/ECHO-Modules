package com.knoxhack.echorelictech;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoRelicTechContainmentContract {
    public static final String MODULE_ID = "echorelictech";
    public static final String ADAPTERCORE_CONTRACT_ID = "echorelictech:relic/containment_plan";
    public static final String REFERENCE_RELIC_ID = "echorelictech:phase_anchor";
    public static final String REFERENCE_VAULT_ID = "echorelictech:pre_gridfall_research_vault";

    private EchoRelicTechContainmentContract() {
    }

    public static Map<String, Object> executeReferencePlan(String packId) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        plan.put("service", "echorelictech:containment_service");
        plan.put("containmentPlanExecuted", true);
        plan.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        plan.put("relicSnapshot", Map.of(
                "relicId", REFERENCE_RELIC_ID,
                "displayName", "Phase Anchor",
                "condition", "DAMAGED",
                "tier", "PROTOTYPE",
                "category", "UTILITY",
                "identified", true,
                "boundDimension", "echoashfallprotocol:wasteland_surface",
                "boundPosition", "118,64,-42"
        ));
        plan.put("vaultRoute", Map.of(
                "vaultId", REFERENCE_VAULT_ID,
                "mapLayer", "echoholomap:layer/relic_vaults",
                "structureKey", "echorelictech:worldgen/structure/pre_gridfall_research_vault",
                "requiresNullCharge", true,
                "recommendedContainment", "null_shielded_vault"
        ));
        plan.put("workbenchActions", List.of(
                action("scan_relic", "ANALYZE", "LOW", "Relic Analyzer", false),
                action("stabilize_phase_matrix", "STABILIZE", "MEDIUM", "Prototype Workbench", true),
                action("dock_null_battery", "CHARGE_NULL_CELL", "LOW", "Null Battery Dock", false),
                action("seal_in_locker", "CONTAIN", "MEDIUM", "Containment Locker", true)
        ));
        plan.put("instabilityProjection", Map.of(
                "startingInstability", 35,
                "stabilizeDelta", -12,
                "containmentDelta", -18,
                "failureThreshold", 60,
                "projectedLevel", "stable"
        ));
        plan.put("failureTable", List.of(
                failure("minor", 60, "cooldown_multiply", "Recall matrix flickered. Cooldown increased."),
                failure("medium", 30, "teleport_offset", "Destination drift detected."),
                failure("major", 10, "hostile_signal", "Nexus echo followed the recall path.")
        ));
        plan.put("diagnostics", List.of(
                "relictech.relic.scanned",
                "relictech.workbench.plan_ready",
                "relictech.instability.projected",
                "relictech.containment.confirmation_required"
        ));
        plan.put("referenceBehavior", "relictech_builds_containment_and_instability_plan");
        return Map.copyOf(plan);
    }

    public static boolean referencePlanPassed(Map<String, Object> plan) {
        return Boolean.TRUE.equals(plan.get("containmentPlanExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(plan.get("adapterCoreContract"))
                && String.valueOf(plan.get("relicSnapshot")).contains(REFERENCE_RELIC_ID)
                && String.valueOf(plan.get("vaultRoute")).contains(REFERENCE_VAULT_ID)
                && String.valueOf(plan.get("workbenchActions")).contains("STABILIZE")
                && String.valueOf(plan.get("workbenchActions")).contains("CONTAIN")
                && String.valueOf(plan.get("instabilityProjection")).contains("projectedLevel=stable")
                && String.valueOf(plan.get("failureTable")).contains("teleport_offset")
                && String.valueOf(plan.get("diagnostics")).contains("relictech.instability.projected");
    }

    private static Map<String, Object> action(
            String id,
            String kind,
            String risk,
            String station,
            boolean requiresConfirmation
    ) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("id", id);
        action.put("kind", kind);
        action.put("risk", risk);
        action.put("station", station);
        action.put("requiresConfirmation", requiresConfirmation);
        action.put("destructive", false);
        return Map.copyOf(action);
    }

    private static Map<String, Object> failure(String severity, int weight, String effect, String message) {
        Map<String, Object> failure = new LinkedHashMap<>();
        failure.put("severity", severity);
        failure.put("weight", weight);
        failure.put("effect", effect);
        failure.put("message", message);
        return Map.copyOf(failure);
    }
}
