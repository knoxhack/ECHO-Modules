package com.knoxhack.echo.eventcore;

public enum EchoWorldEventType {
    WEATHER_EVENT("weather_event"),
    FACTION_RAID("faction_raid"),
    SUPPLY_DROP("supply_drop"),
    CONVOY_AMBUSH("convoy_ambush"),
    NEXUS_STORM("nexus_storm"),
    ARCANA_ANOMALY("arcana_anomaly"),
    SERVER_EVENT("server_event"),
    REGION_EVENT("region_event"),
    POI_EVENT("poi_event"),
    STRUCTURE_EVENT("structure_event"),
    BOSS_EVENT("boss_event"),
    RECOVERY_EVENT("recovery_event"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoWorldEventType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
