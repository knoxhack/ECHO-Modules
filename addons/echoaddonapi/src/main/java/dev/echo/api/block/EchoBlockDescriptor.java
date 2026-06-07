package dev.echo.api.block;

import java.util.Objects;

public record EchoBlockDescriptor(EchoBlockId id, EchoBlockSettings settings, String translationKey) {
    public EchoBlockDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(settings, "settings");
        translationKey = Objects.requireNonNull(translationKey, "translationKey").trim();
    }
}
