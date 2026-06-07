package com.knoxhack.echo.combatcore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoDamageTypeProfile(
        EchoDamageTypeId id,
        EchoCombatDamageKind kind,
        String displayName,
        EchoModuleId owningModule,
        EchoContentReference statusReference,
        EchoContentReference iconReference,
        EchoContentReference soundProfileReference,
        EchoContentReference particleProfileReference,
        boolean bypassesArmor,
        boolean scalesWithDifficulty,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoDamageTypeProfile {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoCombatDamageKind.UNKNOWN : kind;
        displayName = CombatContractGuards.requireText(displayName, "damage type display name");
        diagnostics = CombatContractGuards.immutableList(diagnostics);
        attributes = CombatContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
