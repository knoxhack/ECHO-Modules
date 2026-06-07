package com.knoxhack.echo.reportcore;

public record EchoReportGeneratorId(String value) {
    public EchoReportGeneratorId {
        value = ReportContractGuards.normalizedId(value, "report generator id");
    }

    public static EchoReportGeneratorId of(String value) {
        return new EchoReportGeneratorId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
