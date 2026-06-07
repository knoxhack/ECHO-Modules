package com.knoxhack.echotutorialcore.api;

public enum TutorialCategory {
    START_HERE,
    SURVIVAL,
    TERMINAL,
    SCANNER,
    HOLOMAP,
    LENS,
    POWER,
    MACHINES,
    WATER,
    HAZARDS,
    FACTIONS,
    RESEARCH,
    DRONES,
    COMBAT,
    NEXUS,
    ROUTE_CHAPTERS,
    TROUBLESHOOTING,
    ADVANCED,
    ADDONS;

    public String getTranslationKey() {
        return "echotutorialcore.category." + name().toLowerCase();
    }
}
