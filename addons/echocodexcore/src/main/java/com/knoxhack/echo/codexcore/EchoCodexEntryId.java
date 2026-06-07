package com.knoxhack.echo.codexcore;

public record EchoCodexEntryId(String value) {
    public EchoCodexEntryId {
        value = CodexContractGuards.id(value, "codex entry id");
    }

    public static EchoCodexEntryId of(String value) {
        return new EchoCodexEntryId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
