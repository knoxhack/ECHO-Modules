package com.knoxhack.echo.progressioncore;

public enum EchoObjectiveScope {
    PLAYER("player"),
    TEAM("team"),
    SERVER("server"),
    WORLD("world"),
    PACK_PROFILE("pack_profile"),
    GAME_MODE("game_mode"),
    DEV("dev"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoObjectiveScope(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean shared() {
        return this == TEAM || this == SERVER || this == WORLD || this == PACK_PROFILE || this == GAME_MODE;
    }
}
