package com.knoxhack.echo.difficultycore;

import com.knoxhack.echo.packcore.EchoPackVariantId;

import java.util.Map;

public record EchoPackVariantDifficultyPolicy(
        EchoPackVariantId variantId,
        EchoDifficultyProfileId defaultProfile,
        EchoDifficultyProfileId minimumProfile,
        EchoDifficultyProfileId maximumProfile,
        boolean allowAdaptiveDifficulty,
        boolean allowPlayerOverride,
        Map<String, String> attributes
) {
    public EchoPackVariantDifficultyPolicy {
        attributes = DifficultyContractGuards.immutableMap(attributes);
    }
}
