package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.packcore.EchoPackProfileIssue;
import com.knoxhack.echo.packcore.EchoPackProfileParseResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record EchoPackProfileReport(
        EchoReportDescriptor descriptor,
        EchoReportContext context,
        EchoPackProfileParseResult parseResult,
        Map<String, String> attributes
) {
    public EchoPackProfileReport {
        descriptor = descriptor == null ? EchoReportConstants.descriptor(EchoReportKind.PACK_PROFILE) : descriptor;
        if (parseResult == null) {
            throw new IllegalArgumentException("pack profile parse result must not be null");
        }
        context = context == null ? EchoReportContext.local("", parseResult.requestedPackId(), null, EchoReportGeneratorId.of("echopackcore")) : context;
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public EchoReportEnvelope envelope() {
        return EchoReportEnvelope.of(
                EchoReportSchemaId.of(descriptor.schemaId().toString()),
                context,
                status(),
                summary(),
                issues(),
                data()
        );
    }

    private EchoReportStatus status() {
        if (parseResult.status().name().contains("INVALID")) {
            return EchoReportStatus.FAILED;
        }
        if (parseResult.status().name().equals("FALLBACK") || parseResult.status().name().equals("MISSING")) {
            return EchoReportStatus.DEGRADED;
        }
        return parseResult.issues().isEmpty() ? EchoReportStatus.PASS : EchoReportStatus.PASS_WITH_WARNINGS;
    }

    private EchoReportSummary summary() {
        int warnings = 0;
        int errors = 0;
        int notices = 0;
        int fatals = 0;
        for (EchoPackProfileIssue issue : parseResult.issues()) {
            switch (issue.severity()) {
                case WARNING -> warnings++;
                case ERROR -> errors++;
                case FATAL -> fatals++;
                case NOTICE -> notices++;
                default -> {
                }
            }
        }
        return new EchoReportSummary(warnings, errors, notices, fatals, Map.of(
                "issueCount", Integer.toString(parseResult.issues().size()),
                "profileStatus", parseResult.status().serializedName()
        ));
    }

    private List<EchoReportIssue> issues() {
        return parseResult.issues().stream()
                .map(issue -> new EchoReportIssue(
                        issue.code(),
                        EchoReportIssueSeverity.valueOf(issue.severity().name()),
                        issue.summary(),
                        issue.likelyFiles(),
                        issue.suggestedFix(),
                        issue.attributes()
                ))
                .toList();
    }

    private Map<String, Object> data() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profile", parseResult.normalizedProfile());
        data.put("validation", Map.of(
                "status", parseResult.status().serializedName(),
                "issueCount", parseResult.issues().size(),
                "source", parseResult.source().reportPath()
        ));
        data.put("diagnostics", issues().stream().map(this::issueToMap).toList());
        data.put("relatedDocs", List.of("docs/echo/packos/ECHO_PACK_PROFILES.md"));
        data.put("attributes", attributes);
        return data;
    }

    private Map<String, Object> issueToMap(EchoReportIssue issue) {
        return Map.of(
                "code", issue.code(),
                "severity", issue.severity().serializedName(),
                "summary", issue.summary(),
                "likelyFiles", issue.likelyFiles(),
                "suggestedFix", issue.suggestedFix()
        );
    }
}
