package com.knoxhack.echo.adaptercore;

import java.util.List;
import java.util.Objects;

public record EchoTagDefinition(
        String id,
        String addon,
        String kind,
        List<String> values,
        String source,
        int mergedSourceCount
) {
    public EchoTagDefinition(String id, String addon, String kind, List<String> values, String source) {
        this(id, addon, kind, values, source, 1);
    }

    public EchoTagDefinition {
        id = AdapterContractGuards.requireText(id, "tag definition id");
        addon = AdapterContractGuards.requireText(addon, "tag definition addon");
        kind = AdapterContractGuards.requireText(kind, "tag definition kind");
        values = List.copyOf(Objects.requireNonNull(values, "values"));
        source = AdapterContractGuards.requireText(source, "tag definition source");
        if (mergedSourceCount < 1) {
            throw new IllegalArgumentException("tag definition merged source count must be positive");
        }
    }
}
