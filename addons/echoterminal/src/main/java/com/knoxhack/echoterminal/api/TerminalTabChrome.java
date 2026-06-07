package com.knoxhack.echoterminal.api;

import java.util.Locale;

/**
 * Optional presentation metadata for the modular terminal shell.
 * Existing tabs can rely on {@link #fromDescriptor(TerminalTabDescriptor)}.
 */
public record TerminalTabChrome(
        String shortTitle,
        String group,
        String iconLabel,
        String summary,
        int priority) {
    public static final String GROUP_CORE = "CORE";
    public static final String GROUP_PROTOCOL = "PROTOCOL";
    public static final String GROUP_FIELD = "FIELD";
    public static final String GROUP_SYSTEMS = "SYSTEMS";
    public static final String GROUP_ENDGAME = "ENDGAME";
    public static final String GROUP_NEXUS = "NEXUS";
    public static final String GROUP_ORBITAL = "ORBITAL";
    public static final String GROUP_ADDONS = "ADDONS";

    public TerminalTabChrome {
        shortTitle = clean(shortTitle, "TAB");
        group = clean(group, GROUP_ADDONS).toUpperCase(Locale.ROOT);
        iconLabel = clean(iconLabel, fallbackIcon(shortTitle)).toUpperCase(Locale.ROOT);
        summary = summary == null ? "" : summary.strip();
    }

    public static TerminalTabChrome of(String shortTitle, String group, String iconLabel, String summary, int priority) {
        return new TerminalTabChrome(shortTitle, group, iconLabel, summary, priority);
    }

    public static TerminalTabChrome fromDescriptor(TerminalTabDescriptor descriptor) {
        if (descriptor == null) {
            return new TerminalTabChrome("TAB", GROUP_ADDONS, "TB", "", 0);
        }
        String title = clean(descriptor.title(), "TAB");
        String upper = title.toUpperCase(Locale.ROOT);
        String shortTitle = switch (upper) {
            case "OVERVIEW", "COMMAND DECK" -> "Command Deck";
            case "TERMINAL SETTINGS", "INTERFACE SETTINGS" -> "Interface Settings";
            case "MISSIONS", "PROTOCOL ROADMAP" -> "Protocol Roadmap";
            case "SIDE OPS", "SIGNAL LEADS" -> "Signal Leads";
            case "ARCHIVES", "FIELD ARCHIVE" -> "Field Archive";
            case "CODEX", "SURVIVAL INDEX" -> "Survival Index";
            case "WORLD", "ROUTE MAP" -> "Route Map";
            case "VANILLA", "BASELINE" -> "Baseline";
            case "STATUS", "VITALS SCAN" -> "Vitals Scan";
            case "DRONE", "COMPANION LINK" -> "Companion Link";
            case "NEXUS", "NEXUS CORE" -> "Nexus Core";
            case "ORBITAL", "ORBITAL COMMAND" -> "Orbital Command";
            case "SURVEY", "ROUTE SURVEY" -> "Route Survey";
            case "ECHO", "ECHO-0 RECORDS" -> "ECHO-0 Records";
            default -> title;
        };
        String group = switch (upper) {
            case "OVERVIEW", "COMMAND DECK", "MISSIONS", "PROTOCOL ROADMAP", "SIDE OPS", "SIGNAL LEADS" ->
                    GROUP_PROTOCOL;
            case "ARCHIVES", "FIELD ARCHIVE", "CODEX", "SURVIVAL INDEX", "WORLD", "ROUTE MAP", "VANILLA",
                    "BASELINE" -> GROUP_FIELD;
            case "STATUS", "VITALS SCAN", "DRONE", "COMPANION LINK", "SYSTEMS",
                    "TERMINAL SETTINGS", "INTERFACE SETTINGS" -> GROUP_SYSTEMS;
            case "NEXUS", "NEXUS CORE" -> GROUP_NEXUS;
            case "ORBITAL", "ORBITAL COMMAND", "SURVEY", "ROUTE SURVEY", "ECHO", "ECHO-0 RECORDS" ->
                    GROUP_ORBITAL;
            default -> GROUP_ADDONS;
        };
        String icon = switch (upper) {
            case "OVERVIEW", "COMMAND DECK" -> "CD";
            case "TERMINAL SETTINGS", "INTERFACE SETTINGS" -> "IS";
            case "MISSIONS", "PROTOCOL ROADMAP" -> "PR";
            case "SIDE OPS", "SIGNAL LEADS" -> "SL";
            case "STATUS", "VITALS SCAN" -> "VS";
            case "ARCHIVES", "FIELD ARCHIVE" -> "FA";
            case "CODEX", "SURVIVAL INDEX" -> "SI";
            case "WORLD", "ROUTE MAP" -> "RM";
            case "VANILLA", "BASELINE" -> "BL";
            case "DRONE", "COMPANION LINK" -> "CL";
            case "NEXUS", "NEXUS CORE" -> "NX";
            case "ORBITAL", "ORBITAL COMMAND" -> "OC";
            case "SURVEY", "ROUTE SURVEY" -> "RS";
            case "ECHO", "ECHO-0 RECORDS" -> "E0";
            default -> fallbackIcon(shortTitle);
        };
        String summary = switch (upper) {
            case "OVERVIEW", "COMMAND DECK" -> "Active protocol dashboard";
            case "TERMINAL SETTINGS", "INTERFACE SETTINGS" -> "Presentation and accessibility";
            case "MISSIONS", "PROTOCOL ROADMAP" -> "ECHO-7 route objectives";
            case "SIDE OPS", "SIGNAL LEADS" -> "Optional recon signals";
            case "STATUS", "VITALS SCAN" -> "Systems and hazard scan";
            case "ARCHIVES", "FIELD ARCHIVE" -> "Recovered field records";
            case "CODEX", "SURVIVAL INDEX" -> "Intel and recipes index";
            case "WORLD", "ROUTE MAP" -> "Routes, POIs, signal map";
            case "VANILLA", "BASELINE" -> "Recovered Minecraft tasks";
            case "DRONE", "COMPANION LINK" -> "Drone command channel";
            case "NEXUS", "NEXUS CORE" -> "Final path control";
            case "ORBITAL", "ORBITAL COMMAND" -> "ECHO-0 route telemetry";
            case "SURVEY", "ROUTE SURVEY" -> "ECHO-0 survey network";
            case "ECHO", "ECHO-0 RECORDS" -> "ECHO-0 route records";
            default -> "";
        };
        return new TerminalTabChrome(shortTitle, group, icon, summary, descriptor.order());
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private static String fallbackIcon(String title) {
        String cleaned = clean(title, "TB").replaceAll("[^A-Za-z0-9]", "");
        if (cleaned.length() >= 2) {
            return cleaned.substring(0, 2);
        }
        return cleaned.isEmpty() ? "TB" : cleaned;
    }
}
