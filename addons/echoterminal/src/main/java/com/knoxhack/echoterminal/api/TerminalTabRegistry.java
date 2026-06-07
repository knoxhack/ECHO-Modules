package com.knoxhack.echoterminal.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

public final class TerminalTabRegistry {
    private static final Map<Identifier, TerminalTab> TABS = new ConcurrentHashMap<>();
    private static volatile List<TerminalTab> sortedTabs = List.of();

    private TerminalTabRegistry() {
    }

    public static void register(TerminalTab tab) {
        if (tab == null || tab.descriptor() == null) {
            throw new IllegalArgumentException("Terminal tab and descriptor are required.");
        }
        TABS.put(tab.descriptor().id(), tab);
        ensureSorted();
    }

    public static List<TerminalTab> tabs() {
        return sortedTabs;
    }

    public static void ensureSorted() {
        List<TerminalTab> tabs = new ArrayList<>(TABS.values());
        tabs.sort(Comparator
                .comparingInt((TerminalTab tab) -> tab.descriptor().order())
                .thenComparing(tab -> tab.descriptor().id().toString()));
        sortedTabs = List.copyOf(tabs);
    }

    public static void clearForTests() {
        TABS.clear();
        sortedTabs = List.of();
    }

    public static void withClearedForTests(Runnable runnable) {
        Map<Identifier, TerminalTab> snapshot = Map.copyOf(TABS);
        List<TerminalTab> sortedSnapshot = sortedTabs;
        TABS.clear();
        sortedTabs = List.of();
        try {
            runnable.run();
        } finally {
            TABS.clear();
            TABS.putAll(snapshot);
            sortedTabs = sortedSnapshot;
        }
    }
}
