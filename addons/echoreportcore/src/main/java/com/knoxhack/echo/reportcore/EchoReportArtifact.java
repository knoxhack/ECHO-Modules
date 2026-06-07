package com.knoxhack.echo.reportcore;

import java.util.Map;
import java.util.Objects;

public record EchoReportArtifact(
        EchoReportKind kind,
        String outputPath,
        EchoReportFormat format,
        EchoReportStatus status,
        String checksum,
        boolean deterministic,
        boolean secretsRedacted,
        Map<String, String> attributes
) {
    public EchoReportArtifact {
        Objects.requireNonNull(kind, "kind");
        outputPath = ReportContractGuards.normalizedPath(outputPath == null || outputPath.isBlank() ? kind.defaultOutputPath() : outputPath, "report artifact path");
        format = format == null ? EchoReportFormat.JSON : format;
        status = status == null ? EchoReportStatus.UNKNOWN : status;
        checksum = ReportContractGuards.optionalText(checksum);
        deterministic = true;
        secretsRedacted = true;
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public static EchoReportArtifact planned(EchoReportKind kind) {
        return new EchoReportArtifact(kind, kind.defaultOutputPath(), EchoReportFormat.JSON, EchoReportStatus.UNKNOWN, "", true, true, Map.of());
    }
}
