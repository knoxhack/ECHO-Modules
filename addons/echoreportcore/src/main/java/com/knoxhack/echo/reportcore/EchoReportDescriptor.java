package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.schemacore.EchoSchemaId;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoReportDescriptor(
        EchoReportId id,
        EchoReportKind kind,
        String title,
        String summary,
        String outputPath,
        EchoReportFormat format,
        Set<EchoReportAudience> audiences,
        EchoSchemaId schemaId,
        Set<EchoFeatureId> relatedFeatures,
        boolean deterministic,
        boolean localOnly,
        boolean requiresMinecraftLaunch,
        boolean includesSecrets,
        EchoReportRedactionPolicy redactionPolicy,
        Set<String> relatedDocs,
        Map<String, String> attributes
) {
    public EchoReportDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        title = ReportContractGuards.requireText(title, "report title");
        summary = ReportContractGuards.optionalText(summary);
        outputPath = ReportContractGuards.normalizedPath(outputPath == null || outputPath.isBlank() ? kind.defaultOutputPath() : outputPath, "report output path");
        format = format == null ? EchoReportFormat.JSON : format;
        audiences = ReportContractGuards.immutableSet(audiences);
        relatedFeatures = ReportContractGuards.immutableSet(relatedFeatures);
        deterministic = true;
        localOnly = true;
        includesSecrets = false;
        redactionPolicy = redactionPolicy == null ? EchoReportRedactionPolicy.localDefault() : redactionPolicy;
        relatedDocs = ReportContractGuards.immutableSet(relatedDocs);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public static EchoReportDescriptor of(EchoReportKind kind, String title, Set<EchoReportAudience> audiences, EchoSchemaId schemaId) {
        return new EchoReportDescriptor(
                EchoReportId.of(kind.serializedName()),
                kind,
                title,
                "",
                kind.defaultOutputPath(),
                EchoReportFormat.JSON,
                audiences,
                schemaId,
                Set.of(),
                true,
                true,
                false,
                false,
                EchoReportRedactionPolicy.localDefault(),
                Set.of("docs/echo/tooling/ECHO_REPORT_FORMATS.md"),
                Map.of()
        );
    }
}
