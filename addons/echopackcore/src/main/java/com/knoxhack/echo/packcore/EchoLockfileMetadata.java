package com.knoxhack.echo.packcore;

import java.time.Instant;
import java.util.Map;

public record EchoLockfileMetadata(
        EchoPackLockfileId id,
        String generator,
        Instant generatedAt,
        boolean deterministic,
        boolean localOnly,
        boolean requiresMinecraftLaunch,
        Map<String, String> attributes
) {
    public EchoLockfileMetadata {
        id = id == null ? EchoPackLockfileId.of("default") : id;
        generator = PackContractGuards.optionalText(generator);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        attributes = PackContractGuards.immutableStringMap(attributes);
    }
}
