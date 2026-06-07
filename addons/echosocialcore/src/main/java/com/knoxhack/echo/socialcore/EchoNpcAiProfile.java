package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoNpcAiProfile(
        EchoNpcAiProfileId id,
        EchoNpcRole role,
        EchoHostilityState defaultHostility,
        Set<EchoFeatureId> behaviorFeatures,
        EchoContentGate gate,
        String behaviorSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoNpcAiProfile {
        Objects.requireNonNull(id, "id");
        role = role == null ? EchoNpcRole.UNKNOWN : role;
        defaultHostility = defaultHostility == null ? EchoHostilityState.NEUTRAL : defaultHostility;
        behaviorFeatures = SocialContractGuards.immutableSet(behaviorFeatures);
        gate = gate == null ? EchoContentGate.open() : gate;
        behaviorSummary = SocialContractGuards.optionalText(behaviorSummary);
        developerDetails = SocialContractGuards.optionalText(developerDetails);
        attributes = SocialContractGuards.immutableMap(attributes);
    }
}
