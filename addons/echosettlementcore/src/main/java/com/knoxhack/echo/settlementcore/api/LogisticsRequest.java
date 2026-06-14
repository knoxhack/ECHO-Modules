package com.knoxhack.echo.settlementcore.api;

import net.minecraft.resources.Identifier;

/**
 * Record describing a settlement logistics need.
 */
public record LogisticsRequest(
    Identifier itemId,
    int amountNeeded,
    int amountFulfilled,
    int priority
) {
    public LogisticsRequest {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId is required");
        }
        amountNeeded = Math.max(0, amountNeeded);
        amountFulfilled = Math.max(0, amountFulfilled);
        priority = Math.max(0, priority);
    }

    public int amountRemaining() {
        return Math.max(0, amountNeeded - amountFulfilled);
    }

    public boolean isFulfilled() {
        return amountFulfilled >= amountNeeded;
    }
}
