package com.knoxhack.echo.combatcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoShieldProfile(
        String shieldId,
        EchoContentReference shieldContent,
        double capacity,
        double rechargePerSecond,
        double breakCooldownSeconds,
        List<EchoCombatDamageKind> vulnerableKinds,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoShieldProfile {
        shieldId = CombatContractGuards.requireText(shieldId, "shield id");
        capacity = CombatContractGuards.nonNegative(capacity, "shield capacity");
        rechargePerSecond = CombatContractGuards.nonNegative(rechargePerSecond, "shield recharge per second");
        breakCooldownSeconds = CombatContractGuards.nonNegative(breakCooldownSeconds, "shield break cooldown seconds");
        vulnerableKinds = CombatContractGuards.immutableList(vulnerableKinds);
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = CombatContractGuards.immutableList(diagnostics);
        attributes = CombatContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
