package com.knoxhack.echo.npcore.dialogue;

import java.util.Map;
import net.minecraft.resources.Identifier;

public record EchoNpcDialogue(Identifier id, String start, Map<String, EchoNpcDialogueNode> nodes) {
    public EchoNpcDialogue {
        start = start == null || start.isBlank() ? "intro" : start.trim();
        nodes = Map.copyOf(nodes == null ? Map.of() : nodes);
    }

    public EchoNpcDialogueNode nodeOrFallback(String nodeId) {
        EchoNpcDialogueNode node = nodes.get(nodeId);
        if (node != null) {
            return node;
        }
        node = nodes.get(start);
        if (node != null) {
            return node;
        }
        return new EchoNpcDialogueNode("No dialogue available.", java.util.List.of());
    }
}
