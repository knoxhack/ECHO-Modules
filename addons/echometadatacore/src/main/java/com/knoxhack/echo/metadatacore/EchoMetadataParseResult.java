package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoMetadataParseResult(
        EchoModuleId moduleId,
        EchoMetadataFileKind fileKind,
        EchoMetadataStatus status,
        String sourcePath,
        boolean fallbackUsed,
        EchoModuleManifest moduleManifest,
        EchoAiMetadata aiMetadata,
        Map<String, Object> rawFields,
        List<EchoMetadataIssue> issues
) {
    public EchoMetadataParseResult {
        Objects.requireNonNull(moduleId, "moduleId");
        fileKind = fileKind == null ? EchoMetadataFileKind.MODULE_MANIFEST : fileKind;
        status = status == null ? EchoMetadataStatus.UNKNOWN : status;
        sourcePath = MetadataContractGuards.optionalText(sourcePath);
        rawFields = MetadataContractGuards.immutableMap(rawFields);
        issues = MetadataContractGuards.immutableList(issues);
    }

    public boolean valid() {
        return status == EchoMetadataStatus.PRESENT || status == EchoMetadataStatus.FALLBACK;
    }
}
