package com.knoxhack.echomultiblockcore.api;

import net.minecraft.resources.Identifier;

public record CapabilityDiagnostic(
        Identifier capabilityId,
        String message,
        boolean blocking) {
    public CapabilityDiagnostic {
        capabilityId = capabilityId == null ? MultiblockCapability.WORKCELL.id() : capabilityId;
        message = message == null || message.isBlank() ? "Capability diagnostic unavailable." : message.strip();
    }
}
