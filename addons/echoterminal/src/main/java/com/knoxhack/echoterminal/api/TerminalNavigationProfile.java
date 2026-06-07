package com.knoxhack.echoterminal.api;

import java.util.Locale;

/**
 * Navigation metadata layered over terminal tab rendering.
 */
public record TerminalNavigationProfile(
        TerminalNavigationSection section,
        String chapterId,
        String chapterTitle,
        String chapterIcon,
        int order) {
    public TerminalNavigationProfile {
        section = section == null ? TerminalNavigationSection.CHAPTERS : section.canonical();
        chapterId = clean(chapterId);
        chapterTitle = clean(chapterTitle);
        chapterIcon = clean(chapterIcon).toUpperCase(Locale.ROOT);
    }

    public static TerminalNavigationProfile command(int order) {
        return section(TerminalNavigationSection.COMMAND, order);
    }

    public static TerminalNavigationProfile progress(int order) {
        return section(TerminalNavigationSection.CHAPTERS, order);
    }

    public static TerminalNavigationProfile intel(int order) {
        return section(TerminalNavigationSection.INTEL, order);
    }

    public static TerminalNavigationProfile index(int order) {
        return section(TerminalNavigationSection.INDEX, order);
    }

    public static TerminalNavigationProfile holomap(int order) {
        return section(TerminalNavigationSection.HOLOMAP, order);
    }

    public static TerminalNavigationProfile system(int order) {
        return section(TerminalNavigationSection.SYSTEM, order);
    }

    public static TerminalNavigationProfile terminal(int order) {
        return command(order);
    }

    public static TerminalNavigationProfile core(int order) {
        return intel(order);
    }

    public static TerminalNavigationProfile chaptersHub(int order) {
        return progress(order);
    }

    public static TerminalNavigationProfile section(TerminalNavigationSection section, int order) {
        return new TerminalNavigationProfile(section, "", "", "", order);
    }

    public static TerminalNavigationProfile chapter(String chapterId, String chapterTitle, String chapterIcon, int order) {
        return new TerminalNavigationProfile(
                TerminalNavigationSection.CHAPTERS, chapterId, chapterTitle, chapterIcon, order);
    }

    public boolean hasChapter() {
        return !chapterId.isBlank();
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
