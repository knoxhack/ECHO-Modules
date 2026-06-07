package com.knoxhack.echo.questdirector;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoWorldEventPacing(
        String pacingId,
        EchoContentReference worldEventReference,
        double targetEventsPerHour,
        int minimumGapTicks,
        int maximumConcurrentEvents,
        boolean allowEscalation,
        Map<String, String> attributes
) {
    public EchoWorldEventPacing {
        pacingId = QuestDirectorContractGuards.id(pacingId, "world event pacing id");
        targetEventsPerHour = QuestDirectorContractGuards.nonNegative(targetEventsPerHour, "target events per hour");
        minimumGapTicks = QuestDirectorContractGuards.nonNegative(minimumGapTicks, "minimum gap ticks");
        maximumConcurrentEvents = QuestDirectorContractGuards.nonNegative(maximumConcurrentEvents, "maximum concurrent events");
        attributes = QuestDirectorContractGuards.immutableMap(attributes);
    }
}
