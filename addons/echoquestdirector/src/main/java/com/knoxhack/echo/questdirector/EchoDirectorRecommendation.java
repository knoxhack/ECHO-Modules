package com.knoxhack.echo.questdirector;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoDirectorRecommendation(
        String recommendationId,
        EchoDirectorRecommendationKind kind,
        EchoContentReference targetReference,
        double priority,
        boolean requiresConfirmation,
        String playerSummary,
        String developerDetails,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoDirectorRecommendation {
        recommendationId = QuestDirectorContractGuards.id(recommendationId, "director recommendation id");
        kind = kind == null ? EchoDirectorRecommendationKind.UNKNOWN : kind;
        priority = QuestDirectorContractGuards.nonNegative(priority, "recommendation priority");
        playerSummary = QuestDirectorContractGuards.optionalText(playerSummary);
        developerDetails = QuestDirectorContractGuards.optionalText(developerDetails);
        diagnostics = QuestDirectorContractGuards.immutableList(diagnostics);
        attributes = QuestDirectorContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
