package com.knoxhack.echo.socialcore;

import java.util.Locale;

public record EchoNpcProfileId(String value) {
    public EchoNpcProfileId {
        value = SocialContractGuards.requireText(value, "npc profile id").toLowerCase(Locale.ROOT);
    }

    public static EchoNpcProfileId of(String value) {
        return new EchoNpcProfileId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
