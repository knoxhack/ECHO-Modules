package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoPackId;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record EchoSaveMetadata(
        String saveId,
        EchoPackId packId,
        String packVersion,
        EchoPackVariantId variantId,
        EchoPackChannelId channelId,
        Instant lastPlayedAt,
        String minecraftVersion,
        String loaderVersion,
        Map<String, String> markers
) {
    public EchoSaveMetadata {
        saveId = PackContractGuards.requireText(saveId, "save id");
        Objects.requireNonNull(packId, "packId");
        packVersion = PackContractGuards.optionalText(packVersion);
        lastPlayedAt = lastPlayedAt == null ? Instant.EPOCH : lastPlayedAt;
        minecraftVersion = PackContractGuards.optionalText(minecraftVersion);
        loaderVersion = PackContractGuards.optionalText(loaderVersion);
        markers = PackContractGuards.immutableStringMap(markers);
    }
}
