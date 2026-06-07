package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.platformcore.EchoPackId;

import java.time.Instant;
import java.util.Map;

public record EchoReportContext(
        String workspace,
        String addonSet,
        EchoPackId packId,
        Instant generatedAt,
        EchoReportGeneratorId generator,
        EchoReportRedactionPolicy redactionPolicy,
        Map<String, String> attributes
) {
    public EchoReportContext {
        workspace = ReportContractGuards.optionalText(workspace).isBlank() ? "ECHO" : workspace.trim();
        addonSet = ReportContractGuards.optionalText(addonSet);
        generatedAt = generatedAt == null ? Instant.EPOCH : generatedAt;
        generator = generator == null ? EchoReportGeneratorId.of("unknown") : generator;
        redactionPolicy = redactionPolicy == null ? EchoReportRedactionPolicy.localDefault() : redactionPolicy;
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public static EchoReportContext local(String addonSet, EchoPackId packId, Instant generatedAt, EchoReportGeneratorId generator) {
        return new EchoReportContext("ECHO", addonSet, packId, generatedAt, generator, EchoReportRedactionPolicy.localDefault(), Map.of());
    }
}
