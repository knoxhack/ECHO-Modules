package com.knoxhack.echo.structurecore;

import java.util.Locale;

public record EchoPoiId(String value) {
    public EchoPoiId {
        value = StructureContractGuards.requireText(value, "poi id").toLowerCase(Locale.ROOT);
    }

    public static EchoPoiId of(String value) {
        return new EchoPoiId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
