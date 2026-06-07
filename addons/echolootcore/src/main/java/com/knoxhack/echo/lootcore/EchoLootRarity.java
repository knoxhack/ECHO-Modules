package com.knoxhack.echo.lootcore;

public enum EchoLootRarity {
    COMMON("common"),
    UNCOMMON("uncommon"),
    RARE("rare"),
    EPIC("epic"),
    LEGENDARY("legendary"),
    RELIC("relic"),
    STORY("story"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoLootRarity(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
