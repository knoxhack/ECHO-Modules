package com.knoxhack.echo.agentcore;

public enum EchoAiCommandOutputParser {
    RAW_TEXT("raw_text"),
    GRADLE_BUILD("gradle_build"),
    TEST_REPORT("test_report"),
    JSON_REPORT("json_report"),
    DIAGNOSTIC_REPORT("diagnostic_report"),
    NONE("none");

    private final String serializedName;

    EchoAiCommandOutputParser(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
