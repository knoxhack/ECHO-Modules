package com.knoxhack.echo.scriptcore.api;

import net.minecraft.resources.Identifier;

public record EchoScriptRuntimeMigrationEntry(
        Identifier fromKey,
        Identifier toKey,
        String scope,
        String value,
        boolean copied,
        String note) {
    public EchoScriptRuntimeMigrationEntry {
        scope = scope == null || scope.isBlank() ? "unknown" : scope;
        value = value == null ? "" : value;
        note = note == null ? "" : note;
    }
}
