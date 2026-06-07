package com.knoxhack.echoterminal.api.mission;

import java.util.List;

public record TerminalMissionPresentation(
        String shortTitle,
        String objectiveSummary,
        String nextStep,
        String routeHint,
        String statusTone,
        List<String> tags,
        String relatedIntelKey) {
    public TerminalMissionPresentation {
        shortTitle = clean(shortTitle, "Mission Record");
        objectiveSummary = clean(objectiveSummary, "");
        nextStep = clean(nextStep, "Review this protocol and continue the active route.");
        routeHint = clean(routeHint, "");
        statusTone = clean(statusTone, "neutral");
        tags = List.copyOf(tags == null ? List.of() : tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .toList());
        relatedIntelKey = clean(relatedIntelKey, "");
    }

    public static TerminalMissionPresentation fallback(
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot) {
        String title = definition == null ? "" : definition.title();
        String briefing = definition == null ? "" : definition.briefing();
        String phase = definition == null ? "" : definition.phaseTitle();
        String category = definition == null ? "" : definition.category();
        String difficulty = definition == null ? "" : definition.difficulty();
        String step = snapshot == null ? "" : snapshot.actionHint();
        String tone = snapshot == null ? "neutral" : switch (snapshot.status()) {
            case CLAIMABLE, COMPLETED, CLAIMED -> "success";
            case UNLOCKED -> "active";
            case LOCKED, VIEW_ONLY -> "muted";
        };
        return new TerminalMissionPresentation(
                title,
                briefing,
                step,
                phase,
                tone,
                List.of(category, difficulty),
                "");
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
