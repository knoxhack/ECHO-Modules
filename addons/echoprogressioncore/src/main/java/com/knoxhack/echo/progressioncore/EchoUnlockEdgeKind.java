package com.knoxhack.echo.progressioncore;

public enum EchoUnlockEdgeKind {
    PREREQUISITE("prerequisite"),
    ALTERNATIVE("alternative"),
    RECOMMENDED_NEXT("recommended_next"),
    OPTIONAL_BRANCH("optional_branch"),
    BLOCKS("blocks"),
    REPLACES("replaces"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoUnlockEdgeKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
