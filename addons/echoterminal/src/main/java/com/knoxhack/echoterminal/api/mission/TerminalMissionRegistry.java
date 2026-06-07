package com.knoxhack.echoterminal.api.mission;

import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.TerminalApiIds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

public final class TerminalMissionRegistry {
    private static final Map<Identifier, TerminalMissionProvider> PROVIDERS = new ConcurrentHashMap<>();
    private static volatile List<TerminalMissionProvider> sortedProviders = List.of();

    private TerminalMissionRegistry() {
    }

    public static void register(TerminalMissionProvider provider) {
        Optional<TerminalMissionChapter> chapter = safeChapter(provider, "register");
        if (chapter.isEmpty()) {
            EchoTerminal.LOGGER.warn("Terminal mission provider {} did not expose a valid chapter; ignoring provider.",
                    providerName(provider));
            return;
        }
        Identifier id = TerminalApiIds.requireLowercase(chapter.get().id(), "Terminal mission provider");
        TerminalMissionProvider previous = PROVIDERS.putIfAbsent(id, provider);
        if (previous != null && previous != provider) {
            throw new IllegalArgumentException("Duplicate terminal mission provider id: " + id);
        }
        ensureSorted();
    }

    public static Optional<TerminalMissionProvider> provider(Identifier chapterId) {
        if (chapterId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(PROVIDERS.get(chapterId));
    }

    public static List<TerminalMissionProvider> providers() {
        return sortedProviders;
    }

    public static void ensureSorted() {
        List<TerminalMissionProvider> providers = new ArrayList<>(PROVIDERS.values());
        providers.removeIf(provider -> safeChapter(provider, "sort").isEmpty());
        providers.sort(Comparator
                .comparingInt((TerminalMissionProvider provider) -> safeChapter(provider, "sort")
                        .map(TerminalMissionChapter::order)
                        .orElse(Integer.MAX_VALUE))
                .thenComparing(provider -> safeChapter(provider, "sort")
                        .map(chapter -> chapter.id().toString())
                        .orElse("")));
        sortedProviders = List.copyOf(providers);
    }

    public static void clearForTests() {
        PROVIDERS.clear();
        sortedProviders = List.of();
    }

    public static void withClearedForTests(Runnable runnable) {
        Map<Identifier, TerminalMissionProvider> snapshot = Map.copyOf(PROVIDERS);
        List<TerminalMissionProvider> sortedSnapshot = sortedProviders;
        PROVIDERS.clear();
        sortedProviders = List.of();
        try {
            runnable.run();
        } finally {
            PROVIDERS.clear();
            PROVIDERS.putAll(snapshot);
            sortedProviders = sortedSnapshot;
        }
    }

    private static Optional<TerminalMissionChapter> safeChapter(TerminalMissionProvider provider, String surface) {
        if (provider == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(provider.chapter());
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal mission provider {} failed during {}; ignoring provider output.",
                    providerName(provider), surface, exception);
            return Optional.empty();
        }
    }

    private static String providerName(Object provider) {
        return provider == null ? "<null>" : provider.getClass().getName();
    }
}
