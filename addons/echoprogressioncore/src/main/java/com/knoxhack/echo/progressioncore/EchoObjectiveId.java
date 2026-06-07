package com.knoxhack.echo.progressioncore;

import java.util.Locale;

public record EchoObjectiveId(String value) {
    public EchoObjectiveId {
        value = ProgressionContractGuards.requireText(value, "objective id").toLowerCase(Locale.ROOT);
    }

    public static EchoObjectiveId of(String value) {
        return new EchoObjectiveId(value);
    }

    public static EchoObjectiveId of(String namespace, String path) {
        return new EchoObjectiveId(
                ProgressionContractGuards.requireText(namespace, "objective id namespace")
                        + ":"
                        + ProgressionContractGuards.requireText(path, "objective id path")
        );
    }

    public String namespace() {
        int split = value.indexOf(':');
        return split < 0 ? "" : value.substring(0, split);
    }

    public String path() {
        int split = value.indexOf(':');
        return split < 0 ? value : value.substring(split + 1);
    }

    @Override
    public String toString() {
        return value;
    }
}
