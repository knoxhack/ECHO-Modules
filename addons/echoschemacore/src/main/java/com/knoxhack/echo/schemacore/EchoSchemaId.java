package com.knoxhack.echo.schemacore;

import java.util.Locale;

public record EchoSchemaId(String value) {
    public EchoSchemaId {
        value = SchemaContractGuards.requireText(value, "schema id").toLowerCase(Locale.ROOT);
    }

    public static EchoSchemaId of(String value) {
        return new EchoSchemaId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
