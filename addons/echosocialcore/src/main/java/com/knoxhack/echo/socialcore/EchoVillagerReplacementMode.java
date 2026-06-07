package com.knoxhack.echo.socialcore;

public enum EchoVillagerReplacementMode {
    DISABLED("disabled"),
    OPT_IN("opt_in"),
    PACK_PROFILE("pack_profile"),
    WORLD_RULE("world_rule"),
    DEV_TEST("dev_test"),
    CUSTOM("custom");

    private final String serializedName;

    EchoVillagerReplacementMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean canReplaceVillagers() {
        return this != DISABLED;
    }
}
