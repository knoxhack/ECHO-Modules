package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoApiStability;

import java.util.Objects;

public record EchoPackChannel(
        EchoPackChannelId id,
        String name,
        String summary,
        EchoApiStability stability,
        boolean publicChannel,
        boolean defaultChannel
) {
    public EchoPackChannel {
        Objects.requireNonNull(id, "id");
        name = PackContractGuards.requireText(name, "pack channel name");
        summary = PackContractGuards.optionalText(summary);
        stability = stability == null ? EchoApiStability.EXPERIMENTAL : stability;
    }
}
