package com.knoxhack.echo.packcore;

public record EchoJarFingerprint(
        String fileName,
        long sizeBytes,
        String sha256,
        String checksumMode,
        String checksumStatus
) {
    public EchoJarFingerprint {
        fileName = PackContractGuards.requireText(fileName, "jar file name");
        sizeBytes = Math.max(-1L, sizeBytes);
        sha256 = PackContractGuards.optionalText(sha256);
        checksumMode = PackContractGuards.optionalText(checksumMode);
        checksumStatus = PackContractGuards.optionalText(checksumStatus);
    }
}
