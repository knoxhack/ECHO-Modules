package com.knoxhack.echolens.registry;

import net.minecraft.resources.Identifier;

public record LensProviderHealth(
        Identifier id,
        boolean loaded,
        boolean categoryEnabled,
        boolean serverSafe,
        String registrationSource,
        int failureCount,
        String lastFailure) {
    public LensProviderHealth {
        registrationSource = registrationSource == null || registrationSource.isBlank()
                ? "unknown"
                : registrationSource.strip();
        lastFailure = lastFailure == null ? "" : lastFailure.strip();
    }

    public boolean healthy() {
        return failureCount <= 0 && lastFailure.isBlank();
    }
}
