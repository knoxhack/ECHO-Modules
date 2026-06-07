package com.knoxhack.echo.socialcore;

import java.util.Locale;

public record EchoDialogueNodeId(String value) {
    public EchoDialogueNodeId {
        value = SocialContractGuards.requireText(value, "dialogue node id").toLowerCase(Locale.ROOT);
    }

    public static EchoDialogueNodeId of(String value) {
        return new EchoDialogueNodeId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
