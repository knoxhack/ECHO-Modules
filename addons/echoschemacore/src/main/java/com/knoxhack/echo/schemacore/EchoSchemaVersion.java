package com.knoxhack.echo.schemacore;

public record EchoSchemaVersion(String value) implements Comparable<EchoSchemaVersion> {
    public EchoSchemaVersion {
        value = SchemaContractGuards.requireText(value, "schema version");
    }

    public static EchoSchemaVersion of(String value) {
        return new EchoSchemaVersion(value);
    }

    @Override
    public int compareTo(EchoSchemaVersion other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
