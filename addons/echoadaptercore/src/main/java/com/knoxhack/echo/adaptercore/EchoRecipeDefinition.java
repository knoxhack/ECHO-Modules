package com.knoxhack.echo.adaptercore;

import java.util.List;
import java.util.Objects;

public record EchoRecipeDefinition(
        String id,
        String addon,
        String type,
        List<String> inputs,
        List<String> outputs,
        String source
) {
    public EchoRecipeDefinition {
        id = AdapterContractGuards.requireText(id, "recipe definition id");
        addon = AdapterContractGuards.requireText(addon, "recipe definition addon");
        type = AdapterContractGuards.requireText(type, "recipe definition type");
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
        outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
        source = AdapterContractGuards.requireText(source, "recipe definition source");
    }
}
