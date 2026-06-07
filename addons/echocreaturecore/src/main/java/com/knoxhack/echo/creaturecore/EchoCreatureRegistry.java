package com.knoxhack.echo.creaturecore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoCreatureRegistry(
        Map<EchoCreatureArchetypeId, EchoCreatureArchetype> archetypes,
        Map<EchoCreatureAiProfileId, EchoCreatureAiProfile> aiProfiles,
        List<EchoDiagnostic> diagnostics
) {
    public EchoCreatureRegistry {
        archetypes = CreatureContractGuards.immutableMap(archetypes);
        aiProfiles = CreatureContractGuards.immutableMap(aiProfiles);
        diagnostics = CreatureContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || archetypes.values().stream().anyMatch(EchoCreatureArchetype::blocking);
    }
}
