package com.knoxhack.echo.socialcore;

import java.util.Locale;

public record EchoDialogueTreeId(String value) {
    public EchoDialogueTreeId {
        value = SocialContractGuards.requireText(value, "dialogue tree id").toLowerCase(Locale.ROOT);
    }

    public static EchoDialogueTreeId of(String value) {
        return new EchoDialogueTreeId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
