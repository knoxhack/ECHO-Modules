package com.knoxhack.echotutorialcore.api;

public enum TutorialHintType {
    INFO,
    WARNING,
    DANGER,
    BLOCKED,
    MISSING_ITEM,
    PROGRESSION,
    SYSTEM_HELP,
    MISSION_HELP,
    RECIPE_HELP,
    HAZARD_HELP,
    COMBAT_HELP,
    MACHINE_HELP,
    POWER_HELP,
    ROUTE_HELP;

    public String getTranslationKey() {
        return "echotutorialcore.hint_type." + name().toLowerCase();
    }
}
