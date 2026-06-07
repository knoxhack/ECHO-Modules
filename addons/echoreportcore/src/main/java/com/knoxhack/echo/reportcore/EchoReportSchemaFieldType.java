package com.knoxhack.echo.reportcore;

public enum EchoReportSchemaFieldType {
    OBJECT("object"),
    ARRAY("array"),
    STRING("string"),
    INTEGER("integer"),
    BOOLEAN("boolean"),
    NUMBER("number");

    private final String serializedName;

    EchoReportSchemaFieldType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
