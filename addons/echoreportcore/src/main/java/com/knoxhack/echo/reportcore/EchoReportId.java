package com.knoxhack.echo.reportcore;

public record EchoReportId(String value) {
    public EchoReportId {
        value = ReportContractGuards.normalizedId(value, "report id");
    }

    public static EchoReportId of(String value) {
        return new EchoReportId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
