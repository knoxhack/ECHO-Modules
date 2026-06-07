package com.knoxhack.echo.spawncore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.progressioncore.EchoProgressionId;
import com.knoxhack.echo.socialcore.EchoFactionId;

import java.util.Map;
import java.util.Set;

public record EchoSpawnCondition(
        EchoSpawnRuleKind kind,
        EchoContentReference biomeReference,
        EchoContentReference hazardReference,
        EchoContentReference poiReference,
        EchoContentReference structureReference,
        EchoContentReference weatherEventReference,
        EchoFactionId factionId,
        EchoProgressionId progressionId,
        Set<EchoFeatureId> requiredFeatures,
        EchoContentGate gate,
        Map<String, String> attributes
) {
    public EchoSpawnCondition {
        kind = kind == null ? EchoSpawnRuleKind.UNKNOWN : kind;
        requiredFeatures = SpawnContractGuards.immutableSet(requiredFeatures);
        gate = gate == null ? EchoContentGate.open() : gate;
        attributes = SpawnContractGuards.immutableMap(attributes);
    }
}
