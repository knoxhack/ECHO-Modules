package com.knoxhack.echo.adaptercore;

import java.util.List;
import java.util.Objects;

public record EchoLootDefinition(
        String id,
        String addon,
        List<String> entries,
        String source
) {
    public EchoLootDefinition {
        id = AdapterContractGuards.requireText(id, "loot definition id");
        addon = AdapterContractGuards.requireText(addon, "loot definition addon");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        source = AdapterContractGuards.requireText(source, "loot definition source");
    }
}
