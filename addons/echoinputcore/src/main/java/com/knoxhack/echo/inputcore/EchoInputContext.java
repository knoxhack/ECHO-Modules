package com.knoxhack.echo.inputcore;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoInputContext(
        EchoInputContextId id,
        String label,
        int priority,
        boolean controllerReady,
        List<EchoInputBinding> bindings,
        Map<String, String> attributes
) {
    public EchoInputContext {
        Objects.requireNonNull(id, "id");
        label = InputContractGuards.optionalText(label);
        priority = InputContractGuards.nonNegative(priority, "input context priority");
        bindings = InputContractGuards.immutableList(bindings);
        attributes = InputContractGuards.immutableMap(attributes);
    }
}
