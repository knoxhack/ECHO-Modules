package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.contentcore.EchoContentGate;

import java.util.Map;
import java.util.Objects;

public record EchoFactionAlliance(
        String allianceId,
        EchoFactionId sourceFaction,
        EchoFactionId targetFaction,
        EchoAllianceState allianceState,
        EchoHostilityState hostilityState,
        int minimumReputation,
        EchoContentGate gate,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoFactionAlliance {
        allianceId = SocialContractGuards.requireText(allianceId, "faction alliance id");
        Objects.requireNonNull(sourceFaction, "sourceFaction");
        Objects.requireNonNull(targetFaction, "targetFaction");
        allianceState = allianceState == null ? EchoAllianceState.UNKNOWN : allianceState;
        hostilityState = hostilityState == null ? EchoHostilityState.UNKNOWN : hostilityState;
        minimumReputation = SocialContractGuards.boundedReputation(minimumReputation);
        gate = gate == null ? EchoContentGate.open() : gate;
        playerSummary = SocialContractGuards.optionalText(playerSummary);
        developerDetails = SocialContractGuards.optionalText(developerDetails);
        attributes = SocialContractGuards.immutableMap(attributes);
    }

    public boolean conflict() {
        return allianceState.conflict() || hostilityState.hostile();
    }
}
