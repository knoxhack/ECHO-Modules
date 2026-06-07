package com.knoxhack.echo.combatcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoCombatRegistry(
        Map<EchoDamageTypeId, EchoDamageTypeProfile> damageTypes,
        Map<EchoArmorProfileId, EchoArmorProfile> armorProfiles,
        Map<EchoWeaponTraitId, EchoWeaponTrait> weaponTraits,
        List<EchoEnemyScalingProfile> enemyScalingProfiles,
        List<EchoBossPhase> bossPhases,
        List<EchoShieldProfile> shieldProfiles,
        List<EchoCombatTelemetryEvent> telemetryEvents,
        List<EchoHitFeedbackHook> hitFeedbackHooks,
        List<EchoDiagnostic> diagnostics
) {
    public EchoCombatRegistry {
        damageTypes = CombatContractGuards.immutableMap(damageTypes);
        armorProfiles = CombatContractGuards.immutableMap(armorProfiles);
        weaponTraits = CombatContractGuards.immutableMap(weaponTraits);
        enemyScalingProfiles = CombatContractGuards.immutableList(enemyScalingProfiles);
        bossPhases = CombatContractGuards.immutableList(bossPhases);
        shieldProfiles = CombatContractGuards.immutableList(shieldProfiles);
        telemetryEvents = CombatContractGuards.immutableList(telemetryEvents);
        hitFeedbackHooks = CombatContractGuards.immutableList(hitFeedbackHooks);
        diagnostics = CombatContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || damageTypes.values().stream().anyMatch(EchoDamageTypeProfile::blocking)
                || armorProfiles.values().stream().anyMatch(EchoArmorProfile::blocking)
                || weaponTraits.values().stream().anyMatch(EchoWeaponTrait::blocking)
                || shieldProfiles.stream().anyMatch(EchoShieldProfile::blocking);
    }
}
