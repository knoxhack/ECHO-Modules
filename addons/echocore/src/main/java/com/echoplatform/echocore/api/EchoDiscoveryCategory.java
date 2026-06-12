package com.echoplatform.echocore.api;

public enum EchoDiscoveryCategory {
    STRUCTURE,
    REGION,
    BIOME,
    GUARDIAN,
    EVENT,
    FACTION;

    public String displayName() {
        String lower = name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
