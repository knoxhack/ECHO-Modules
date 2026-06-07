package com.knoxhack.echoorbitalremnants.progression;

public enum RocketTier {
    EMERGENCY_ROCKET("Emergency Rocket", 1, true),
    ORBITAL_SHUTTLE("Orbital Shuttle", 2, false),
    NEXUS_DRIVE_VESSEL("Nexus Drive Vessel", 3, false);

    private final String displayName;
    private final int tier;
    private final boolean salvaged;

    RocketTier(String displayName, int tier, boolean salvaged) {
        this.displayName = displayName;
        this.tier = tier;
        this.salvaged = salvaged;
    }

    public String displayName() {
        return displayName;
    }

    public int tier() {
        return tier;
    }

    public boolean isSalvaged() {
        return salvaged;
    }
}
