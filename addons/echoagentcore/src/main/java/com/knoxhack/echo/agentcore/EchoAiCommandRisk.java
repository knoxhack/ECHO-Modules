package com.knoxhack.echo.agentcore;

public enum EchoAiCommandRisk {
    INFORMATIONAL("informational", false),
    LOW("low", false),
    MEDIUM("medium", true),
    HIGH("high", true),
    DESTRUCTIVE("destructive", true),
    PRIVILEGED("privileged", true);

    private final String serializedName;
    private final boolean requiresConfirmation;

    EchoAiCommandRisk(String serializedName, boolean requiresConfirmation) {
        this.serializedName = serializedName;
        this.requiresConfirmation = requiresConfirmation;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean requiresConfirmation() {
        return requiresConfirmation;
    }
}
