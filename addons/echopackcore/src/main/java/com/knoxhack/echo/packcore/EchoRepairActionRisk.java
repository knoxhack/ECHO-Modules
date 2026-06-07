package com.knoxhack.echo.packcore;

public enum EchoRepairActionRisk {
    NONE("none", false),
    LOW("low", false),
    MEDIUM("medium", false),
    HIGH("high", true),
    DESTRUCTIVE("destructive", true),
    UNKNOWN("unknown", true);

    private final String serializedName;
    private final boolean requiresReview;

    EchoRepairActionRisk(String serializedName, boolean requiresReview) {
        this.serializedName = serializedName;
        this.requiresReview = requiresReview;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean requiresReview() {
        return requiresReview;
    }
}
