package com.knoxhack.echo.difficultycore;

public enum EchoDifficultyMetricKind {
    HAZARD_INTENSITY("hazard_intensity"),
    LOOT_SCALING("loot_scaling"),
    COMBAT_SCALING("combat_scaling"),
    SURVIVAL_DRAIN_SCALING("survival_drain_scaling"),
    ENCOUNTER_DENSITY("encounter_density"),
    SPAWN_DENSITY("spawn_density"),
    BOSS_PRESSURE("boss_pressure"),
    RECOVERY_ASSISTANCE("recovery_assistance"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoDifficultyMetricKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
