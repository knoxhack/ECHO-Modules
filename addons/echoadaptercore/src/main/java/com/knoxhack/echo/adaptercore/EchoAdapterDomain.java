package com.knoxhack.echo.adaptercore;

import java.util.Locale;
import java.util.Optional;

public enum EchoAdapterDomain {
    BLOCKS("blocks"),
    ITEMS("items"),
    ENTITIES("entities"),
    RECIPES("recipes"),
    LOOT("loot"),
    STRUCTURES("structures"),
    UI_SCREENS("ui_screens"),
    SOUNDS("sounds"),
    MISSIONS("missions"),
    SAVES("saves"),
    WORLDGEN("worldgen"),
    NETWORKING("networking"),
    COMMANDS("commands"),
    DIAGNOSTICS("diagnostics"),
    MAPS("maps"),
    PACKS("packs"),
    THEMES("themes"),
    WIKI("wiki"),
    ASSETS("assets"),
    DATA("data"),
    RENDERING("rendering"),
    INPUT("input"),
    PLAYER("player"),
    WEATHER("weather"),
    HAZARDS("hazards"),
    MACHINES("machines"),
    POWER("power"),
    ECONOMY("economy"),
    STORY("story");

    private final String id;

    EchoAdapterDomain(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<EchoAdapterDomain> fromId(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (EchoAdapterDomain domain : values()) {
            if (domain.id.equals(normalized) || domain.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(domain);
            }
        }
        return Optional.empty();
    }
}
