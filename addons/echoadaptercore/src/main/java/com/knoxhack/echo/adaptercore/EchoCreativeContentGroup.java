package com.knoxhack.echo.adaptercore;

import java.util.List;
import java.util.Objects;

public record EchoCreativeContentGroup(
        String id,
        String addon,
        List<String> entries,
        String source
) {
    public EchoCreativeContentGroup {
        id = AdapterContractGuards.requireText(id, "creative content group id");
        addon = AdapterContractGuards.requireText(addon, "creative content group addon");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        source = AdapterContractGuards.requireText(source, "creative content group source");
    }
}
