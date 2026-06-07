package com.knoxhack.echoterminal.api.mission;

import com.knoxhack.echoterminal.api.TerminalApiIds;
import net.minecraft.resources.Identifier;

public record TerminalMissionIntelUnlock(
        TerminalMissionIntelKind kind,
        Identifier id,
        String title,
        String summary) {
    public TerminalMissionIntelUnlock {
        kind = kind == null ? TerminalMissionIntelKind.DISCOVERY : kind;
        id = TerminalApiIds.requireLowercase(id, "Terminal mission intel unlock");
        title = clean(title, readable(id));
        summary = clean(summary, "");
    }

    public static TerminalMissionIntelUnlock archive(Identifier id, String title, String summary) {
        return new TerminalMissionIntelUnlock(TerminalMissionIntelKind.ARCHIVE, id, title, summary);
    }

    public static TerminalMissionIntelUnlock route(Identifier id, String title, String summary) {
        return new TerminalMissionIntelUnlock(TerminalMissionIntelKind.ROUTE, id, title, summary);
    }

    public static TerminalMissionIntelUnlock discovery(Identifier id, String title, String summary) {
        return new TerminalMissionIntelUnlock(TerminalMissionIntelKind.DISCOVERY, id, title, summary);
    }

    public static TerminalMissionIntelUnlock faction(Identifier id, String title, String summary) {
        return new TerminalMissionIntelUnlock(TerminalMissionIntelKind.FACTION, id, title, summary);
    }

    public static TerminalMissionIntelUnlock poi(Identifier id, String title, String summary) {
        return new TerminalMissionIntelUnlock(TerminalMissionIntelKind.POI, id, title, summary);
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String readable(Identifier id) {
        if (id == null) {
            return "Intel";
        }
        String path = id.getPath().replace('/', '_');
        StringBuilder builder = new StringBuilder();
        for (String part : path.split("_+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? id.toString() : builder.toString();
    }
}
