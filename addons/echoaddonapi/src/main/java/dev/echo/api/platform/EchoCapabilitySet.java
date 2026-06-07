package dev.echo.api.platform;

import java.util.Set;

public record EchoCapabilitySet(Set<EchoPlatformCapability> values) {
    public EchoCapabilitySet {
        values = Set.copyOf(values);
    }

    public boolean has(EchoPlatformCapability capability) {
        return values.contains(capability);
    }
}
