package com.knoxhack.echomultiblockcore.api;

import net.minecraft.resources.Identifier;

public record CapabilityRequirement(
        Identifier capabilityId,
        int amount,
        int throughput,
        String unit,
        boolean required) {
    public CapabilityRequirement {
        capabilityId = capabilityId == null ? MultiblockCapability.POWER_INPUT.id() : capabilityId;
        amount = Math.max(0, amount);
        throughput = Math.max(0, throughput);
        unit = unit == null || unit.isBlank() ? "units" : unit.strip();
    }

    public static CapabilityRequirement of(Identifier capabilityId, int amount, int throughput, String unit) {
        return new CapabilityRequirement(capabilityId, amount, throughput, unit, true);
    }
}
