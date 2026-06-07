package com.knoxhack.echo.scriptcore.api;

import java.util.List;

public record EchoScriptRuntimeSnapshot(
        boolean available,
        String backend,
        String owner,
        List<EchoScriptRuntimeValue> values) {
    public EchoScriptRuntimeSnapshot {
        backend = backend == null || backend.isBlank() ? "unavailable" : backend;
        owner = owner == null || owner.isBlank() ? "unknown" : owner;
        values = List.copyOf(values == null ? List.of() : values);
    }
}
