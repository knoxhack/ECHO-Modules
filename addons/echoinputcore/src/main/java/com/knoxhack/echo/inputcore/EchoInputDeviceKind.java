package com.knoxhack.echo.inputcore;

public enum EchoInputDeviceKind {
    KEYBOARD("keyboard"),
    MOUSE("mouse"),
    GAMEPAD("gamepad"),
    TOUCH("touch"),
    VIRTUAL("virtual"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoInputDeviceKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
