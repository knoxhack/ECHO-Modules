package com.knoxhack.echoterminal.api;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Vanilla-safe geometric icons for terminal navigation and action cards.
 */
public enum TerminalIcon {
    CORE,
    FIELD,
    SYSTEMS,
    ENDGAME,
    ADDONS,
    OVERVIEW,
    MISSIONS,
    STATUS,
    DRONE,
    NEXUS,
    ORBITAL,
    ARCHIVES,
    CODEX,
    WORLD,
    VANILLA,
    SEARCH,
    SETTINGS,
    CHECK,
    LOCK,
    TARGET,
    CLOCK,
    DEFAULT;

    public static TerminalIcon fromGroup(String group) {
        if (TerminalNavigationSection.COMMAND.key().equals(group)) {
            return OVERVIEW;
        }
        if (TerminalNavigationSection.INTEL.key().equals(group)) {
            return FIELD;
        }
        if (TerminalNavigationSection.INDEX.key().equals(group)) {
            return CODEX;
        }
        if (TerminalNavigationSection.HOLOMAP.key().equals(group)) {
            return WORLD;
        }
        if (TerminalNavigationSection.SYSTEM.key().equals(group)) {
            return SYSTEMS;
        }
        if (TerminalNavigationSection.CHAPTERS.key().equals(group)) {
            return ADDONS;
        }
        if (TerminalNavigationSection.TERMINAL.key().equals(group)) {
            return OVERVIEW;
        }
        if (TerminalNavigationSection.CORE.key().equals(group)) {
            return FIELD;
        }
        if (TerminalTabChrome.GROUP_PROTOCOL.equals(group)) {
            return CORE;
        }
        if (TerminalTabChrome.GROUP_CORE.equals(group)) {
            return CORE;
        }
        if (TerminalTabChrome.GROUP_FIELD.equals(group)) {
            return FIELD;
        }
        if (TerminalTabChrome.GROUP_SYSTEMS.equals(group)) {
            return SYSTEMS;
        }
        if (TerminalTabChrome.GROUP_ENDGAME.equals(group)) {
            return ENDGAME;
        }
        if (TerminalTabChrome.GROUP_NEXUS.equals(group)) {
            return NEXUS;
        }
        if (TerminalTabChrome.GROUP_ORBITAL.equals(group)) {
            return ORBITAL;
        }
        if (TerminalTabChrome.GROUP_ADDONS.equals(group)) {
            return ADDONS;
        }
        return DEFAULT;
    }

    public static TerminalIcon fromTitle(String title) {
        String value = title == null ? "" : title.toLowerCase();
        if (value.contains("command deck") || value.contains("overview")) {
            return OVERVIEW;
        }
        if (value.contains("what now")) {
            return SEARCH;
        }
        if (value.contains("protocol roadmap") || value.contains("mission")) {
            return MISSIONS;
        }
        if (value.contains("signal lead")) {
            return MISSIONS;
        }
        if (value.contains("vitals") || value.contains("status")) {
            return STATUS;
        }
        if (value.contains("companion") || value.contains("drone")) {
            return DRONE;
        }
        if (value.contains("nexus")) {
            return NEXUS;
        }
        if (value.contains("echo-0") || value.contains("orbital") || value.contains("route survey")) {
            return ORBITAL;
        }
        if (value.contains("archive")) {
            return ARCHIVES;
        }
        if (value.contains("survival index") || value.contains("codex")) {
            return CODEX;
        }
        if (value.contains("route map") || value.contains("world")) {
            return WORLD;
        }
        if (value.contains("baseline") || value.contains("vanilla")) {
            return VANILLA;
        }
        if (value.contains("chapter guide") || value.contains("addon")) {
            return ADDONS;
        }
        if (value.contains("settings")) {
            return SETTINGS;
        }
        return DEFAULT;
    }

