package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record EchoVillagerReplacementPlan(
        String planId,
        EchoVillagerReplacementMode mode,
        boolean enabledByDefault,
        String configKey,
        EchoFeatureId requiredFeature,
        EchoContentGate gate,
        List<EchoNpcProfileId> replacementProfiles,
        Set<String> vanillaProfessions,
        List<EchoDiagnostic> diagnostics,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoVillagerReplacementPlan {
        planId = SocialContractGuards.requireText(planId, "villager replacement plan id");
        mode = mode == null ? EchoVillagerReplacementMode.DISABLED : mode;
        configKey = SocialContractGuards.optionalText(configKey);
        gate = gate == null ? EchoContentGate.open() : gate;
        if (mode.canReplaceVillagers() && configKey.isEmpty() && requiredFeature == null && !gate.gated()) {
            throw new IllegalArgumentException("non-disabled villager replacement plans must declare a config key, feature, or gate");
        }
        if (enabledByDefault && configKey.isEmpty()) {
            throw new IllegalArgumentException("enabled villager replacement plans must declare an explicit config key");
        }
        replacementProfiles = SocialContractGuards.immutableList(replacementProfiles);
        vanillaProfessions = SocialContractGuards.immutableSet(vanillaProfessions);
        diagnostics = SocialContractGuards.immutableList(diagnostics);
        playerSummary = SocialContractGuards.optionalText(playerSummary);
        developerDetails = SocialContractGuards.optionalText(developerDetails);
        attributes = SocialContractGuards.immutableMap(attributes);
    }

    public boolean replacementAllowedByContract() {
        return mode.canReplaceVillagers() && (requiredFeature != null || gate.gated() || !configKey.isEmpty());
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
