package com.knoxhack.echopowergrid.api;

import java.util.UUID;

public record PowerGridSnapshot(
    UUID networkId,
    long totalGeneration,
    long totalDemand,
    long totalStored,
    long totalCapacity,
    EchoGridState state,
    EchoPowerQuality quality,
    int nodeCount
) {
    public boolean isPowered() {
        return state != EchoGridState.OFFLINE && state != EchoGridState.TRIPPED && state != EchoGridState.EMERGENCY;
    }

    /**
     * Returns the total drawable power from this network right now:
     * surplus generation per tick plus all stored energy in batteries and generator buffers.
     */
    public long availablePower() {
        return Math.max(0, totalGeneration - totalDemand + totalStored);
    }
}
