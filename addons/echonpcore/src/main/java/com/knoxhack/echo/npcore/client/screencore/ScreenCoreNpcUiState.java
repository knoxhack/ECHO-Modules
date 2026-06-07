package com.knoxhack.echo.npcore.client.screencore;

import com.knoxhack.echo.npcore.network.EchoNpcScreenState;
import java.util.Locale;

final class ScreenCoreNpcUiState {
    private static volatile EchoNpcScreenState state;
    private static volatile String selectedTab = "talk";
    private static volatile boolean active;

    private ScreenCoreNpcUiState() {
    }

    static void open(EchoNpcScreenState next) {
        active = true;
        update(next);
    }

    static void update(EchoNpcScreenState next) {
        state = next;
        String tab = clean(next == null ? "" : next.currentTab());
        if (validTab(tab)) {
            selectedTab = tab;
        } else if (selectedTab.isBlank()) {
            selectedTab = "talk";
        }
    }

    static void clear() {
        state = null;
        selectedTab = "talk";
        active = false;
    }

    static EchoNpcScreenState state() {
        return state;
    }

    static boolean activeFor(EchoNpcScreenState next) {
        EchoNpcScreenState current = state;
        return active && current != null && next != null && current.entityId() == next.entityId();
    }

    static String selectedTab() {
        return selectedTab.isBlank() ? "talk" : selectedTab;
    }

    static void selectedTab(String tab) {
        String clean = clean(tab);
        selectedTab = validTab(clean) ? clean : "talk";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean validTab(String tab) {
        return switch (tab) {
            case "talk", "trade", "services", "intel" -> true;
            default -> false;
        };
    }
}
