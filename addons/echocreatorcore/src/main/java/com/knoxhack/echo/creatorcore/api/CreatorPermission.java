package com.knoxhack.echo.creatorcore.api;

public enum CreatorPermission {
    BLOCKED(0),
    VIEWER(1),
    OPERATOR(2),
    CREATOR(3),
    DEVELOPER(4);

    private final int rank;

    CreatorPermission(int rank) {
        this.rank = rank;
    }

    public boolean atLeast(CreatorPermission required) {
        return rank >= required.rank;
    }
}
