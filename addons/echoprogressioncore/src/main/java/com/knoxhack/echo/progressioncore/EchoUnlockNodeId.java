package com.knoxhack.echo.progressioncore;

import java.util.Locale;

public record EchoUnlockNodeId(String value) {
    public EchoUnlockNodeId {
        value = ProgressionContractGuards.requireText(value, "unlock node id").toLowerCase(Locale.ROOT);
    }

    public static EchoUnlockNodeId of(String value) {
        return new EchoUnlockNodeId(value);
    }

    public static EchoUnlockNodeId of(String namespace, String path) {
        return new EchoUnlockNodeId(
                ProgressionContractGuards.requireText(namespace, "unlock node id namespace")
                        + ":"
                        + ProgressionContractGuards.requireText(path, "unlock node id path")
        );
    }

    @Override
    public String toString() {
        return value;
    }
}
