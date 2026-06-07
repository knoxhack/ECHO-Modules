package com.knoxhack.echo.scriptcore.api;

import java.util.Set;
import net.minecraft.resources.Identifier;

public interface EchoScriptAdapter {
    Identifier id();

    boolean isAvailable();

    Set<String> supportedDefinitionTypes();

    Set<String> supportedActions();

    Set<String> supportedConditions();

    void registerDefinitions(EchoScriptRegistryView registry, EchoDiagnosticSink diagnostics);

    EchoActionResult executeAction(EchoAction action, EchoScriptExecutionContext context);

    EchoConditionResult evaluateCondition(EchoCondition condition, EchoScriptExecutionContext context);
}
