package dev.echo.api.block;

import java.util.Objects;

public record EchoBlockId(String value) {
    public EchoBlockId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("block id must not be blank");
        }
    }
}
