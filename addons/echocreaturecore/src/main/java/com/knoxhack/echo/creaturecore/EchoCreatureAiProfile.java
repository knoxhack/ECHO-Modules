package com.knoxhack.echo.creaturecore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.socialcore.EchoHostilityState;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoCreatureAiProfile(
        EchoCreatureAiProfileId id,
        EchoCreatureRole role,
        EchoHostilityState defaultHostility,
        Set<EchoFeatureId> behaviorFeatures,
        EchoContentReference dialogueReference,
        EchoContentReference combatBehaviorReference,
        EchoContentGate gate,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoCreatureAiProfile {
        Objects.requireNonNull(id, "id");
        role = role == null ? EchoCreatureRole.UNKNOWN : role;
        defaultHostility = defaultHostility == null ? EchoHostilityState.UNKNOWN : defaultHostility;
        behaviorFeatures = CreatureContractGuards.immutableSet(behaviorFeatures);
        gate = gate == null ? EchoContentGate.open() : gate;
        playerSummary = CreatureContractGuards.optionalText(playerSummary);
        developerDetails = CreatureContractGuards.optionalText(developerDetails);
        attributes = CreatureContractGuards.immutableMap(attributes);
    }
}
