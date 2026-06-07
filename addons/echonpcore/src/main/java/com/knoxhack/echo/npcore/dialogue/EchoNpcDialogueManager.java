package com.knoxhack.echo.npcore.dialogue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoNpcDialogueManager {
    private static volatile Map<Identifier, EchoNpcDialogue> dialogues = Map.of();

    private EchoNpcDialogueManager() {
    }

    public static void replace(Map<Identifier, EchoNpcDialogue> loaded) {
        dialogues = Map.copyOf(loaded == null ? Map.of() : loaded);
    }

    public static Optional<EchoNpcDialogue> get(Identifier id) {
        return Optional.ofNullable(dialogues.get(id));
    }

    public static EchoNpcDialogue getOrFallback(Identifier id) {
        EchoNpcDialogue dialogue = dialogues.get(id);
        if (dialogue != null) {
            return dialogue;
        }
        dialogue = dialogues.get(Identifier.fromNamespaceAndPath("echonpcore", "test_survivor"));
        if (dialogue != null) {
            return dialogue;
        }
        return new EchoNpcDialogue(
                Identifier.fromNamespaceAndPath("echonpcore", "missing"),
                "intro",
                Map.of("intro", new EchoNpcDialogueNode("No dialogue available.",
                        List.of(new EchoNpcDialogueOption("exit", "Close.", "", "close")))));
    }

    public static int count() {
        return dialogues.size();
    }
}
