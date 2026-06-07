package com.knoxhack.echo.scriptcore.model;

import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import java.util.List;

public record EchoEndingDefinition(
        EchoScriptDefinition base,
        int priority,
        List<EchoCondition> endingConditions,
        List<EchoAction> endingActions,
        String terminalSummary) implements DelegatingScriptDefinition {
    public EchoEndingDefinition {
        endingConditions = List.copyOf(endingConditions == null ? List.of() : endingConditions);
        endingActions = List.copyOf(endingActions == null ? List.of() : endingActions);
        terminalSummary = terminalSummary == null ? "" : terminalSummary;
    }
}
