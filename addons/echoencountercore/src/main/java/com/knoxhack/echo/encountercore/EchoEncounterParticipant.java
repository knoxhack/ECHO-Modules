package com.knoxhack.echo.encountercore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoEncounterParticipant(
        String participantId,
        EchoEncounterParticipantRole role,
        EchoContentReference creatureReference,
        EchoContentReference factionReference,
        int minCount,
        int maxCount,
        double weight,
        Map<String, String> attributes
) {
    public EchoEncounterParticipant {
        participantId = EncounterContractGuards.id(participantId, "encounter participant id");
        role = role == null ? EchoEncounterParticipantRole.UNKNOWN : role;
        minCount = EncounterContractGuards.nonNegative(minCount, "min count");
        maxCount = EncounterContractGuards.nonNegative(maxCount, "max count");
        if (maxCount < minCount) {
            throw new IllegalArgumentException("max count must be greater than or equal to min count");
        }
        weight = EncounterContractGuards.nonNegative(weight, "participant weight");
        attributes = EncounterContractGuards.immutableMap(attributes);
    }
}
