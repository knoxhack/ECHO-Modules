package com.knoxhack.echo.questdirector;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoMissionSelectionRule(
        String ruleId,
        EchoContentReference missionReference,
        EchoContentReference routeReference,
        EchoContentReference objectiveReference,
        EchoContentGate gate,
        double priority,
        boolean repeatable,
        Map<String, String> attributes
) {
    public EchoMissionSelectionRule {
        ruleId = QuestDirectorContractGuards.id(ruleId, "mission selection rule id");
        gate = gate == null ? EchoContentGate.open() : gate;
        priority = QuestDirectorContractGuards.nonNegative(priority, "mission priority");
        attributes = QuestDirectorContractGuards.immutableMap(attributes);
    }
}
