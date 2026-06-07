package com.knoxhack.echo.contentcore;

import com.knoxhack.echo.platformcore.EchoPackId;

import java.util.Map;
import java.util.Objects;

public record EchoContentPackRef(
        EchoPackId packId,
        String variant,
        String channel,
        boolean required,
        boolean official,
        String summary,
        Map<String, String> attributes
) {
    public EchoContentPackRef {
        Objects.requireNonNull(packId, "packId");
        variant = ContentContractGuards.optionalText(variant);
        channel = ContentContractGuards.optionalText(channel);
        summary = ContentContractGuards.optionalText(summary);
        attributes = ContentContractGuards.immutableMap(attributes);
    }

    public boolean scopedToVariant() {
        return !variant.isEmpty();
    }

    public boolean scopedToChannel() {
        return !channel.isEmpty();
    }
}
