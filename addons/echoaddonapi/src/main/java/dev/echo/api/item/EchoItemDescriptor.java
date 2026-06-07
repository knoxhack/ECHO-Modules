package dev.echo.api.item;

import java.util.Objects;

public record EchoItemDescriptor(EchoItemId id, EchoItemSettings settings, String translationKey) {
    public EchoItemDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(settings, "settings");
        translationKey = Objects.requireNonNull(translationKey, "translationKey").trim();
    }
}
