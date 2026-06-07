package com.knoxhack.echo.combatcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.progressioncore.EchoProgressionId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoBossPhase(
        EchoBossPhaseId id,
        String displayName,
        EchoContentReference bossReference,
        double startsAtHealthRatio,
        EchoProgressionId unlockProgressionId,
        List<EchoContentReference> abilityReferences,
        List<EchoContentReference> statusReferences,
        EchoContentGate gate,
        String playerSummary,
        String developerDetails,
        Map<String, String> attributes
) {
    public EchoBossPhase {
        Objects.requireNonNull(id, "id");
        displayName = CombatContractGuards.requireText(displayName, "boss phase display name");
        startsAtHealthRatio = Math.max(0.0D, Math.min(1.0D, CombatContractGuards.finite(startsAtHealthRatio, "boss phase health ratio")));
        abilityReferences = CombatContractGuards.immutableList(abilityReferences);
        statusReferences = CombatContractGuards.immutableList(statusReferences);
        gate = gate == null ? EchoContentGate.open() : gate;
        playerSummary = CombatContractGuards.optionalText(playerSummary);
        developerDetails = CombatContractGuards.optionalText(developerDetails);
        attributes = CombatContractGuards.immutableMap(attributes);
    }
}
