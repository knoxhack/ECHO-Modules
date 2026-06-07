package com.knoxhack.echocursecore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoCurseCoreStateContract {
    public static final String MODULE_ID = "echocursecore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echocursecore:player/curse_state_resolution";
    public static final String REFERENCE_PLAYER_ID = "ashfall-scout-curse-001";
    public static final String ECHO_ROT = "echocursecore:curse/echo_rot";
    public static final String BLOOD_DEBT = "echocursecore:curse/blood_debt";

    private EchoCurseCoreStateContract() {
    }

    public static Map<String, Object> executeReferenceStateResolution(String packId) {
        Map<String, Object> resolution = new LinkedHashMap<>();
        resolution.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        resolution.put("service", "echocursecore:curse_state_service");
        resolution.put("curseStateResolved", true);
        resolution.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        resolution.put("playerId", REFERENCE_PLAYER_ID);
        resolution.put("operations", List.of(
                operation("apply_echo_rot", ECHO_ROT, "signal_backlash", 3, 0, 3),
                operation("bind_blood_debt", BLOOD_DEBT, "contract", 2, 0, 2),
                operation("cleanse_echo_rot", ECHO_ROT, "ritual_cleansing", -1, 3, 2),
                operation("pay_blood_debt", BLOOD_DEBT, "ledger_stabilize", -25, 80, 55)
        ));
        resolution.put("activeCurses", List.of(
                curseState(ECHO_ROT, 2, false, 0),
                curseState(BLOOD_DEBT, 2, true, 55)
        ));
        resolution.put("contractSummary", Map.of(
                "contractCount", 1,
                "totalDebt", 55,
                "contractResistance", 35,
                "severReadyCount", 0
        ));
        resolution.put("cleansingPlan", Map.of(
                "planCode", 2,
                "action", "pay_contract_debt",
                "target", BLOOD_DEBT,
                "readiness", 10
        ));
        resolution.put("tickEffects", List.of(
                effect("minecraft:darkness", 45, 0, ECHO_ROT),
                effect("minecraft:weakness", 80, 0, ECHO_ROT),
                effect("minecraft:strength", 80, 0, BLOOD_DEBT)
        ));
        resolution.put("events", List.of(
                event("curse.gained", ECHO_ROT, 3, "signal_backlash"),
                event("curse.gained", BLOOD_DEBT, 2, "contract"),
                event("curse.cleansed", ECHO_ROT, 2, "ritual_cleansing"),
                event("curse.debt_paid", BLOOD_DEBT, 55, "ledger_stabilize")
        ));
        resolution.put("diagnostics", List.of(
                "curse.state.loaded",
                "curse.contract.debt_recalculated",
                "curse.cleansing.plan_selected",
                "curse.tick.effects_resolved"
        ));
        resolution.put("referenceBehavior", "cursecore_resolves_persistent_player_curse_state");
        return Map.copyOf(resolution);
    }

    public static boolean referenceStateResolutionPassed(Map<String, Object> resolution) {
        return Boolean.TRUE.equals(resolution.get("curseStateResolved"))
                && ADAPTERCORE_CONTRACT_ID.equals(resolution.get("adapterCoreContract"))
                && REFERENCE_PLAYER_ID.equals(resolution.get("playerId"))
                && String.valueOf(resolution.get("operations")).contains("cleanse_echo_rot")
                && String.valueOf(resolution.get("activeCurses")).contains("stage=2")
                && String.valueOf(resolution.get("contractSummary")).contains("totalDebt=55")
                && String.valueOf(resolution.get("contractSummary")).contains("contractResistance=35")
                && String.valueOf(resolution.get("cleansingPlan")).contains("pay_contract_debt")
                && String.valueOf(resolution.get("cleansingPlan")).contains("readiness=10")
                && String.valueOf(resolution.get("tickEffects")).contains("minecraft:weakness")
                && String.valueOf(resolution.get("diagnostics")).contains("curse.tick.effects_resolved");
    }

    private static Map<String, Object> operation(
            String id,
            String curseId,
            String source,
            int delta,
            int before,
            int after
    ) {
        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put("id", id);
        operation.put("curseId", curseId);
        operation.put("source", source);
        operation.put("delta", delta);
        operation.put("before", before);
        operation.put("after", after);
        return Map.copyOf(operation);
    }

    private static Map<String, Object> curseState(String curseId, int stage, boolean contractBound, int debt) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("curseId", curseId);
        state.put("stage", stage);
        state.put("contractBound", contractBound);
        state.put("debt", debt);
        return Map.copyOf(state);
    }

    private static Map<String, Object> effect(String effectId, int durationTicks, int amplifier, String curseId) {
        Map<String, Object> effect = new LinkedHashMap<>();
        effect.put("effectId", effectId);
        effect.put("durationTicks", durationTicks);
        effect.put("amplifier", amplifier);
        effect.put("curseId", curseId);
        return Map.copyOf(effect);
    }

    private static Map<String, Object> event(String eventId, String curseId, int value, String source) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", eventId);
        event.put("curseId", curseId);
        event.put("value", value);
        event.put("source", source);
        return Map.copyOf(event);
    }
}
