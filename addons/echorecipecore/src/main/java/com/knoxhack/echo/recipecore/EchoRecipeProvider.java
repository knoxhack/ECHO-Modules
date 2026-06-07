package com.knoxhack.echo.recipecore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Set;

public interface EchoRecipeProvider {
    String providerId();

    EchoModuleId moduleId();

    default Set<EchoRecipeType> providedTypes() {
        return Set.of();
    }

    default List<EchoRecipeCategory> categories() {
        return List.of();
    }

    EchoRecipeSearchResult search(EchoRecipeQuery query);

    default List<EchoMachineRecipeView> machineRecipes(EchoRecipeQuery query) {
        return List.of();
    }

    default List<EchoDiagnostic> diagnostics() {
        return List.of();
    }
}
