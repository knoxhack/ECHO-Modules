package com.knoxhack.echoterminal.api;

import java.util.List;

public record TerminalAddonSection(
        String title,
        List<String> lines) {
    public TerminalAddonSection {
        title = clean(title, "Section");
        lines = List.copyOf(lines == null
                ? List.of()
                : lines.stream()
                        .filter(line -> line != null && !line.isBlank())
                        .map(String::strip)
                        .toList());
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
