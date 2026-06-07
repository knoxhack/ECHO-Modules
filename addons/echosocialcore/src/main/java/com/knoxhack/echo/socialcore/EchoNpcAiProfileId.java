package com.knoxhack.echo.socialcore;

import java.util.Locale;

public record EchoNpcAiProfileId(String value) {
    public EchoNpcAiProfileId {
        value = SocialContractGuards.requireText(value, "npc ai profile id").toLowerCase(Locale.ROOT);
    }

    public static EchoNpcAiProfileId of(String value) {
        return new EchoNpcAiProfileId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
