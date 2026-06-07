package com.knoxhack.echo.machinecore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.recipecore.EchoRecipeId;

import java.util.List;
import java.util.Map;

public record EchoMachineRecipeBinding(
        EchoRecipeId recipeId,
        EchoContentReference recipeReference,
        String recipeSlot,
        int minimumTier,
        List<String> requiredUpgrades,
        Map<String, String> attributes
) {
    public EchoMachineRecipeBinding {
        recipeSlot = MachineContractGuards.optionalText(recipeSlot);
        minimumTier = MachineContractGuards.nonNegative(minimumTier, "minimum tier");
        requiredUpgrades = MachineContractGuards.immutableList(requiredUpgrades);
        attributes = MachineContractGuards.immutableMap(attributes);
    }

    public boolean hasRecipe() {
        return recipeId != null || recipeReference != null;
    }
}
