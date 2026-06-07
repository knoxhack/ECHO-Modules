package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record MultiblockProgressionParseResult(
        Identifier resourceId,
        Identifier progressionId,
        MultiblockProgressionDefinition definition,
        List<String> warnings,
        List<String> errors) {
    public MultiblockProgressionParseResult {
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        errors = List.copyOf(errors == null ? List.of() : errors);
    }

    public boolean valid() {
        return definition != null && errors.isEmpty();
    }
}
