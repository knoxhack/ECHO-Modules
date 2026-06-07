package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record MultiblockAutomationRecipeParseResult(
        Identifier resourceId,
        Identifier recipeId,
        MultiblockAutomationRecipe recipe,
        List<String> warnings,
        List<String> errors) {
    public MultiblockAutomationRecipeParseResult {
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        errors = List.copyOf(errors == null ? List.of() : errors);
    }

    public boolean valid() {
        return recipe != null && errors.isEmpty();
    }
}
