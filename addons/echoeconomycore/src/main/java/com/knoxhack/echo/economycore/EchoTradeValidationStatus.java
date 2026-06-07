package com.knoxhack.echo.economycore;

public enum EchoTradeValidationStatus {
    VALID("valid"),
    WARNING("warning"),
    BLOCKED("blocked"),
    MISSING_COST("missing_cost"),
    MISSING_OUTPUT("missing_output"),
    INSUFFICIENT_FUNDS("insufficient_funds"),
    REPUTATION_GATED("reputation_gated"),
    FACTION_LOCKED("faction_locked"),
    OUT_OF_STOCK("out_of_stock"),
    DUPLICATE_RISK("duplicate_risk"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoTradeValidationStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean blocking() {
        return this != VALID && this != WARNING && this != UNKNOWN;
    }
}
