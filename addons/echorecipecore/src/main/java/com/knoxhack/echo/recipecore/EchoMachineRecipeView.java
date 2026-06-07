package com.knoxhack.echo.recipecore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoMachineRecipeView(
        String viewId,
        EchoRecipeId recipeId,
        EchoContentId machineContent,
        EchoRecipeType type,
        String machineName,
        int durationTicks,
        int energyCost,
        List<EchoRecipeIngredient> inputs,
        List<EchoRecipeOutput> outputs,
        Set<String> requiredTools,
        EchoContentGate gate,
        String playerSummary,
        String developerDetails,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoMachineRecipeView {
        viewId = RecipeContractGuards.requireText(viewId, "machine recipe view id");
        Objects.requireNonNull(recipeId, "recipeId");
        Objects.requireNonNull(machineContent, "machineContent");
        type = type == null ? EchoRecipeType.MACHINE : type;
        machineName = RecipeContractGuards.optionalText(machineName);
        durationTicks = RecipeContractGuards.nonNegative(durationTicks, "machine recipe duration");
        energyCost = RecipeContractGuards.nonNegative(energyCost, "machine recipe energy cost");
        inputs = RecipeContractGuards.immutableList(inputs);
        outputs = RecipeContractGuards.immutableList(outputs);
        requiredTools = RecipeContractGuards.immutableSet(requiredTools);
        gate = gate == null ? EchoContentGate.open() : gate;
        playerSummary = RecipeContractGuards.optionalText(playerSummary);
        developerDetails = RecipeContractGuards.optionalText(developerDetails);
        diagnostics = RecipeContractGuards.immutableList(diagnostics);
        attributes = RecipeContractGuards.immutableMap(attributes);
    }

    public boolean blocked() {
        return gate.blocksWhenMissing()
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || inputs.stream().anyMatch(EchoRecipeIngredient::blockingWhenUnavailable);
    }
}
