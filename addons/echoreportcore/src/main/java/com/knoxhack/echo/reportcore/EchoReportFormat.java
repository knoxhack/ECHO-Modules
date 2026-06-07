package com.knoxhack.echo.reportcore;

public enum EchoReportFormat {
    JSON("json"),
    JSON_LINES("jsonl"),
    MARKDOWN("markdown"),
    TEXT("text");

    private final String serializedName;

    EchoReportFormat(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
