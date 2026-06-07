package com.knoxhack.echo.agentcore;

public enum EchoAiCommandEnvironmentPolicy {
    READ_ONLY("read_only"),
    WORKSPACE_ONLY("workspace_only"),
    LOCAL_ONLY("local_only"),
    NETWORK_DENIED("network_denied"),
    CONFIRM_NETWORK("confirm_network"),
    CONFIRM_WRITE("confirm_write"),
    BLOCKED("blocked");

    private final String serializedName;

    EchoAiCommandEnvironmentPolicy(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean requiresConfirmation() {
        return this == CONFIRM_NETWORK || this == CONFIRM_WRITE || this == BLOCKED;
    }
}
