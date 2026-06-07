package com.knoxhack.echo.powercore;

import java.util.Map;

public record EchoPowerStorageProfile(
        long capacity,
        long stored,
        long maxInputPerTick,
        long maxOutputPerTick,
        double passiveLossPerTick,
        Map<String, String> attributes
) {
    public EchoPowerStorageProfile {
        capacity = PowerContractGuards.nonNegative(capacity, "power storage capacity");
        stored = Math.min(PowerContractGuards.nonNegative(stored, "stored power"), capacity);
        maxInputPerTick = PowerContractGuards.nonNegative(maxInputPerTick, "max input per tick");
        maxOutputPerTick = PowerContractGuards.nonNegative(maxOutputPerTick, "max output per tick");
        passiveLossPerTick = PowerContractGuards.nonNegative(passiveLossPerTick, "passive loss per tick");
        attributes = PowerContractGuards.immutableMap(attributes);
    }

    public boolean storesEnergy() {
        return capacity > 0L;
    }
}
