package com.knoxhack.echo.combatcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoArmorProfile(
        EchoArmorProfileId id,
        String displayName,
        EchoContentReference armorContent,
        double baseReduction,
        Map<EchoCombatDamageKind, Double> damageKindReduction,
        List<EchoContentReference> statusResistances,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoArmorProfile {
        Objects.requireNonNull(id, "id");
        displayName = CombatContractGuards.requireText(displayName, "armor profile display name");
        baseReduction = CombatContractGuards.nonNegative(baseReduction, "armor base reduction");
        damageKindReduction = CombatContractGuards.immutableMap(damageKindReduction);
        statusResistances = CombatContractGuards.immutableList(statusResistances);
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = CombatContractGuards.immutableList(diagnostics);
        attributes = CombatContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
