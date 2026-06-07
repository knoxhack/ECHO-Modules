package com.knoxhack.echo.packcore;

public enum EchoRepairPlanStatus {
    NO_REPAIR_NEEDED("no_repair_needed", false),
    REPAIRABLE("repairable", false),
    PARTIALLY_REPAIRABLE("partially_repairable", false),
    MANUAL_ACTION_REQUIRED("manual_action_required", false),
    UNSAFE("unsafe", true),
    BLOCKED("blocked", true),
    UNKNOWN("unknown", false);

    private final String serializedName;
    private final boolean blocking;

    EchoRepairPlanStatus(String serializedName, boolean blocking) {
        this.serializedName = serializedName;
        this.blocking = blocking;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean blocking() {
        return blocking;
    }
}
