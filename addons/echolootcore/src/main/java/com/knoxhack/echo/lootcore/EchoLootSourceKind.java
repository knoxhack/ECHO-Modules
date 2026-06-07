package com.knoxhack.echo.lootcore;

public enum EchoLootSourceKind {
    RELIC_DROP("relic_drop"),
    FACTION_LOOT("faction_loot"),
    POI_LOOT("poi_loot"),
    WEATHER_EVENT_LOOT("weather_event_loot"),
    MISSION_REWARD_POOL("mission_reward_pool"),
    STRUCTURE_LOOT("structure_loot"),
    CREATURE_LOOT("creature_loot"),
    BARTER_REWARD("barter_reward"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoLootSourceKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
