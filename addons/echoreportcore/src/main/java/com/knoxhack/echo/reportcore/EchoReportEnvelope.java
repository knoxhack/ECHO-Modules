package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.platformcore.EchoPackId;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record EchoReportEnvelope(
        EchoReportSchemaId schema,
        Instant generatedAt,
        EchoReportGeneratorId generator,
        String workspace,
        String addonSet,
        EchoPackId packId,
        EchoReportStatus status,
        EchoReportSummary summary,
        List<EchoReportIssue> issues,
        Map<String, Object> data
) {
    public EchoReportEnvelope {
        if (schema == null) {
            throw new IllegalArgumentException("report schema must not be null");
        }
        generatedAt = generatedAt == null ? Instant.EPOCH : generatedAt;
        generator = generator == null ? EchoReportGeneratorId.of("unknown") : generator;
        workspace = ReportContractGuards.optionalText(workspace).isBlank() ? "ECHO" : workspace.trim();
        addonSet = ReportContractGuards.optionalText(addonSet);
        status = status == null ? EchoReportStatus.UNKNOWN : status;
        summary = summary == null ? EchoReportSummary.empty() : summary;
        issues = ReportContractGuards.immutableList(issues);
        data = ReportContractGuards.immutableMap(data);
    }

    public static EchoReportEnvelope of(
            EchoReportSchemaId schema,
            EchoReportContext context,
            EchoReportStatus status,
            EchoReportSummary summary,
            List<EchoReportIssue> issues,
            Map<String, Object> data
    ) {
        EchoReportContext safeContext = context == null
                ? new EchoReportContext("ECHO", "", null, Instant.EPOCH, EchoReportGeneratorId.of("unknown"), EchoReportRedactionPolicy.localDefault(), Map.of())
                : context;
        return new EchoReportEnvelope(
                schema,
                safeContext.generatedAt(),
                safeContext.generator(),
                safeContext.workspace(),
                safeContext.addonSet(),
                safeContext.packId(),
                status,
                summary,
                issues,
                data
        );
    }
}
