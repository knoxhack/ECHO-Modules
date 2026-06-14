package com.knoxhack.echo.settlementcore.api;

import java.util.Locale;

/**
 * Canonical settlement NPC job types.
 */
public enum JobType {
    DIVER,
    ENGINEER,
    MEDIC,
    CARTOGRAPHER,
    DEEP_MINER,
    PRESSURE_MECHANIC,
    XENO_BIOLOGIST;

    private final String key = name().toLowerCase(Locale.ROOT);

    public String getSerializedName() {
        return key;
    }
}
