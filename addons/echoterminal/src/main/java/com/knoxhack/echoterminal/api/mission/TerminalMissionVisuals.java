package com.knoxhack.echoterminal.api.mission;

import com.knoxhack.echoterminal.api.TerminalVisualAssets;
import java.util.Locale;
import net.minecraft.resources.Identifier;

public record TerminalMissionVisuals(
        Identifier categoryArt,
        String trackType,
        String heroVariant,
        String visualTone) {
    public TerminalMissionVisuals {
        categoryArt = categoryArt == null ? TerminalVisualAssets.MISSION_SIDE_OPS : categoryArt;
        trackType = clean(trackType, "side_ops");
        heroVariant = clean(heroVariant, "standard");
        visualTone = clean(visualTone, "neutral");
    }

    public static TerminalMissionVisuals fallback(
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot) {
        String category = definition == null ? "" : definition.category();
        Identifier art = TerminalVisualAssets.missionHeroArt(definition == null ? null : definition.id(), category);
        String track = trackType(category);
        String tone = snapshot == null ? "neutral" : switch (snapshot.status()) {
            case CLAIMABLE, COMPLETED, CLAIMED -> "success";
            case UNLOCKED -> "active";
            case LOCKED, VIEW_ONLY -> "locked";
        };
        return new TerminalMissionVisuals(art, track, "category", tone);
    }

    private static String trackType(String category) {
        String key = category == null ? "" : category.toLowerCase(Locale.ROOT);
        if (key.contains("survival")) {
            return "survival";
        }
        if (key.contains("craft")) {
            return "crafting";
        }
        if (key.contains("tech") || key.contains("research") || key.contains("power")) {
            return "tech";
        }
        if (key.contains("explor") || key.contains("world") || key.contains("route")) {
            return "exploration";
        }
        if (key.contains("combat") || key.contains("guardian") || key.contains("warden")) {
            return "combat";
        }
        if (key.contains("story") || key.contains("nexus") || key.contains("archive")) {
            return "story";
        }
        return "side_ops";
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
