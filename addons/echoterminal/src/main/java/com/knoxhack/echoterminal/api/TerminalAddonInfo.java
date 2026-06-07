package com.knoxhack.echoterminal.api;

import java.util.List;

public record TerminalAddonInfo(
        String summary,
        List<TerminalAddonMetric> metrics,
        List<TerminalAddonSection> sections,
        List<TerminalAddonLink> links,
        TerminalAddonGuide guide) {
    private static final TerminalAddonInfo EMPTY =
            new TerminalAddonInfo("", List.of(), List.of(), List.of(), TerminalAddonGuide.empty());

    public TerminalAddonInfo(
            String summary,
            List<TerminalAddonMetric> metrics,
            List<TerminalAddonSection> sections,
            List<TerminalAddonLink> links) {
        this(summary, metrics, sections, links, TerminalAddonGuide.empty());
    }

    public TerminalAddonInfo {
        summary = clean(summary, "");
        metrics = List.copyOf(metrics == null
                ? List.of()
                : metrics.stream().filter(metric -> metric != null).toList());
        sections = List.copyOf(sections == null
                ? List.of()
                : sections.stream().filter(section -> section != null).toList());
        links = List.copyOf(links == null
                ? List.of()
                : links.stream().filter(link -> link != null).toList());
        guide = guide == null ? TerminalAddonGuide.empty() : guide;
    }

    public static TerminalAddonInfo empty() {
        return EMPTY;
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
