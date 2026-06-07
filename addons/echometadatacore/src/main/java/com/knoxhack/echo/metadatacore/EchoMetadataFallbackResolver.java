package com.knoxhack.echo.metadatacore;

public final class EchoMetadataFallbackResolver {
    private EchoMetadataFallbackResolver() {
    }

    public static EchoMetadataStatus resolve(EchoMetadataStatus canonicalStatus, boolean legacyMetadataPresent) {
        if (canonicalStatus == EchoMetadataStatus.MISSING && legacyMetadataPresent) {
            return EchoMetadataStatus.FALLBACK;
        }
        return canonicalStatus == null ? EchoMetadataStatus.UNKNOWN : canonicalStatus;
    }

    public static boolean fallbackUsed(EchoMetadataStatus status) {
        return status == EchoMetadataStatus.FALLBACK;
    }
}
