package com.knoxhack.echo.questdirector;

public record EchoQuestDirectorId(String value) {
    public EchoQuestDirectorId {
        value = QuestDirectorContractGuards.id(value, "quest director id");
    }

    public static EchoQuestDirectorId of(String value) {
        return new EchoQuestDirectorId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
