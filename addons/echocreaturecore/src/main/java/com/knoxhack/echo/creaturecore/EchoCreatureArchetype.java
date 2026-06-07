package com.knoxhack.echo.creaturecore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.socialcore.EchoFactionId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoCreatureArchetype(
        EchoCreatureArchetypeId id,
        String displayName,
        EchoCreatureRole role,
        EchoModuleId owningModule,
        EchoFactionId factionId,
        EchoContentReference entityReference,
        EchoCreatureAiProfileId aiProfileId,
        List<EchoCreatureSpawnTag> spawnTags,
        EchoContentReference renderProfileReference,
        EchoContentReference soundProfileReference,
        EchoContentReference lootProfileReference,
        EchoCreatureScanMetadata scanMetadata,
        List<EchoParticleAnchor> particleAnchors,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoCreatureArchetype {
        Objects.requireNonNull(id, "id");
        displayName = CreatureContractGuards.requireText(displayName, "creature archetype display name");
        role = role == null ? EchoCreatureRole.UNKNOWN : role;
        spawnTags = CreatureContractGuards.immutableList(spawnTags);
        particleAnchors = CreatureContractGuards.immutableList(particleAnchors);
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = CreatureContractGuards.immutableList(diagnostics);
        attributes = CreatureContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
