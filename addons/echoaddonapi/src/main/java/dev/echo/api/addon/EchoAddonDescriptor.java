package dev.echo.api.addon;

import java.util.Objects;
import java.util.Set;

public record EchoAddonDescriptor(
        EchoAddonId id,
        EchoAddonVersion version,
        String name,
        EchoAddonKind kind,
        EchoAddonRole role,
        Set<EchoAddonRuntimeTarget> runtimeTargets
) {
    public EchoAddonDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        name = Objects.requireNonNull(name, "name").trim();
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(role, "role");
        runtimeTargets = Set.copyOf(runtimeTargets);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (runtimeTargets.isEmpty()) {
            throw new IllegalArgumentException("runtimeTargets must not be empty");
        }
    }
}
