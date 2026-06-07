package com.knoxhack.echo.scriptcore.api;

import net.minecraft.resources.Identifier;

public record EchoScriptRuntimeValue(
        Identifier key,
        String scope,
        String value) {
    public EchoScriptRuntimeValue {
        scope = scope == null || scope.isBlank() ? "unknown" : scope;
        value = value == null ? "" : value;
    }
}
