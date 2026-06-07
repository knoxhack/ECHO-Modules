package com.knoxhack.echo.packcore;

public record EchoLockedConfig(
        String path,
        EchoLockfileChecksum checksum,
        boolean required,
        boolean userEditable,
        EchoLockfileSource source,
        String summary
) {
    public EchoLockedConfig {
        path = PackContractGuards.requireText(path, "locked config path");
        checksum = checksum == null ? new EchoLockfileChecksum("", EchoLockfileChecksumMode.UNKNOWN, "", java.util.List.of()) : checksum;
        source = source == null ? EchoLockfileSource.UNKNOWN : source;
        summary = PackContractGuards.optionalText(summary);
    }
}
