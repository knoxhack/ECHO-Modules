package com.knoxhack.echo.socialcore;

public enum EchoDialogueNodeKind {
    GREETING("greeting"),
    STATEMENT("statement"),
    CHOICE_HUB("choice_hub"),
    BRANCH("branch"),
    SERVICE("service"),
    TRADE("trade"),
    MISSION_OFFER("mission_offer"),
    MISSION_TURN_IN("mission_turn_in"),
    EXIT("exit"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoDialogueNodeKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
