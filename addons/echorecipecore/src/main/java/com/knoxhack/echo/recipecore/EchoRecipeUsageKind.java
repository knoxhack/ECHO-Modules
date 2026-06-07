package com.knoxhack.echo.recipecore;

public enum EchoRecipeUsageKind {
    INPUT("input"),
    OUTPUT("output"),
    CATALYST("catalyst"),
    MACHINE("machine"),
    TOOL("tool"),
    BYPRODUCT("byproduct"),
    UNLOCK("unlock"),
    DISPLAY("display"),
    SOURCE("source"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoRecipeUsageKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
