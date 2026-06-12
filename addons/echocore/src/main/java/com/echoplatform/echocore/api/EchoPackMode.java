package com.echoplatform.echocore.api;

public enum EchoPackMode {
    BASELINE("ECHO Modules", "Baseline runtime"),
    ASHFALL("Ashfall Protocol", "Ashfall field runtime"),
    ORBITAL_STANDALONE("Orbital Remnants", "Orbital standalone runtime");

    private final String displayName;
    private final String statusLine;

    EchoPackMode(String displayName, String statusLine) {
        this.displayName = displayName;
        this.statusLine = statusLine;
    }

    public String displayName() {
        return displayName;
    }

    public String statusLine() {
        return statusLine;
    }
}
