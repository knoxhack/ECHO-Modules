package com.knoxhack.echo.codexcore;

import java.util.Map;
import java.util.Set;

public record EchoCodexSearchMetadata(
        Set<String> tags,
        Set<String> keywords,
        Set<String> aliases,
        int searchWeight,
        Map<String, String> attributes
) {
    public EchoCodexSearchMetadata {
        tags = CodexContractGuards.immutableSet(tags);
        keywords = CodexContractGuards.immutableSet(keywords);
        aliases = CodexContractGuards.immutableSet(aliases);
        searchWeight = CodexContractGuards.nonNegative(searchWeight, "codex search weight");
        attributes = CodexContractGuards.immutableMap(attributes);
    }
}
