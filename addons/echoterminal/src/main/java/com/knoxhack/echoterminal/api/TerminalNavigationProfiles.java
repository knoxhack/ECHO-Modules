package com.knoxhack.echoterminal.api;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

public final class TerminalNavigationProfiles {
    private static final Map<Identifier, TerminalNavigationProfile> PROFILES = new ConcurrentHashMap<>();

    private TerminalNavigationProfiles() {
    }

    public static void register(Identifier tabId, TerminalNavigationProfile profile) {
        if (tabId == null || profile == null) {
            throw new IllegalArgumentException("Terminal navigation tab id and profile are required.");
        }
        PROFILES.put(tabId, profile);
    }

    public static Optional<TerminalNavigationProfile> profile(Identifier tabId) {
        return Optional.ofNullable(tabId == null ? null : PROFILES.get(tabId));
    }

    public static TerminalNavigationProfile profileFor(TerminalTab tab) {
        if (tab == null || tab.descriptor() == null) {
            return TerminalNavigationProfile.command(0);
        }
        TerminalNavigationProfile registered = PROFILES.get(tab.descriptor().id());
        return registered == null ? fallbackFor(tab) : registered;
    }

    public static boolean referenceOnlyChapterContext(TerminalRenderContext context) {
        Identifier tabId = context == null || context.themeContext() == null
                ? null
                : context.themeContext().activeTabId();
        TerminalNavigationProfile profile = tabId == null ? null : PROFILES.get(tabId);
        return profile != null && profile.hasChapter();
    }

    public static void clearForTests() {
        PROFILES.clear();
    }

    public static void withClearedForTests(Runnable runnable) {
        Map<Identifier, TerminalNavigationProfile> snapshot = Map.copyOf(PROFILES);
        PROFILES.clear();
        try {
            runnable.run();
        } finally {
            PROFILES.clear();
            PROFILES.putAll(snapshot);
        }
    }

    private static TerminalNavigationProfile fallbackFor(TerminalTab tab) {
        TerminalTabChrome chrome = tab.chrome();
        int order = tab.descriptor().order();
        String group = chrome == null ? "" : chrome.group();
        if (TerminalTabChrome.GROUP_PROTOCOL.equals(group)) {
            return TerminalNavigationProfile.command(order);
        }
        if (TerminalTabChrome.GROUP_SYSTEMS.equals(group)) {
            return TerminalNavigationProfile.system(order);
        }
        if (TerminalTabChrome.GROUP_CORE.equals(group)) {
            return TerminalNavigationProfile.index(order);
        }
        if (List.of(TerminalTabChrome.GROUP_FIELD,
                TerminalTabChrome.GROUP_ENDGAME, TerminalTabChrome.GROUP_NEXUS).contains(group)) {
            return TerminalNavigationProfile.intel(order);
        }
        if (TerminalTabChrome.GROUP_ORBITAL.equals(group)) {
            return TerminalNavigationProfile.holomap(order);
        }
        if (TerminalTabChrome.GROUP_ADDONS.equals(group)) {
            return TerminalNavigationProfile.progress(order);
        }
        return TerminalNavigationProfile.chapter(fallbackChapterId(group), fallbackChapterTitle(group), "", order);
    }

    private static String fallbackChapterId(String group) {
        String cleaned = group == null ? "" : group.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_./:-]", "_");
        return cleaned.isBlank() ? "addons" : cleaned;
    }

    private static String fallbackChapterTitle(String group) {
        String cleaned = group == null ? "" : group.strip();
        return cleaned.isBlank() ? "Addons" : cleaned;
    }
}
