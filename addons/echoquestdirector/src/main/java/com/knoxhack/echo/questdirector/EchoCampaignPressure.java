package com.knoxhack.echo.questdirector;

import java.util.Map;

public record EchoCampaignPressure(
        String pressureId,
        double currentPressure,
        double targetPressure,
        double increaseRate,
        double reliefRate,
        String playerSummary,
        Map<String, String> attributes
) {
    public EchoCampaignPressure {
        pressureId = QuestDirectorContractGuards.id(pressureId, "campaign pressure id");
        currentPressure = QuestDirectorContractGuards.ratio(currentPressure, "current pressure");
        targetPressure = QuestDirectorContractGuards.ratio(targetPressure, "target pressure");
        increaseRate = QuestDirectorContractGuards.nonNegative(increaseRate, "increase rate");
        reliefRate = QuestDirectorContractGuards.nonNegative(reliefRate, "relief rate");
        playerSummary = QuestDirectorContractGuards.optionalText(playerSummary);
        attributes = QuestDirectorContractGuards.immutableMap(attributes);
    }
}
