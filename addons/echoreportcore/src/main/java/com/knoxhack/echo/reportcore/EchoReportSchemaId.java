package com.knoxhack.echo.reportcore;

public record EchoReportSchemaId(String value) {
    public EchoReportSchemaId {
        value = ReportContractGuards.normalizedId(value, "report schema id");
    }

    public static EchoReportSchemaId of(String value) {
        return new EchoReportSchemaId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
