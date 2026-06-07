package com.knoxhack.echo.economycore;

public enum EchoCurrencyKind {
    ITEM_BACKED("item_backed"),
    VIRTUAL("virtual"),
    FACTION_CREDIT("faction_credit"),
    REPUTATION("reputation"),
    TOKEN("token"),
    BARTER_VALUE("barter_value"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoCurrencyKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
