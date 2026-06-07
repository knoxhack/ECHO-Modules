package com.knoxhack.echo.inputcore;

public enum EchoInputActionKind {
    TERMINAL_SHORTCUT("terminal_shortcut"),
    LENS_SCAN("lens_scan"),
    HOLOMAP_OPEN("holomap_open"),
    VEHICLE_CONTROL("vehicle_control"),
    COMBAT_CONTROL("combat_control"),
    RADIAL_MENU("radial_menu"),
    SCREEN_ACTION("screen_action"),
    CREATOR_TOOL("creator_tool"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoInputActionKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
