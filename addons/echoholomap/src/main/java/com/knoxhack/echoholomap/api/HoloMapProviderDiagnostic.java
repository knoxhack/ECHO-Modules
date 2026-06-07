package com.knoxhack.echoholomap.api;

import com.knoxhack.echoholomap.HoloMapIds;
import net.minecraft.resources.Identifier;

public record HoloMapProviderDiagnostic(
        Identifier providerId,
        String providerType,
        boolean healthy,
        int layers,
        int markers,
        int routes,
        int overlays,
        String message,
        long failures) {
    public HoloMapProviderDiagnostic {
        providerId = providerId == null ? HoloMapIds.id("provider/unknown") : providerId;
        providerType = providerType == null || providerType.isBlank() ? "unknown" : providerType.strip();
        layers = Math.max(0, layers);
        markers = Math.max(0, markers);
        routes = Math.max(0, routes);
        overlays = Math.max(0, overlays);
        message = message == null ? "" : message.strip();
        failures = Math.max(0L, failures);
    }
}
