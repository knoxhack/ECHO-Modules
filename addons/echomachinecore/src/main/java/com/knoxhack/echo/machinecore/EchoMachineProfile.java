package com.knoxhack.echo.machinecore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoMachineProfile(
        EchoMachineId id,
        EchoMachineKind kind,
        EchoMachineState defaultState,
        EchoModuleId ownerModule,
        EchoContentReference machineBlockReference,
        List<EchoMachineRecipeBinding> recipeBindings,
        List<EchoMachineUpgradeSlot> upgradeSlots,
        EchoMachineMaintenanceProfile maintenanceProfile,
        List<EchoMachineFailureState> failureStates,
        List<EchoMachineAutomationHook> automationHooks,
        EchoMachineIntegrationRefs integrationRefs,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoMachineProfile {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoMachineKind.UNKNOWN : kind;
        defaultState = defaultState == null ? EchoMachineState.UNKNOWN : defaultState;
        recipeBindings = MachineContractGuards.immutableList(recipeBindings);
        upgradeSlots = MachineContractGuards.immutableList(upgradeSlots);
        failureStates = MachineContractGuards.immutableList(failureStates);
        automationHooks = MachineContractGuards.immutableList(automationHooks);
        diagnostics = MachineContractGuards.immutableList(diagnostics);
        attributes = MachineContractGuards.immutableMap(attributes);
    }

    public boolean degradedByDefault() {
        return defaultState.degraded()
                || failureStates.stream().anyMatch(EchoMachineFailureState::blocking)
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
