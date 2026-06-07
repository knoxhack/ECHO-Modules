package com.knoxhack.echolens.api;

import net.minecraft.resources.Identifier;

public record LensProviderDiagnostic(
        Identifier id,
        String providerClass,
        int priority,
        LensDataCategory category,
        boolean loaded,
        boolean enabled) {
    public LensProviderDiagnostic {
        if (id == null) {
            throw new IllegalArgumentException("Lens provider diagnostic id is required.");
        }
        providerClass = providerClass == null || providerClass.isBlank() ? "unknown" : providerClass.strip();
        category = category == null ? LensDataCategory.IDENTITY : category;
    }
}
