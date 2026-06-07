package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.schemacore.EchoSchemaId;

import java.util.List;
import java.util.Objects;

public record EchoReportSchemaDescriptor(
        EchoReportKind kind,
        EchoSchemaId schemaId,
        String schemaVersion,
        String docsPath,
        List<String> commonFields,
        List<EchoReportSchemaField> reportFields,
        boolean deterministic,
        boolean localOnly,
        boolean requiresMinecraftLaunch,
        boolean includesSecrets
) {
    public EchoReportSchemaDescriptor {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(schemaId, "schemaId");
        schemaVersion = ReportContractGuards.requireText(schemaVersion, "report schema version");
        docsPath = ReportContractGuards.normalizedPath(docsPath, "report schema docs path");
        commonFields = ReportContractGuards.immutableList(commonFields);
        reportFields = ReportContractGuards.immutableList(reportFields);
        deterministic = true;
        localOnly = true;
        requiresMinecraftLaunch = false;
        includesSecrets = false;
    }
}
