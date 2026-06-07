package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;
import java.util.Map;

public record EchoMetadataScanResult(
        String workspace,
        String addonSet,
        List<EchoMetadataParseResult> moduleMetadata,
        List<EchoMetadataParseResult> aiMetadata,
        Map<EchoModuleId, EchoMetadataStatus> moduleStatuses,
        Map<EchoModuleId, EchoMetadataStatus> aiStatuses,
        List<EchoMetadataIssue> issues
) {
    public EchoMetadataScanResult {
        workspace = MetadataContractGuards.optionalText(workspace);
        addonSet = MetadataContractGuards.optionalText(addonSet);
        moduleMetadata = MetadataContractGuards.immutableList(moduleMetadata);
        aiMetadata = MetadataContractGuards.immutableList(aiMetadata);
        moduleStatuses = MetadataContractGuards.immutableMap(moduleStatuses);
        aiStatuses = MetadataContractGuards.immutableMap(aiStatuses);
        issues = MetadataContractGuards.immutableList(issues);
    }
}
