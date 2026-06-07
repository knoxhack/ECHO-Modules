package com.knoxhack.echo.packcore;

public record EchoPackLockfileVersion(String value) {
    public EchoPackLockfileVersion {
        value = PackContractGuards.requireText(value, "pack lockfile version");
    }

    public static EchoPackLockfileVersion of(String value) {
        return new EchoPackLockfileVersion(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
