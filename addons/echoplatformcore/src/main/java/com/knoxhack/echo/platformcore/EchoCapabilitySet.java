package com.knoxhack.echo.platformcore;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public record EchoCapabilitySet(Set<EchoCapabilityId> capabilities) {
    public EchoCapabilitySet {
        capabilities = EchoContractGuards.immutableSet(capabilities);
    }

    public static EchoCapabilitySet empty() {
        return new EchoCapabilitySet(Set.of());
    }

    public static EchoCapabilitySet of(EchoCapabilityId... capabilities) {
        return new EchoCapabilitySet(Arrays.stream(capabilities).collect(Collectors.toUnmodifiableSet()));
    }

    public boolean contains(EchoCapabilityId capabilityId) {
        return capabilities.contains(capabilityId);
    }
}
