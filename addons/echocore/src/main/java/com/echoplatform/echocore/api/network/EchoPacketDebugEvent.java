package com.echoplatform.echocore.api.network;

import java.time.Instant;
import net.minecraft.resources.Identifier;

public record EchoPacketDebugEvent(
        Identifier payloadId,
        EchoPacketDirection direction,
        EchoPacketKind kind,
        String playerName,
        boolean accepted,
        String detail,
        int bytes,
        Instant observedAt) {
    public EchoPacketDebugEvent {
        observedAt = observedAt == null ? Instant.now() : observedAt;
        playerName = playerName == null ? "" : playerName;
        detail = detail == null ? "" : detail;
    }

    public EchoPacketDebugEvent(String channel, EchoPacketKind kind, EchoPacketDirection direction, int bytes,
            Instant observedAt) {
        this(Identifier.fromNamespaceAndPath("echocore", sanitize(channel)), direction, kind, "", true, "", bytes,
                observedAt);
    }

    public EchoPacketDebugEvent(Identifier payloadId, EchoPacketDirection direction, EchoPacketKind kind,
            String playerName, boolean accepted, String detail) {
        this(payloadId, direction, kind, playerName, accepted, detail, 0, Instant.now());
    }

    public String channel() {
        return payloadId == null ? "" : payloadId.toString();
    }

    private static String sanitize(String channel) {
        String value = channel == null || channel.isBlank() ? "unknown" : channel.toLowerCase();
        return value.replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
    }
}
