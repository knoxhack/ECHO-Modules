package com.knoxhack.echo.adaptercore;

import java.util.List;
import java.util.Objects;

public record EchoStructureDefinition(
        String id,
        String addon,
        String kind,
        String source,
        List<String> references
) {
    public EchoStructureDefinition {
        id = AdapterContractGuards.requireText(id, "structure definition id");
        addon = AdapterContractGuards.requireText(addon, "structure definition addon");
        kind = AdapterContractGuards.requireText(kind, "structure definition kind");
        source = AdapterContractGuards.requireText(source, "structure definition source");
        references = List.copyOf(Objects.requireNonNull(references, "references"));
    }
}
