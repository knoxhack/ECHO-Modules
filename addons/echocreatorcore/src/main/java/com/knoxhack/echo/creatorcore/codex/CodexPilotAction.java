package com.knoxhack.echo.creatorcore.codex;

import com.google.gson.JsonObject;

public record CodexPilotAction(
        String id,
        String command,
        JsonObject args,
        String prompt,
        String source) {
    public CodexPilotAction {
        id = safe(id);
        command = safe(command);
        args = args == null ? new JsonObject() : args;
        prompt = safe(prompt);
        source = safe(source);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
