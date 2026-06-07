package com.knoxhack.echorecovery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoRecoveryFieldPlanContract {
    public static final String MODULE_ID = "echorecovery";
    public static final String ADAPTERCORE_CONTRACT_ID = "echorecovery:player/field_recovery_plan";
    public static final String REFERENCE_GRAVE_ID = "echorecovery:grave/ashfall-crash-cache-001";
    public static final String REFERENCE_OWNER_ID = "player-001";

    private EchoRecoveryFieldPlanContract() {
    }

    public static Map<String, Object> executeReferencePlan(String packId) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        plan.put("service", "echorecovery:recovery_service");
        plan.put("recoveryPlanExecuted", true);
        plan.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        plan.put("graveSnapshot", Map.of(
                "graveId", REFERENCE_GRAVE_ID,
                "ownerId", REFERENCE_OWNER_ID,
                "ownerName", "Ashfall Scout",
                "dimension", "echoashfallprotocol:wasteland_surface",
                "position", "118,64,-42",
                "graveTypeId", "echorecovery:ashfall_field_recovery_cache",
                "storedItemCount", 7,
                "xpStored", 23,
                "contaminated", true,
                "recovered", false
        ));
        plan.put("itemRules", List.of(
                itemRule("ashfall:water_ration", "RECOVER_TO_GRAVE", false),
                itemRule("ashfall:return_keystone", "PROTECTED", true),
                itemRule("minecraft:rotten_flesh", "DESTROY_ON_DEATH", false)
        ));
        plan.put("compassTarget", Map.of(
                "targetId", REFERENCE_GRAVE_ID,
                "distanceBlocks", 214,
                "signalStatus", "weather-interference",
                "holoMapLayer", "echoholomap:layer/field_route"
        ));
        plan.put("actions", List.of(
                action("create_grave_snapshot", "CREATE_SNAPSHOT", "LOW", false),
                action("mark_compass_target", "MARK_RECOVERY_TARGET", "LOW", false),
                action("preserve_saves", "PRESERVE_SAVES", "LOW", false),
                action("recover_safe_items", "RECOVER_ITEMS", "MEDIUM", true)
        ));
        plan.put("safeMode", Map.of(
                "mode", "recovery",
                "automaticExecutionAllowed", false,
                "requiresConfirmation", true,
                "destructiveActions", 0
        ));
        plan.put("diagnostics", List.of(
                "recovery.grave.captured",
                "recovery.compass.targeted",
                "recovery.rules.applied",
                "recovery.confirmation.required"
        ));
        plan.put("referenceBehavior", "recovery_builds_field_recovery_plan");
        return Map.copyOf(plan);
    }

    public static boolean referencePlanPassed(Map<String, Object> plan) {
        return Boolean.TRUE.equals(plan.get("recoveryPlanExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(plan.get("adapterCoreContract"))
                && String.valueOf(plan.get("graveSnapshot")).contains(REFERENCE_GRAVE_ID)
                && String.valueOf(plan.get("graveSnapshot")).contains("storedItemCount=7")
                && String.valueOf(plan.get("itemRules")).contains("ashfall:return_keystone")
                && String.valueOf(plan.get("compassTarget")).contains("weather-interference")
                && String.valueOf(plan.get("actions")).contains("RECOVER_ITEMS")
                && String.valueOf(plan.get("safeMode")).contains("requiresConfirmation=true")
                && String.valueOf(plan.get("diagnostics")).contains("recovery.rules.applied");
    }

    private static Map<String, Object> itemRule(String itemId, String result, boolean protectedItem) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("itemId", itemId);
        rule.put("result", result);
        rule.put("protected", protectedItem);
        return Map.copyOf(rule);
    }

    private static Map<String, Object> action(
            String id,
            String kind,
            String risk,
            boolean requiresConfirmation
    ) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("id", id);
        action.put("kind", kind);
        action.put("risk", risk);
        action.put("requiresConfirmation", requiresConfirmation);
        action.put("destructive", false);
        return Map.copyOf(action);
    }
}
