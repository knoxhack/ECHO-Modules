package com.knoxhack.echoterminal.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

public final class TerminalArchiveRegistry {
    public static final String MANUAL_SOURCE = "manual";

    private static final Map<Identifier, TerminalArchiveEntry> ENTRIES = new ConcurrentHashMap<>();
    private static final Map<Identifier, String> SOURCES = new ConcurrentHashMap<>();

    private TerminalArchiveRegistry() {
    }

    public static void register(TerminalArchiveEntry entry) {
        register(MANUAL_SOURCE, entry);
    }

    public static void register(String source, TerminalArchiveEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Terminal archive entry is required.");
        }
        String safeSource = source == null || source.isBlank() ? MANUAL_SOURCE : source.strip();
        ENTRIES.put(entry.id(), entry);
        SOURCES.put(entry.id(), safeSource);
    }

    public static void replaceSource(String source, Collection<TerminalArchiveEntry> entries) {
        String safeSource = source == null || source.isBlank() ? MANUAL_SOURCE : source.strip();
        unregisterSource(safeSource);
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (TerminalArchiveEntry entry : entries) {
            register(safeSource, entry);
        }
    }

    public static int unregisterSource(String source) {
        String safeSource = source == null || source.isBlank() ? MANUAL_SOURCE : source.strip();
        List<Identifier> removed = SOURCES.entrySet().stream()
                .filter(entry -> safeSource.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        removed.forEach(id -> {
            ENTRIES.remove(id);
            SOURCES.remove(id);
        });
        return removed.size();
    }

    public static Optional<String> sourceOf(Identifier id) {
        return Optional.ofNullable(id == null ? null : SOURCES.get(id));
    }

    public static List<TerminalArchiveEntry> entries() {
        List<TerminalArchiveEntry> entries = new ArrayList<>(ENTRIES.values());
        entries.sort(Comparator.comparing(TerminalArchiveEntry::group).thenComparing(TerminalArchiveEntry::title));
        return List.copyOf(entries);
    }

    public static void clearForTests() {
        ENTRIES.clear();
        SOURCES.clear();
    }
}
