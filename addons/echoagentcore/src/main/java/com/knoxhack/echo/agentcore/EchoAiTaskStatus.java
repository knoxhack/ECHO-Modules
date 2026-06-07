package com.knoxhack.echo.agentcore;

public enum EchoAiTaskStatus {
    PROPOSED("proposed"),
    READY("ready"),
    RUNNING("running"),
    BLOCKED("blocked"),
    FAILED("failed"),
    COMPLETED("completed"),
    NEEDS_REVIEW("needs_review"),
    SUPERSEDED("superseded");

    private final String serializedName;

    EchoAiTaskStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean terminal() {
        return this == FAILED || this == COMPLETED || this == SUPERSEDED;
    }
}
