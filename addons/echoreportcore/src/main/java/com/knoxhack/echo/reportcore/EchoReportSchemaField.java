package com.knoxhack.echo.reportcore;

import java.util.Objects;

public record EchoReportSchemaField(
        String path,
        EchoReportSchemaFieldType type,
        boolean required,
        boolean nonEmpty,
        String summary
) {
    public EchoReportSchemaField {
        path = ReportContractGuards.requireText(path, "report schema field path");
        Objects.requireNonNull(type, "type");
        summary = ReportContractGuards.optionalText(summary);
    }

    public static EchoReportSchemaField required(String path, EchoReportSchemaFieldType type, String summary) {
        return new EchoReportSchemaField(path, type, true, false, summary);
    }

    public static EchoReportSchemaField requiredNonEmpty(String path, EchoReportSchemaFieldType type, String summary) {
        return new EchoReportSchemaField(path, type, true, true, summary);
    }
}
