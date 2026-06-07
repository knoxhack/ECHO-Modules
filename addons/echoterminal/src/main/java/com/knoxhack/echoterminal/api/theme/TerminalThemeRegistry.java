package com.knoxhack.echoterminal.api.theme;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public final class TerminalThemeRegistry {
    private static final Map<Identifier, TerminalTheme> THEMES = new LinkedHashMap<>();
    private static Identifier defaultThemeId = BuiltinTerminalThemes.ECHO_CONSOLE;

    private TerminalThemeRegistry() {
    }

    public static void register(TerminalTheme theme) {
        Objects.requireNonNull(theme, "Terminal theme is required.");
        bootstrap();
        if (THEMES.containsKey(theme.id())) {
            throw new IllegalArgumentException("Duplicate terminal theme id: " + theme.id());
        }
        THEMES.put(theme.id(), theme);
    }

    public static TerminalTheme byId(Identifier id) {
        bootstrap();
        TerminalTheme theme = id == null ? null : THEMES.get(id);
        return theme == null ? defaultTheme() : theme;
    }

    public static TerminalTheme defaultTheme() {
        bootstrap();
        TerminalTheme theme = THEMES.get(defaultThemeId);
        if (theme == null && !THEMES.isEmpty()) {
            theme = THEMES.values().iterator().next();
        }
        return theme;
    }

    public static Identifier defaultThemeId() {
        bootstrap();
        return defaultThemeId;
    }

    public static boolean setDefaultTheme(Identifier id) {
        bootstrap();
        if (id == null || !THEMES.containsKey(id)) {
            return false;
        }
        defaultThemeId = id;
        return true;
    }

    public static List<TerminalTheme> all() {
        bootstrap();
        return List.copyOf(THEMES.values());
    }

    public static boolean contains(Identifier id) {
        bootstrap();
        return id != null && THEMES.containsKey(id);
    }

    public static List<Identifier> ids() {
        bootstrap();
        return new ArrayList<>(THEMES.keySet());
    }

    public static void clearForTests() {
        THEMES.clear();
        defaultThemeId = BuiltinTerminalThemes.ECHO_CONSOLE;
    }

    static void registerBuiltin(TerminalTheme theme) {
        THEMES.putIfAbsent(theme.id(), theme);
    }

    private static void bootstrap() {
        if (THEMES.isEmpty()) {
            BuiltinTerminalThemes.register();
        }
    }
}
