package com.knoxhack.echomultiblockcore.api;

import net.minecraft.resources.Identifier;

public record CapabilityThroughput(
        Identifier capabilityId,
        int required,
        int available,
        int throughput,
        String unit) {
    public CapabilityThroughput {
        capabilityId = capabilityId == null ? MultiblockCapability.WORKCELL.id() : capabilityId;
        required = Math.max(0, required);
        available = Math.max(0, available);
        throughput = Math.max(0, throughput);
        unit = unit == null || unit.isBlank() ? "units" : unit.strip();
    }

    public boolean satisfied() {
        return available >= required;
    }
}
