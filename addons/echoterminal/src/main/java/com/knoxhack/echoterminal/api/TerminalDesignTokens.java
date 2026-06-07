package com.knoxhack.echoterminal.api;

import java.util.Locale;

/**
 * Shared readability-first sizing and semantic color helpers for terminal pages.
 */
public final class TerminalDesignTokens {
    public static final int GAP_SMALL = 8;
    public static final int GAP_CARD = 12;
    public static final int GAP_SECTION = 16;
    public static final int GAP_MAJOR = 24;
    public static final int CARD_PADDING = 12;
    public static final int BUTTON_HEIGHT = 24;
    public static final int LARGE_BUTTON_HEIGHT = 30;
    public static final int CHIP_HEIGHT = 16;
    public static final int LIST_ROW_HEIGHT = 42;
    public static final int CYBERGLASS_RADIUS = 4;
    public static final int CYBERGLASS_PANEL_RADIUS = 6;
    public static final int MISSION_PHASE_ROW_HEIGHT = 64;
    public static final int MISSION_BRIEFING_ROW_HEIGHT = 42;
    public static final int MISSION_ACTION_BUTTON_HEIGHT = 30;
    public static final String ACTION_FOCUS_ACTIVE_MISSION = "terminal.focus_active_mission";
    public static final String MISSION_BRIEFING_SCROLL_KEY = "terminal.missions.briefing";
    public static final boolean EXPAND_CURRENT_PHASE_BY_DEFAULT = true;

    private TerminalDesignTokens() {
    }

    public static int cyberglassRowHeight(String role) {
        String value = role == null ? "" : role.strip().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "phase", "mission" -> MISSION_PHASE_ROW_HEIGHT;
            case "briefing", "requirement", "reward" -> MISSION_BRIEFING_ROW_HEIGHT;
            case "compact" -> 28;
            default -> LIST_ROW_HEIGHT;
        };
    }

    public static String normalizeStatus(String label) {
        String value = label == null ? "" : label.strip().toUpperCase(Locale.ROOT);
        if (value.equals("READY TO CLAIM") || value.equals("CLAIMABLE")) {
            return "CLAIM";
        }
        if (value.equals("COMPLETED") || value.equals("CLAIMED") || value.equals("COMPLETE")) {
            return "DONE";
        }
        if (value.equals("UNLOCKED") || value.equals("AVAILABLE")) {
            return "READY";
        }
        if (value.equals("BLOCKER") || value.equals("BLOCKED") || value.equals("DANGER")) {
            return "MISSING";
        }
        if (value.equals("NEEDED") || value.equals("NEED") || value.equals("INCOMPLETE")) {
            return "MISSING";
        }
        if (value.equals("VIEW") || value.equals("REFERENCE")) {
            return "INFO";
        }
        return value;
    }

    public static boolean semanticStatus(String label) {
        return switch (normalizeStatus(label)) {
            case "READY", "ACTIVE", "DONE", "LOCKED", "MISSING", "WARNING", "CLAIM", "OPTIONAL", "INFO",
                    "OPEN", "ONLINE", "OFFLINE", "UNAVAILABLE", "IDLE" -> true;
            default -> false;
        };
    }

    public static int statusColor(TerminalRenderContext context, String label, int fallback) {
        return switch (normalizeStatus(label)) {
            case "READY", "CLAIM", "OPEN", "ONLINE" -> TerminalUi.success(context);
            case "ACTIVE", "WARNING" -> TerminalUi.warning(context);
            case "DONE" -> TerminalUi.success(context);
            case "MISSING", "OFFLINE", "UNAVAILABLE" -> TerminalUi.danger(context);
            case "LOCKED", "IDLE" -> TerminalUi.muted(context);
            case "OPTIONAL", "INFO" -> TerminalUi.accent(context);
            default -> fallback;
        };
    }
}
