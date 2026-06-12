package com.knoxhack.echoopenlandsprotocol.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OpenlandsWaystoneRuntime {
    public static final int ACTIVE_STONES_REQUIRED_FOR_FAST_TRAVEL = 2;

    private OpenlandsWaystoneRuntime() {
    }

    public static OpenlandsWaystoneTransition advance(OpenlandsWaystoneState current, Map<String, Integer> availableInputs) {
        if (current == null) {
            return OpenlandsWaystoneTransition.rejected(OpenlandsWaystoneState.UNDISCOVERED, "missing_current_state");
        }
        OpenlandsWaystoneState next = current.next();
        if (next == null) {
            return OpenlandsWaystoneTransition.rejected(current, "already_active");
        }

        Map<String, Integer> consumed = requiredInputsForTransition(current, availableInputs == null ? Map.of() : availableInputs);
        if (consumed == null) {
            return OpenlandsWaystoneTransition.rejected(current, "missing_required_inputs");
        }
        return OpenlandsWaystoneTransition.accepted(current, next, consumed);
    }

    public static boolean fastTravelUnlocked(int activeStoneCount) {
        return activeStoneCount >= ACTIVE_STONES_REQUIRED_FOR_FAST_TRAVEL;
    }

    public static Map<String, Map<String, Integer>> requiredInputsByState() {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        for (OpenlandsWaystoneState state : OpenlandsWaystoneState.values()) {
            result.put(state.id(), canonicalRequiredInputs(state));
        }
        return Map.copyOf(result);
    }

    private static Map<String, Integer> requiredInputsForTransition(OpenlandsWaystoneState current, Map<String, Integer> availableInputs) {
        return switch (current) {
            case UNDISCOVERED, DISCOVERED -> Map.of();
            case DEBRIS_CLEARED -> chooseStoneRepairInput(availableInputs);
            case STONE_REPAIRED -> require(availableInputs, Map.of("repair_kit", 1));
            case FITTED -> require(availableInputs, Map.of("copper_fitting", 4));
            case CHARGED -> require(availableInputs, Map.of("waystone_core", 1, "glow_crystal", 1));
            case BOUND -> require(availableInputs, Map.of("route_binding", 1));
            case ACTIVE -> null;
        };
    }

    private static Map<String, Integer> canonicalRequiredInputs(OpenlandsWaystoneState current) {
        return switch (current) {
            case UNDISCOVERED, DISCOVERED, ACTIVE -> Map.of();
            case DEBRIS_CLEARED -> Map.of("fieldstone_piece", 8);
            case STONE_REPAIRED -> Map.of("repair_kit", 1);
            case FITTED -> Map.of("copper_fitting", 4);
            case CHARGED -> Map.of("waystone_core", 1, "glow_crystal", 1);
            case BOUND -> Map.of("route_binding", 1);
        };
    }

    private static Map<String, Integer> chooseStoneRepairInput(Map<String, Integer> availableInputs) {
        Map<String, Integer> fieldstone = require(availableInputs, Map.of("fieldstone_piece", 8));
        if (fieldstone != null) return fieldstone;
        return require(availableInputs, Map.of("limestone", 1));
    }

    private static Map<String, Integer> require(Map<String, Integer> availableInputs, Map<String, Integer> requiredInputs) {
        for (Map.Entry<String, Integer> required : requiredInputs.entrySet()) {
            if (availableInputs.getOrDefault(required.getKey(), 0) < required.getValue()) return null;
        }
        return requiredInputs;
    }
}