    public void draw(GuiGraphicsExtractor graphics, int x, int y, int size, int color, boolean active) {
        int c = opaque(color);
        int dim = active ? 0x6638DFF4 : 0x33244352;
        graphics.fill(x, y, x + size, y + size, 0x44071117);
        graphics.outline(x, y, size, size, active ? c : 0x885D7F92);
        int mid = x + size / 2;
        int cy = y + size / 2;
        switch (this) {
            case CORE, OVERVIEW -> {
                graphics.outline(x + 5, y + 5, size - 10, size - 10, c);
                graphics.fill(mid - 2, cy - 2, mid + 3, cy + 3, c);
            }
            case FIELD, WORLD -> {
                graphics.fill(x + 5, y + 6, x + 7, y + size - 5, c);
                graphics.fill(mid - 1, y + 4, mid + 1, y + size - 7, c);
                graphics.fill(x + size - 7, y + 7, x + size - 5, y + size - 4, c);
                graphics.fill(x + 7, y + 6, mid - 1, y + 9, dim);
                graphics.fill(mid + 1, y + size - 10, x + size - 7, y + size - 7, dim);
            }
            case SYSTEMS -> {
                graphics.outline(x + 6, y + 6, size - 12, size - 12, c);
                graphics.fill(mid - 1, y + 3, mid + 1, y + 8, c);
                graphics.fill(mid - 1, y + size - 8, mid + 1, y + size - 3, c);
                graphics.fill(x + 3, cy - 1, x + 8, cy + 1, c);
                graphics.fill(x + size - 8, cy - 1, x + size - 3, cy + 1, c);
            }
            case ENDGAME -> {
                graphics.fill(mid - 1, y + 4, mid + 1, y + size - 4, c);
                graphics.fill(x + 4, cy - 1, x + size - 4, cy + 1, c);
                graphics.fill(mid - 4, cy - 4, mid + 5, cy + 5, dim);
            }
            case ADDONS, ARCHIVES -> {
                graphics.outline(x + 5, y + 5, size / 2, size / 2, c);
                graphics.outline(mid - 1, y + 5, size / 2, size / 2, c);
                graphics.outline(x + 5, mid - 1, size / 2, size / 2, c);
            }
            case MISSIONS -> {
                graphics.fill(x + 6, y + 5, x + 8, y + size - 5, c);
                graphics.fill(x + 8, y + 5, x + size - 6, y + 8, c);
                graphics.fill(x + 8, y + 8, x + size - 9, y + 11, dim);
            }
            case STATUS -> {
                graphics.fill(x + 5, cy, x + 10, cy, c);
                graphics.fill(x + 10, cy, x + 14, cy - 6, c);
                graphics.fill(x + 14, cy - 6, x + 20, cy + 6, c);
                graphics.fill(x + 20, cy + 6, x + size - 5, cy + 6, c);
            }
            case DRONE -> {
                graphics.fill(mid - 5, cy - 3, mid + 6, cy + 4, c);
                graphics.outline(x + 4, y + 5, 8, 8, c);
                graphics.outline(x + size - 12, y + 5, 8, 8, c);
                graphics.outline(x + 4, y + size - 13, 8, 8, c);
                graphics.outline(x + size - 12, y + size - 13, 8, 8, c);
            }
            case NEXUS -> {
                graphics.outline(x + 6, y + 6, size - 12, size - 12, c);
                graphics.fill(mid - 1, y + 7, mid + 1, y + size - 7, c);
                graphics.fill(x + 7, cy - 1, x + size - 7, cy + 1, c);
            }
            case ORBITAL, CODEX, VANILLA -> {
                graphics.outline(x + 5, y + 5, size - 10, size - 10, c);
                graphics.fill(mid - 2, cy - 2, mid + 3, cy + 3, c);
                graphics.fill(x + 4, cy - 1, x + size - 4, cy + 1, dim);
            }
            case SEARCH -> {
                graphics.outline(x + 5, y + 5, size - 12, size - 12, c);
                graphics.fill(x + size - 9, y + size - 8, x + size - 4, y + size - 6, c);
            }
            case SETTINGS -> {
                graphics.outline(x + 6, y + 6, size - 12, size - 12, c);
                graphics.fill(mid - 2, cy - 2, mid + 3, cy + 3, c);
            }
            case CHECK -> {
                graphics.fill(x + 4, cy, x + 7, cy + 3, c);
                graphics.fill(x + 7, cy + 3, x + size - 6, cy - (size / 2) + 4, c);
                graphics.fill(x + 7, cy + 2, x + size - 6, cy - (size / 2) + 5, c);
            }
            case LOCK -> {
                graphics.fill(x + 5, cy - 1, x + size - 5, cy + 5, c);
                graphics.outline(x + 5, cy - 1, size - 10, 6, c);
                graphics.fill(mid - 2, cy + 2, mid + 3, cy + 5, dim);
            }
            case TARGET -> {
                graphics.fill(mid - 1, y + 4, mid + 1, y + size - 4, c);
                graphics.fill(x + 4, cy - 1, x + size - 4, cy + 1, c);
                graphics.outline(mid - 3, cy - 3, 7, 7, c);
            }
            case CLOCK -> {
                graphics.outline(x + 5, y + 5, size - 10, size - 10, c);
                graphics.fill(mid - 1, cy - 4, mid + 1, cy + 1, c);
                graphics.fill(mid - 1, cy, mid + 4, cy + 2, c);
            }
            default -> {
                graphics.outline(x + 6, y + 6, size - 12, size - 12, c);
                graphics.fill(x + 9, cy - 1, x + size - 9, cy + 1, c);
            }
        }
    }

    private static int opaque(int color) {
        return color | 0xFF000000;
    }
}
