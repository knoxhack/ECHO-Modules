package com.knoxhack.echo.npcore.dialogue;

import java.util.List;

public record EchoNpcDialogueNode(String text, List<EchoNpcDialogueOption> options) {
    public EchoNpcDialogueNode {
        text = text == null || text.isBlank() ? "No dialogue available." : text.trim();
        options = List.copyOf(options == null ? List.of() : options);
    }
}
