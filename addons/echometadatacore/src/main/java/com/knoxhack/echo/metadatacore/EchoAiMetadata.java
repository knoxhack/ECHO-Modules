package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EchoAiMetadata(
        EchoMetadataSchemaRef schema,
        EchoModuleId module,
        String summary,
        List<EchoMetadataOwner> owners,
        List<String> importantPackages,
        List<String> mainClasses,
        List<EchoMetadataTask> commonTasks,
        List<String> doNotEdit,
        List<EchoMetadataProtectedFileRule> protectedFiles,
        List<String> safeEditZones,
        boolean requiresHumanReview,
        List<String> testCommands,
        List<String> buildCommands,
        Set<EchoMetadataAgentLane> recommendedAgentLanes,
        List<String> knownScreens,
        List<String> knownRegistries,
        List<EchoMetadataDiagnosticHint> commonDiagnostics,
        List<String> promptHints
) {
    public EchoAiMetadata {
        schema = schema == null ? EchoMetadataSchemaRef.aiMetadata(EchoMetadataConstants.SCHEMA_VERSION_1) : schema;
        Objects.requireNonNull(module, "module");
        summary = MetadataContractGuards.optionalText(summary);
        owners = MetadataContractGuards.immutableList(owners);
        importantPackages = MetadataContractGuards.immutableList(importantPackages);
        mainClasses = MetadataContractGuards.immutableList(mainClasses);
        commonTasks = MetadataContractGuards.immutableList(commonTasks);
        doNotEdit = MetadataContractGuards.immutableList(doNotEdit);
        protectedFiles = MetadataContractGuards.immutableList(protectedFiles);
        safeEditZones = MetadataContractGuards.immutableList(safeEditZones);
        testCommands = MetadataContractGuards.immutableList(testCommands);
        buildCommands = MetadataContractGuards.immutableList(buildCommands);
        recommendedAgentLanes = MetadataContractGuards.immutableSet(recommendedAgentLanes);
        knownScreens = MetadataContractGuards.immutableList(knownScreens);
        knownRegistries = MetadataContractGuards.immutableList(knownRegistries);
        commonDiagnostics = MetadataContractGuards.immutableList(commonDiagnostics);
        promptHints = MetadataContractGuards.immutableList(promptHints);
    }
}
