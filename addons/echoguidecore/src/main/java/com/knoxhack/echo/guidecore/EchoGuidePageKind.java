package com.knoxhack.echo.guidecore;

public enum EchoGuidePageKind {
    MANUAL_PAGE("manual_page"),
    SEARCHABLE_PAGE("searchable_page"),
    RECIPE_HELP("recipe_help"),
    MISSION_HELP("mission_help"),
    MACHINE_HELP("machine_help"),
    TUTORIAL("tutorial"),
    TROUBLESHOOTING("troubleshooting"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoGuidePageKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
