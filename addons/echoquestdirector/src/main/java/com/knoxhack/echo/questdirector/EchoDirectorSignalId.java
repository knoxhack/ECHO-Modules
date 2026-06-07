package com.knoxhack.echo.questdirector;

public record EchoDirectorSignalId(String value) {
    public EchoDirectorSignalId {
        value = QuestDirectorContractGuards.id(value, "director signal id");
    }

    public static EchoDirectorSignalId of(String value) {
        return new EchoDirectorSignalId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
