package dev.echo.api.addon;

import java.util.Objects;

public record EchoAddonId(String value) {
    public EchoAddonId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!value.matches("[a-z][a-z0-9_]{1,63}")) {
            throw new IllegalArgumentException("addon id must match [a-z][a-z0-9_]{1,63}");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
