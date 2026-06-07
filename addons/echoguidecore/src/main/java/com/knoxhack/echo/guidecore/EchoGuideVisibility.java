package com.knoxhack.echo.guidecore;

public enum EchoGuideVisibility {
    ALWAYS("always"),
    UNLOCKED("unlocked"),
    DISCOVERED("discovered"),
    HIDDEN("hidden"),
    DEBUG("debug"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoGuideVisibility(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean searchableByDefault() {
        return this == ALWAYS || this == UNLOCKED || this == DISCOVERED;
    }
}
