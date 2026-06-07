package com.knoxhack.echo.progressioncore;

import java.util.Locale;

public record EchoProgressionId(String value) {
    public EchoProgressionId {
        value = ProgressionContractGuards.requireText(value, "progression id").toLowerCase(Locale.ROOT);
    }

    public static EchoProgressionId of(String value) {
        return new EchoProgressionId(value);
    }

    public static EchoProgressionId of(String namespace, String path) {
        return new EchoProgressionId(
                ProgressionContractGuards.requireText(namespace, "progression id namespace")
                        + ":"
                        + ProgressionContractGuards.requireText(path, "progression id path")
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

    public boolean namespaced() {
        return value.indexOf(':') > 0 && value.indexOf(':') < value.length() - 1;
    }

    @Override
    public String toString() {
        return value;
    }
}
