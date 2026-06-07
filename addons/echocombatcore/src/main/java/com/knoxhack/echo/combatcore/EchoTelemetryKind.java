package com.knoxhack.echo.combatcore;

public enum EchoTelemetryKind {
    DAMAGE_DEALT("damage_dealt"),
    DAMAGE_TAKEN("damage_taken"),
    BLOCKED("blocked"),
    SHIELDED("shielded"),
    EVADED("evaded"),
    CRITICAL_HIT("critical_hit"),
    STATUS_APPLIED("status_applied"),
    BOSS_PHASE_CHANGED("boss_phase_changed"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoTelemetryKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
