package com.knoxhack.echo.socialcore;

import java.util.Locale;

public record EchoFactionId(String value) {
    public EchoFactionId {
        value = SocialContractGuards.requireText(value, "faction id").toLowerCase(Locale.ROOT);
    }

    public static EchoFactionId of(String value) {
        return new EchoFactionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
