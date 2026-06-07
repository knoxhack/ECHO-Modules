package com.knoxhack.echomultiblockcore.runtime;

import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockState;
import com.knoxhack.echomultiblockcore.api.ValidationResult;

public final class MultiblockIntegrityService {
    private MultiblockIntegrityService() {
    }

    public static float integrityFromValidation(ValidationResult result) {
        return Math.max(0.0F, Math.min(100.0F, (float) (result.completion() * 100.0D)));
    }

    public static MultiblockState stateFor(MultiblockDefinition definition, float integrity, boolean structurallyValid) {
        if (!structurallyValid) {
            return integrity <= offlineThreshold(definition) ? MultiblockState.OFFLINE : MultiblockState.DAMAGED;
        }
        return integrity < damagedThreshold(definition) ? MultiblockState.DAMAGED : MultiblockState.FORMED;
    }

    public static int damagedThreshold(MultiblockDefinition definition) {
        return definition == null ? 70 : definition.integrityRules().damagedThreshold();
    }

    public static int offlineThreshold(MultiblockDefinition definition) {
        return definition == null ? 25 : definition.integrityRules().offlineThreshold();
    }
}
