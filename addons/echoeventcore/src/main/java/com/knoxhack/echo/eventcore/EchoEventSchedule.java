package com.knoxhack.echo.eventcore;

import com.knoxhack.echo.packcore.EchoPackVariantId;

import java.util.Map;

public record EchoEventSchedule(
        String scheduleId,
        EchoEventScheduleKind kind,
        EchoPackVariantId packVariantId,
        long minDelayTicks,
        long maxDelayTicks,
        long cooldownTicks,
        long activeWindowStartTick,
        long activeWindowEndTick,
        boolean repeatable,
        Map<String, String> attributes
) {
    public EchoEventSchedule {
        scheduleId = EventContractGuards.id(scheduleId, "event schedule id");
        kind = kind == null ? EchoEventScheduleKind.UNKNOWN : kind;
        minDelayTicks = EventContractGuards.nonNegative(minDelayTicks, "min delay ticks");
        maxDelayTicks = EventContractGuards.nonNegative(maxDelayTicks, "max delay ticks");
        cooldownTicks = EventContractGuards.nonNegative(cooldownTicks, "cooldown ticks");
        activeWindowStartTick = EventContractGuards.nonNegative(activeWindowStartTick, "active window start tick");
        activeWindowEndTick = EventContractGuards.nonNegative(activeWindowEndTick, "active window end tick");
        if (maxDelayTicks < minDelayTicks) {
            throw new IllegalArgumentException("max delay ticks must be greater than or equal to min delay ticks");
        }
        if (activeWindowEndTick != 0L && activeWindowEndTick < activeWindowStartTick) {
            throw new IllegalArgumentException("active window end tick must be zero or greater than or equal to start tick");
        }
        attributes = EventContractGuards.immutableMap(attributes);
    }
}
