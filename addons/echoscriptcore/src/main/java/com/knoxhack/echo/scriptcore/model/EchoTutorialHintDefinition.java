package com.knoxhack.echo.scriptcore.model;

import com.google.gson.JsonObject;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import java.util.List;

public record EchoTutorialHintDefinition(
        EchoScriptDefinition base,
        String message,
        List<EchoCondition> triggerConditions,
        int priority,
        boolean once,
        JsonObject terminalCard) implements DelegatingScriptDefinition {
    public EchoTutorialHintDefinition {
        message = message == null ? "" : message;
        triggerConditions = List.copyOf(triggerConditions == null ? List.of() : triggerConditions);
        terminalCard = terminalCard == null ? new JsonObject() : terminalCard.deepCopy();
    }
}
