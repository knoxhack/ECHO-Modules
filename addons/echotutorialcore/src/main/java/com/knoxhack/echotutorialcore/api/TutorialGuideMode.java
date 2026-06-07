package com.knoxhack.echotutorialcore.api;

public enum TutorialGuideMode {
    OFF,
    MINIMAL,
    NORMAL,
    ASSISTED;

    public String getTranslationKey() {
        return "echotutorialcore.guide_mode." + name().toLowerCase();
    }

    public static TutorialGuideMode byName(String name) {
        for (TutorialGuideMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return NORMAL;
    }
}
