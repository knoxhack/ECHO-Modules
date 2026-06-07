package com.knoxhack.echo.structurecore;

import java.util.Locale;

public record EchoStructureId(String value) {
    public EchoStructureId {
        value = StructureContractGuards.requireText(value, "structure id").toLowerCase(Locale.ROOT);
    }

    public static EchoStructureId of(String value) {
        return new EchoStructureId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
