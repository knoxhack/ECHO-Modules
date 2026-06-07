package com.knoxhack.echo.npcore.dialogue;

public record EchoNpcDialogueOption(
        String id,
        String label,
        String next,
        String action,
        String requiresMission,
        int requiresFactionStanding,
        String disabledReason,
        String target,
        String actionId) {
    public EchoNpcDialogueOption(String id, String label, String next, String action) {
        this(id, label, next, action, "", Integer.MIN_VALUE, "", "", "");
    }

    public EchoNpcDialogueOption {
        id = clean(id, "option");
        label = clean(label, id);
        next = clean(next, "");
        action = clean(action, "");
        requiresMission = clean(requiresMission, "");
        disabledReason = clean(disabledReason, "");
        target = clean(target, "");
        actionId = clean(actionId, "");
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
