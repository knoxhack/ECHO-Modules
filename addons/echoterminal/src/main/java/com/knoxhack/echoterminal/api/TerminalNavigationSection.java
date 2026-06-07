package com.knoxhack.echoterminal.api;

import java.util.Locale;
import java.util.List;

/**
 * Top-level command areas for the terminal shell.
 */
public enum TerminalNavigationSection {
    TERMINAL("Command", 0),
    CORE("Intel", 200),
    CHAPTERS("Progress", 100),
    COMMAND("Command", 0),
    INTEL("Intel", 200),
    INDEX("Index", 240),
    HOLOMAP("HoloMap", 260),
    SYSTEM("System", 300);

    private final String label;
    private final int order;

    TerminalNavigationSection(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String key() {
        return name();
    }

    public String label() {
        return label;
    }

    public int order() {
        return order;
    }

    public TerminalNavigationSection canonical() {
        return switch (this) {
            case TERMINAL -> COMMAND;
            case CORE -> INTEL;
            default -> this;
        };
    }

    public static List<TerminalNavigationSection> storyFirstOrder() {
        return List.of(COMMAND, CHAPTERS, INTEL, INDEX, HOLOMAP, SYSTEM);
    }

    public static TerminalNavigationSection fromKey(String key) {
        String normalized = key == null ? "" : key.strip().toUpperCase(Locale.ROOT);
        for (TerminalNavigationSection section : values()) {
            if (section.name().equals(normalized)) {
                return section.canonical();
            }
        }
        return COMMAND;
    }
}
