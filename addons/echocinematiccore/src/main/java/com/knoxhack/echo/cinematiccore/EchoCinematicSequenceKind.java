package com.knoxhack.echo.cinematiccore;

public enum EchoCinematicSequenceKind {
    INTRO("intro"),
    MISSION_STINGER("mission_stinger"),
    BOSS_INTRO("boss_intro"),
    DROP_POD_LANDING("drop_pod_landing"),
    TERMINAL_BOOT("terminal_boot"),
    CUTSCENE("cutscene"),
    SCREENSHOT_MODE("screenshot_mode"),
    CINEMATIC_MODE("cinematic_mode"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoCinematicSequenceKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
