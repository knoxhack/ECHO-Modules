package com.knoxhack.echo.spawncore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.socialcore.EchoFactionId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoSpawnProfile(
        EchoSpawnProfileId id,
        String displayName,
        EchoModuleId owningModule,
        EchoSpawnDensityPolicy densityPolicy,
        EchoFactionId factionId,
        EchoContentReference regionReference,
        EchoContentReference poiReference,
        EchoContentReference weatherEventReference,
        EchoContentReference hazardReference,
        List<EchoSpawnRule> rules,
        EchoContentGate gate,
        String developerDetails,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoSpawnProfile {
        Objects.requireNonNull(id, "id");
        displayName = SpawnContractGuards.requireText(displayName, "spawn profile display name");
        densityPolicy = densityPolicy == null ? EchoSpawnDensityPolicy.UNKNOWN : densityPolicy;
        rules = SpawnContractGuards.immutableList(rules);
        gate = gate == null ? EchoContentGate.open() : gate;
        developerDetails = SpawnContractGuards.optionalText(developerDetails);
        diagnostics = SpawnContractGuards.immutableList(diagnostics);
        attributes = SpawnContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || rules.stream().anyMatch(EchoSpawnRule::blocking);
    }
}
