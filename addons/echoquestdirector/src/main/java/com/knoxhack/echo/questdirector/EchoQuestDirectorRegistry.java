package com.knoxhack.echo.questdirector;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoQuestDirectorRegistry(
        Map<EchoQuestDirectorId, EchoQuestDirectorProfile> profiles,
        List<EchoDirectorSignal> signals,
        List<EchoDirectorRecommendation> recommendations,
        List<EchoDiagnostic> diagnostics
) {
    public EchoQuestDirectorRegistry {
        profiles = QuestDirectorContractGuards.immutableMap(profiles);
        signals = QuestDirectorContractGuards.immutableList(signals);
        recommendations = QuestDirectorContractGuards.immutableList(recommendations);
        diagnostics = QuestDirectorContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || profiles.values().stream().anyMatch(EchoQuestDirectorProfile::blocking)
                || recommendations.stream().anyMatch(EchoDirectorRecommendation::blocking);
    }
}
