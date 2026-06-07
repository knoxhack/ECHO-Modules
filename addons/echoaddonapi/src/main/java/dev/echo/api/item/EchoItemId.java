package dev.echo.api.item;

import java.util.Objects;

public record EchoItemId(String value) {
    public EchoItemId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("item id must not be blank");
        }
    }
}
