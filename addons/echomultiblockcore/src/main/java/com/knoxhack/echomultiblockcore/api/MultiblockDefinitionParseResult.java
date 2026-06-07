package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record MultiblockDefinitionParseResult(
        Identifier resourceId,
        Identifier definitionId,
        MultiblockDefinition definition,
        List<String> warnings,
        List<String> errors) {
    public MultiblockDefinitionParseResult {
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        errors = List.copyOf(errors == null ? List.of() : errors);
        definitionId = definitionId == null && definition != null ? definition.id() : definitionId;
    }

    public boolean valid() {
        return definition != null && errors.isEmpty();
    }
}
