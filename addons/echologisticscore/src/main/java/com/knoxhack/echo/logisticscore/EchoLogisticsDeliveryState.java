package com.knoxhack.echo.logisticscore;

public enum EchoLogisticsDeliveryState {
    QUEUED("queued"),
    RESERVED("reserved"),
    IN_TRANSIT("in_transit"),
    WAITING_FOR_ROUTE("waiting_for_route"),
    WAITING_FOR_STOCK("waiting_for_stock"),
    DELIVERED("delivered"),
    FAILED("failed"),
    CANCELED("canceled"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoLogisticsDeliveryState(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean terminal() {
        return this == DELIVERED || this == FAILED || this == CANCELED;
    }
}
