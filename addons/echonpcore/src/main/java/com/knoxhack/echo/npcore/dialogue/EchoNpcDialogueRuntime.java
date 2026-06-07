package com.knoxhack.echo.npcore.dialogue;

public final class EchoNpcDialogueRuntime {
    private EchoNpcDialogueRuntime() {
    }

    public static String safeStart(EchoNpcDialogue dialogue) {
        if (dialogue == null || dialogue.start() == null || dialogue.start().isBlank()) {
            return "intro";
        }
        return dialogue.start();
    }

    public static EchoNpcDialogueOption findOption(EchoNpcDialogueNode node, String optionId) {
        if (node == null || optionId == null) {
            return null;
        }
        return node.options().stream()
                .filter(option -> option.id().equals(optionId))
                .findFirst()
                .orElse(null);
    }
}
