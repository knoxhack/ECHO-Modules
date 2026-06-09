package com.knoxhack.echocore.api.network;

import java.time.Instant;

public record EchoPacketDebugEvent(String channel, EchoPacketKind kind, EchoPacketDirection direction, int bytes, Instant observedAt) {
    public EchoPacketDebugEvent {
        observedAt = observedAt == null ? Instant.now() : observedAt;
    }
}
