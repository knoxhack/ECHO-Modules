package com.knoxhack.echo.scriptcore.model;

import com.knoxhack.echo.scriptcore.api.EchoAction;
import com.knoxhack.echo.scriptcore.api.EchoCondition;
import java.util.List;

public record EchoWorldStateDefinition(
        EchoScriptDefinition base,
        List<EchoCondition> setBy,
        List<EchoAction> effects) implements DelegatingScriptDefinition {
    public EchoWorldStateDefinition {
        setBy = List.copyOf(setBy == null ? List.of() : setBy);
        effects = List.copyOf(effects == null ? List.of() : effects);
    }
}
