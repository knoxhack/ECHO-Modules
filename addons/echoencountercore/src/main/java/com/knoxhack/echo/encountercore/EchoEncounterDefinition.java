package com.knoxhack.echo.encountercore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoEncounterDefinition(
        EchoEncounterId id,
        String displayName,
        EchoEncounterType type,
        EchoModuleId owningModule,
        EchoContentReference worldEventReference,
        EchoContentReference regionReference,
        EchoContentReference poiReference,
        EchoContentReference factionReference,
        EchoContentReference bossGateReference,
        EchoContentReference lootProfileReference,
        List<EchoEncounterParticipant> participants,
        List<EchoEncounterReward> rewards,
        List<EchoEncounterConstraint> constraints,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoEncounterDefinition {
        Objects.requireNonNull(id, "id");
        displayName = EncounterContractGuards.requireText(displayName, "encounter display name");
        type = type == null ? EchoEncounterType.UNKNOWN : type;
        participants = EncounterContractGuards.immutableList(participants);
        rewards = EncounterContractGuards.immutableList(rewards);
        constraints = EncounterContractGuards.immutableList(constraints);
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = EncounterContractGuards.immutableList(diagnostics);
        attributes = EncounterContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return gate.blocksWhenMissing()
                || constraints.stream().anyMatch(EchoEncounterConstraint::blocking)
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
