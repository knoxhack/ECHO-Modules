package com.knoxhack.echo.questdirector;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoRoutePacing(
        String pacingId,
        EchoContentReference routeReference,
        double targetProgressPerHour,
        int reminderIntervalTicks,
        int gracePeriodTicks,
        boolean allowCatchUp,
        Map<String, String> attributes
) {
    public EchoRoutePacing {
        pacingId = QuestDirectorContractGuards.id(pacingId, "route pacing id");
        targetProgressPerHour = QuestDirectorContractGuards.nonNegative(targetProgressPerHour, "target progress per hour");
        reminderIntervalTicks = QuestDirectorContractGuards.nonNegative(reminderIntervalTicks, "reminder interval ticks");
        gracePeriodTicks = QuestDirectorContractGuards.nonNegative(gracePeriodTicks, "grace period ticks");
        attributes = QuestDirectorContractGuards.immutableMap(attributes);
    }
}
