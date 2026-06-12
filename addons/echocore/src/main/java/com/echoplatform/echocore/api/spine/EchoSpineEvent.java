package com.echoplatform.echocore.api.spine;

import java.time.Instant;
import java.util.Map;

public record EchoSpineEvent(String channel, String type, String sourceModule, Instant emittedAt, Map<String, String> attributes) {
    public EchoSpineEvent {
        emittedAt = emittedAt == null ? Instant.now() : emittedAt;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static EchoSpineEvent of(String channel, String type, String sourceModule) {
        return new EchoSpineEvent(channel, type, sourceModule, Instant.now(), Map.of());
    }
}
