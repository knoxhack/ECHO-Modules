package com.knoxhack.echo.packcore;

import java.util.Locale;

public record EchoPackLockfileId(String value) {
    public EchoPackLockfileId {
        value = PackContractGuards.requireText(value, "pack lockfile id").toLowerCase(Locale.ROOT);
    }

    public static EchoPackLockfileId of(String value) {
        return new EchoPackLockfileId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
