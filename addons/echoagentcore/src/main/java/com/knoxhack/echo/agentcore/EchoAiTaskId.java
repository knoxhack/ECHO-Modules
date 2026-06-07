package com.knoxhack.echo.agentcore;

import java.util.Locale;

public record EchoAiTaskId(String value) {
    public EchoAiTaskId {
        value = AgentContractGuards.requireText(value, "AI task id").toLowerCase(Locale.ROOT);
    }

    public static EchoAiTaskId of(String value) {
        return new EchoAiTaskId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
