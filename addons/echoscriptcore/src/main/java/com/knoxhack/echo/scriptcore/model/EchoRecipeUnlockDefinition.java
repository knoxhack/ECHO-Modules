package com.knoxhack.echo.scriptcore.model;

import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoRecipeUnlockDefinition(
        EchoScriptDefinition base,
        Optional<Identifier> recipe,
        List<EchoCondition> recipeUnlockConditions,
        List<EchoAction> recipeActions) implements DelegatingScriptDefinition {
    public EchoRecipeUnlockDefinition {
        recipe = recipe == null ? Optional.empty() : recipe;
        recipeUnlockConditions = List.copyOf(recipeUnlockConditions == null ? List.of() : recipeUnlockConditions);
        recipeActions = List.copyOf(recipeActions == null ? List.of() : recipeActions);
    }
}
