package com.knoxhack.echoterminal.api.mission;

import java.util.Locale;

public enum TerminalMissionRole {
    MAIN,
    /** Optional missions are nonblocking side objectives that may reveal context without gating the main route. */
    OPTIONAL,
    REFERENCE;

    public static TerminalMissionRole fallback(TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
        if (snapshot != null && snapshot.status() == TerminalMissionStatus.VIEW_ONLY) {
            return REFERENCE;
        }
        String haystack = ((definition == null ? "" : definition.category())
                + " "
                + (definition == null ? "" : definition.phaseTitle())
                + " "
                + (definition == null || definition.id() == null ? "" : definition.id().getPath()))
                .toLowerCase(Locale.ROOT);
        if (haystack.contains("optional") || haystack.contains("side") || haystack.contains("contract")) {
            return OPTIONAL;
        }
        if (haystack.contains("reference") || haystack.contains("preview") || haystack.contains("view")) {
            return REFERENCE;
        }
        return MAIN;
    }
}
