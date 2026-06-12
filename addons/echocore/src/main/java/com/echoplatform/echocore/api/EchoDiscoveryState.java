package com.echoplatform.echocore.api;

public enum EchoDiscoveryState {
    LOCKED,
    DISCOVERED,
    CHECKED;

    public String displayName() {
        String lower = name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
