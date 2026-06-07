package com.knoxhack.echoterminal.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record TerminalContact(
        Identifier id,
        String displayName,
        String role,
        Identifier factionId,
        String summary,
        long lastInteractionTick,
        List<String> serviceLines,
        List<Identifier> missionIds) {
    public TerminalContact {
        TerminalApiIds.requireLowercase(id, "Terminal contact id");
        displayName = clean(displayName, id.getPath());
        role = clean(role, "Contact");
        summary = clean(summary, "");
        serviceLines = List.copyOf(serviceLines == null ? List.of() : serviceLines.stream()
                .filter(line -> line != null && !line.isBlank())
                .map(String::trim)
                .toList());
        missionIds = List.copyOf(missionIds == null ? List.of() : missionIds.stream()
                .filter(mission -> mission != null)
                .toList());
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim();
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
