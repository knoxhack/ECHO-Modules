package com.knoxhack.echo.packcore;

import java.util.Objects;

public record EchoLockedResource(
        String path,
        EchoLockfileChecksum checksum,
        boolean required,
        EchoLockfileSource source,
        String summary
) {
    public EchoLockedResource {
        path = PackContractGuards.requireText(path, "locked resource path");
        Objects.requireNonNull(checksum, "checksum");
        source = source == null ? EchoLockfileSource.UNKNOWN : source;
        summary = PackContractGuards.optionalText(summary);
    }
}
