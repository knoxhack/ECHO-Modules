package com.knoxhack.echo.scriptcore.model;

import java.util.List;

public record EchoDialogueDefinition(
        EchoScriptDefinition base,
        String speaker,
        List<String> lines,
        List<EchoDialogueChoice> choices) implements DelegatingScriptDefinition {
    public EchoDialogueDefinition {
        speaker = speaker == null ? "" : speaker;
        lines = List.copyOf(lines == null ? List.of() : lines);
        choices = List.copyOf(choices == null ? List.of() : choices);
    }
}
