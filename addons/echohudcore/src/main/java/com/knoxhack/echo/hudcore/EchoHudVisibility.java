package com.knoxhack.echo.hudcore;

public enum EchoHudVisibility {
    ALWAYS("always", true),
    CONTEXTUAL("contextual", true),
    UNLOCKED_ONLY("unlocked_only", false),
    HIDDEN_BY_DEFAULT("hidden_by_default", false),
    DEBUG_ONLY("debug_only", false),
    DISABLED("disabled", false),
    UNKNOWN("unknown", false);

    private final String serializedName;
    private final boolean visibleByDefault;

    EchoHudVisibility(String serializedName, boolean visibleByDefault) {
        this.serializedName = serializedName;
        this.visibleByDefault = visibleByDefault;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean visibleByDefault() {
        return visibleByDefault;
    }
}
