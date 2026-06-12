package com.echoplatform.echocore.api;

public enum WorldRegionType {
    CRASH_ZONE,
    CONVOY_ROUTE,
    RUINED_CITY,
    TOXIC_SWAMP,
    RADIATION_ZONE,
    CRYOGENIC_RUINS,
    NEXUS_SCAR,
    ANOMALY_ZONE,
    ORBITAL_DEBRIS_FIELD,
    SECURE_OUTPOST,
    CUSTOM;

    public String displayName() {
        String raw = name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
