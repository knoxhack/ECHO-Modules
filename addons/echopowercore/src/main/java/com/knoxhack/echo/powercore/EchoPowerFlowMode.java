package com.knoxhack.echo.powercore;

public enum EchoPowerFlowMode {
    PRODUCE("produce"),
    CONSUME("consume"),
    STORE("store"),
    TRANSFER("transfer"),
    BALANCE("balance"),
    DISABLED("disabled"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoPowerFlowMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
