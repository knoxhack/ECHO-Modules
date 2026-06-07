package dev.echo.api.addon;

import java.util.Objects;

public record EchoAddonVersion(String value) {
    public EchoAddonVersion {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("version must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
