package com.knoxhack.echo.agentcore;

public enum EchoAiTaskPriority {
    LOW("low", 10),
    NORMAL("normal", 20),
    HIGH("high", 30),
    CRITICAL("critical", 40);

    private final String serializedName;
    private final int rank;

    EchoAiTaskPriority(String serializedName, int rank) {
        this.serializedName = serializedName;
        this.rank = rank;
    }

    public String serializedName() {
        return serializedName;
    }

    public int rank() {
        return rank;
    }
}
