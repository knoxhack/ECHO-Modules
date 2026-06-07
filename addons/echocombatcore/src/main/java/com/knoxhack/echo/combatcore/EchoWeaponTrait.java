package com.knoxhack.echo.combatcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoWeaponTrait(
        EchoWeaponTraitId id,
        String displayName,
        EchoContentReference weaponReference,
        EchoDamageTypeId damageTypeId,
        double damageMultiplier,
        double cooldownMultiplier,
        Set<EchoFeatureId> behaviorFeatures,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoWeaponTrait {
        Objects.requireNonNull(id, "id");
        displayName = CombatContractGuards.requireText(displayName, "weapon trait display name");
        damageMultiplier = CombatContractGuards.nonNegative(damageMultiplier, "weapon trait damage multiplier");
        cooldownMultiplier = CombatContractGuards.nonNegative(cooldownMultiplier, "weapon trait cooldown multiplier");
        behaviorFeatures = CombatContractGuards.immutableSet(behaviorFeatures);
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = CombatContractGuards.immutableList(diagnostics);
        attributes = CombatContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
