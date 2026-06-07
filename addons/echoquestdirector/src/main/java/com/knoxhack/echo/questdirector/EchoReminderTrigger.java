package com.knoxhack.echo.questdirector;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoReminderTrigger(
        String reminderId,
        EchoContentReference targetReference,
        int delayTicks,
        int repeatTicks,
        int maxRepeats,
        String messageKey,
        Map<String, String> attributes
) {
    public EchoReminderTrigger {
        reminderId = QuestDirectorContractGuards.id(reminderId, "reminder id");
        delayTicks = QuestDirectorContractGuards.nonNegative(delayTicks, "delay ticks");
        repeatTicks = QuestDirectorContractGuards.nonNegative(repeatTicks, "repeat ticks");
        maxRepeats = QuestDirectorContractGuards.nonNegative(maxRepeats, "max repeats");
        messageKey = QuestDirectorContractGuards.optionalText(messageKey);
        attributes = QuestDirectorContractGuards.immutableMap(attributes);
    }
}
