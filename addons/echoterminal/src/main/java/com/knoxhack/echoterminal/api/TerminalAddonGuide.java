package com.knoxhack.echoterminal.api;

import java.util.List;

public record TerminalAddonGuide(
        String label,
        int sortOrder,
        String stageLabel,
        String startHint,
        List<String> starterSteps,
        boolean mainline) {
    private static final TerminalAddonGuide EMPTY =
            new TerminalAddonGuide("", Integer.MAX_VALUE, "", "", List.of(), false);

    public TerminalAddonGuide {
        label = clean(label, "");
        stageLabel = clean(stageLabel, "");
        startHint = clean(startHint, "");
        starterSteps = List.copyOf(starterSteps == null
                ? List.of()
                : starterSteps.stream()
                        .filter(step -> step != null && !step.isBlank())
                        .map(String::strip)
                        .toList());
    }

    public static TerminalAddonGuide empty() {
        return EMPTY;
    }

    public static TerminalAddonGuide mainline(
            int chapterNumber, int sortOrder, String stageLabel, String startHint, List<String> starterSteps) {
        return new TerminalAddonGuide("Chapter " + chapterNumber, sortOrder, stageLabel, startHint, starterSteps, true);
    }

    public static TerminalAddonGuide optional(
            int sortOrder, String stageLabel, String startHint, List<String> starterSteps) {
        return new TerminalAddonGuide("Optional", sortOrder, stageLabel, startHint, starterSteps, false);
    }

    public boolean isEmpty() {
        return label.isBlank() && stageLabel.isBlank() && startHint.isBlank() && starterSteps.isEmpty();
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
