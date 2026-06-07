package com.knoxhack.echoterminal.api.theme;

import java.util.Locale;

/**
 * Semantic icon lookup key used by terminal themes. Keys are intentionally
 * descriptive so theme authors can replace icon art without coupling to the
 * current file layout.
 */
public record TerminalIconKey(String category, String name) {
    public TerminalIconKey {
        category = normalize(category, "fallback");
        name = normalize(name, "unknown");
    }

    public static TerminalIconKey of(String category, String name) {
        return new TerminalIconKey(category, name);
    }

    public static TerminalIconKey group(String name) {
        return of("group", name);
    }

    public static TerminalIconKey page(String name) {
        return of("page", name);
    }

    public static TerminalIconKey action(String name) {
        return of("action", name);
    }

    public static TerminalIconKey state(String name) {
        return of("state", name);
    }

    public static TerminalIconKey missionCategory(String name) {
        return of("mission_category", name);
    }

    public static TerminalIconKey reward(String name) {
        return of("reward", name);
    }

    public static TerminalIconKey chapter(String name) {
        return of("chapter", name);
    }

    public static TerminalIconKey theme(String name) {
        return of("theme", name);
    }

    public static TerminalIconKey fallback(String name) {
        return of("fallback", name);
    }

    private static String normalize(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_./:-]+", "_");
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
