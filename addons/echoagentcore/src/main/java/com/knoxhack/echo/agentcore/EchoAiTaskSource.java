package com.knoxhack.echo.agentcore;

public enum EchoAiTaskSource {
    DIAGNOSTIC("diagnostic"),
    REPAIR_PLAN("repair_plan"),
    MISSING_ASSET("missing_asset"),
    RELEASE_READINESS("release_readiness"),
    METADATA("metadata"),
    RUN_REPORT("run_report"),
    MANUAL("manual");

    private final String serializedName;

    EchoAiTaskSource(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
