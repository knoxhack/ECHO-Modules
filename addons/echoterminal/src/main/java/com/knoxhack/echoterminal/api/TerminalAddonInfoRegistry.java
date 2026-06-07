package com.knoxhack.echoterminal.api;

import com.knoxhack.echoterminal.EchoTerminal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;

public final class TerminalAddonInfoRegistry {
    private static final Map<String, TerminalAddonInfoProvider> PROVIDERS = new ConcurrentHashMap<>();
    private static volatile List<TerminalAddonInfoProvider> sortedProviders = List.of();

    private TerminalAddonInfoRegistry() {
    }

    public static void register(TerminalAddonInfoProvider provider) {
        String chapterId = safeChapterId(provider, "register");
        if (chapterId == null || chapterId.isBlank()) {
            EchoTerminal.LOGGER.warn("Terminal addon info provider {} did not expose a valid chapter id; ignoring provider.",
                    providerName(provider));
            return;
        }
        String id = requireLowercase(chapterId, "Terminal addon info provider");
        TerminalAddonInfoProvider previous = PROVIDERS.putIfAbsent(id, provider);
        if (previous != null && previous != provider) {
            throw new IllegalArgumentException("Duplicate terminal addon info provider id: " + id);
        }
        ensureSorted();
    }

    public static Optional<TerminalAddonInfoProvider> provider(String chapterId) {
        if (chapterId == null || chapterId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(PROVIDERS.get(chapterId.strip().toLowerCase(Locale.ROOT)));
    }

    public static TerminalAddonInfo info(String chapterId, Player player) {
        return provider(chapterId)
                .map(provider -> safeInfo(provider, player))
                .orElse(TerminalAddonInfo.empty());
    }

    public static List<TerminalAddonInfoProvider> providers() {
        return sortedProviders;
    }

    public static void ensureSorted() {
        List<TerminalAddonInfoProvider> providers = new ArrayList<>(PROVIDERS.values());
        providers.removeIf(provider -> safeChapterId(provider, "sort") == null);
        providers.sort(Comparator.comparing(provider -> {
            String id = safeChapterId(provider, "sort");
            return id == null ? "" : id;
        }));
        sortedProviders = List.copyOf(providers);
    }

    public static void clearForTests() {
        PROVIDERS.clear();
        sortedProviders = List.of();
    }

    public static void withClearedForTests(Runnable runnable) {
        Map<String, TerminalAddonInfoProvider> snapshot = Map.copyOf(PROVIDERS);
        List<TerminalAddonInfoProvider> sortedSnapshot = sortedProviders;
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

    private static TerminalAddonInfo safeInfo(TerminalAddonInfoProvider provider, Player player) {
        try {
            TerminalAddonInfo info = provider.info(player);
            return info == null ? TerminalAddonInfo.empty() : info;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal addon info provider {} failed while building info; using empty info.",
                    providerName(provider), exception);
            return TerminalAddonInfo.empty();
        }
    }

    private static String safeChapterId(TerminalAddonInfoProvider provider, String surface) {
        if (provider == null) {
            return null;
        }
        try {
            String id = provider.chapterId();
            return id == null || id.isBlank() ? null : id.strip();
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal addon info provider {} failed during {}; ignoring provider output.",
                    providerName(provider), surface, exception);
            return null;
        }
    }

    private static String requireLowercase(String chapterId, String label) {
        String id = chapterId == null ? "" : chapterId.strip();
        if (id.isBlank()) {
            throw new IllegalArgumentException(label + " chapter id is required.");
        }
        String lowercase = id.toLowerCase(Locale.ROOT);
        if (!id.equals(lowercase)) {
            throw new IllegalArgumentException(label + " chapter id must be lowercase: " + id);
        }
        return id;
    }

    private static String providerName(Object provider) {
        return provider == null ? "<null>" : provider.getClass().getName();
    }
}
