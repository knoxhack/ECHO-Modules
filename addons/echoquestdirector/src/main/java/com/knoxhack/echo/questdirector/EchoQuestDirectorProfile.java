package com.knoxhack.echo.questdirector;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoQuestDirectorProfile(
        EchoQuestDirectorId id,
        String displayName,
        EchoModuleId owningModule,
        EchoPackId packId,
        List<EchoMissionSelectionRule> missionSelectionRules,
        List<EchoRoutePacing> routePacing,
        List<EchoWorldEventPacing> worldEventPacing,
        List<EchoCampaignPressure> campaignPressures,
        List<EchoReminderTrigger> reminderTriggers,
        List<EchoRecoveryAssistancePacing> recoveryAssistance,
        List<EchoDirectorSignal> signals,
        List<EchoDirectorRecommendation> recommendations,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoQuestDirectorProfile {
        Objects.requireNonNull(id, "id");
        displayName = QuestDirectorContractGuards.requireText(displayName, "quest director display name");
        missionSelectionRules = QuestDirectorContractGuards.immutableList(missionSelectionRules);
        routePacing = QuestDirectorContractGuards.immutableList(routePacing);
        worldEventPacing = QuestDirectorContractGuards.immutableList(worldEventPacing);
        campaignPressures = QuestDirectorContractGuards.immutableList(campaignPressures);
        reminderTriggers = QuestDirectorContractGuards.immutableList(reminderTriggers);
        recoveryAssistance = QuestDirectorContractGuards.immutableList(recoveryAssistance);
        signals = QuestDirectorContractGuards.immutableList(signals);
        recommendations = QuestDirectorContractGuards.immutableList(recommendations);
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = QuestDirectorContractGuards.immutableList(diagnostics);
        attributes = QuestDirectorContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return gate.blocksWhenMissing()
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || recommendations.stream().anyMatch(EchoDirectorRecommendation::blocking);
    }
}
