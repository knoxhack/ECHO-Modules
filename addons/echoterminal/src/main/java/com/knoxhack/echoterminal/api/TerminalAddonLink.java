package com.knoxhack.echoterminal.api;

import net.minecraft.resources.Identifier;

public record TerminalAddonLink(
        Identifier targetTabId,
        String label,
        String detail,
        int color) {
    public TerminalAddonLink {
        TerminalApiIds.requireLowercase(targetTabId, "Terminal addon link target tab");
        label = clean(label, "Open");
        detail = clean(detail, "");
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
