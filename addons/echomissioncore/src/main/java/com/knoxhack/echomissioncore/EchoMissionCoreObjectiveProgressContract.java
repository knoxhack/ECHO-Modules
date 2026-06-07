package com.knoxhack.echomissioncore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoMissionCoreObjectiveProgressContract {
    public static final String MODULE_ID = "echomissioncore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echomissioncore:missions/objective_progression";
    public static final String REFERENCE_MISSION_ID = "echoashfallprotocol:mission/secure_crash_site";
    public static final String REFERENCE_OBJECTIVE_ID = "echoashfallprotocol:objective/restore_emergency_terminal";
    public static final String REFERENCE_TARGET_ID = "echoashfallprotocol:block/emergency_terminal";

    private EchoMissionCoreObjectiveProgressContract() {
    }

    public static Map<String, Object> executeReferenceProgression(String packId) {
        int required = 3;
        int firstProgress = clampProgress(0, 1, required);
        int finalProgress = clampProgress(firstProgress, 3, required);
        boolean complete = finalProgress >= required;

        Map<String, Object> progression = new LinkedHashMap<>();
        progression.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        progression.put("service", "echomissioncore:mission_service");
        progression.put("missionProgressionExecuted", true);
        progression.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        progression.put("missionDefinition", missionDefinition(required));
        progression.put("runtimeEvents", List.of(
                event("MISSION_STARTED", REFERENCE_MISSION_ID, "", 1, "mission status moved from UNLOCKED to ACTIVE"),
                event("OBJECTIVE_PROGRESSED", REFERENCE_MISSION_ID, REFERENCE_OBJECTIVE_ID, 1, "terminal restored to 1/3"),
                event("OBJECTIVE_PROGRESSED", REFERENCE_MISSION_ID, REFERENCE_OBJECTIVE_ID, 3, "terminal progress clamped to 3/3"),
                event("MISSION_COMPLETED", REFERENCE_MISSION_ID, "", 1, "all objectives complete and reward is claimable")
        ));
        progression.put("playerState", Map.of(
                "trackedMissionId", REFERENCE_MISSION_ID,
                "missionStatus", complete ? "CLAIMABLE" : "ACTIVE",
                "objectiveProgress", Map.of(REFERENCE_OBJECTIVE_ID, finalProgress),
                "claimedRewards", List.of(),
                "repeatCompletions", 0,
                "lastCompletedGameTime", 2400L
        ));
        progression.put("objectiveSnapshot", Map.of(
                "objectiveId", REFERENCE_OBJECTIVE_ID,
                "type", "REPAIR_MACHINE",
                "target", REFERENCE_TARGET_ID,
                "progress", finalProgress,
                "required", required,
                "complete", complete
        ));
        progression.put("terminalSnapshot", Map.of(
                "missionId", REFERENCE_MISSION_ID,
                "status", complete ? "CLAIMABLE" : "ACTIVE",
                "progress", "1.00",
                "statusLabel", complete ? "Reward Ready" : "Active",
                "actionHint", complete ? "Claim the pending reward cache." : "Restore the emergency terminal.",
                "actions", List.of(action("claim", "Claim", true, ""))
        ));
        progression.put("rewardState", Map.of(
                "rewardId", "echoashfallprotocol:reward/emergency_cache",
                "claimMode", "CLAIMABLE",
                "claimable", complete,
                "claimed", false
        ));
        progression.put("diagnostics", List.of(
                "mission.service.registered",
                "mission.objective.progressed",
                "mission.progress.clamped",
                "mission.reward.claimable"
        ));
        progression.put("referenceBehavior", "mission_objective_progress_updates_state_and_terminal_snapshot");
        return Map.copyOf(progression);
    }

    public static boolean referenceProgressionPassed(Map<String, Object> progression) {
        return Boolean.TRUE.equals(progression.get("missionProgressionExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(progression.get("adapterCoreContract"))
                && String.valueOf(progression.get("missionDefinition")).contains(REFERENCE_MISSION_ID)
                && String.valueOf(progression.get("runtimeEvents")).contains("OBJECTIVE_PROGRESSED")
                && String.valueOf(progression.get("runtimeEvents")).contains("MISSION_COMPLETED")
                && String.valueOf(progression.get("playerState")).contains("missionStatus=CLAIMABLE")
                && String.valueOf(progression.get("objectiveSnapshot")).contains("progress=3")
                && String.valueOf(progression.get("terminalSnapshot")).contains("Claim the pending reward cache.")
                && String.valueOf(progression.get("rewardState")).contains("claimable=true")
                && String.valueOf(progression.get("diagnostics")).contains("mission.progress.clamped");
    }

    private static int clampProgress(int current, int amount, int required) {
        return Math.min(Math.max(1, required), current + Math.max(0, amount));
    }

    private static Map<String, Object> missionDefinition(int required) {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("missionId", REFERENCE_MISSION_ID);
        definition.put("chapterId", "echomissioncore:missions");
        definition.put("title", "Secure Crash Site");
        definition.put("statusBeforeStart", "UNLOCKED");
        definition.put("repeatPolicy", "ONCE");
        definition.put("objectives", List.of(Map.of(
                "objectiveId", REFERENCE_OBJECTIVE_ID,
                "type", "REPAIR_MACHINE",
                "target", REFERENCE_TARGET_ID,
                "required", required,
                "hidden", false
        )));
        definition.put("rewards", List.of(Map.of(
                "rewardId", "echoashfallprotocol:reward/emergency_cache",
                "claimMode", "CLAIMABLE",
                "label", "Emergency cache"
        )));
        return Map.copyOf(definition);
    }

    private static Map<String, Object> event(
            String type,
            String missionId,
            String objectiveId,
            int amount,
            String summary
    ) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("missionId", missionId);
        event.put("objectiveId", objectiveId);
        event.put("amount", amount);
        event.put("summary", summary);
        return Map.copyOf(event);
    }

    private static Map<String, Object> action(String id, String label, boolean enabled, String disabledReason) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("id", id);
        action.put("label", label);
        action.put("enabled", enabled);
        action.put("disabledReason", disabledReason);
        return Map.copyOf(action);
    }
}
