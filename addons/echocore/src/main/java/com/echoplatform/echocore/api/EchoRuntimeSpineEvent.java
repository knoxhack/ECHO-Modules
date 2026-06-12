package com.echoplatform.echocore.api;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record EchoRuntimeSpineEvent(Identifier id, String source, String phase, Map<String, String> metadata,
        ServerPlayer player) {
    public EchoRuntimeSpineEvent(Identifier id, String source, String phase, Map<String, String> metadata) {
        this(id, source, phase, metadata, null);
    }

    public EchoRuntimeSpineEvent {
        source = source == null ? "" : source;
        phase = phase == null ? "" : phase;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static EchoRuntimeSpineEvent of(String sourceModId, Identifier eventId, ServerPlayer player,
            Identifier targetId, int amount, Map<String, String> context) {
        java.util.LinkedHashMap<String, String> metadata = new java.util.LinkedHashMap<>();
        if (context != null) {
            metadata.putAll(context);
        }
        if (player != null) {
            metadata.put("player", player.getUUID().toString());
        }
        metadata.put("target", targetId == null ? "" : targetId.toString());
        metadata.put("amount", Integer.toString(Math.max(0, amount)));
        return new EchoRuntimeSpineEvent(eventId, sourceModId, targetId == null ? "" : targetId.toString(), metadata,
                player);
    }

    public static EchoRuntimeSpineEvent of(String sourceModId, String eventId, String playerId,
            String targetId, int amount, Map<String, String> context) {
        Identifier parsedEvent = Identifier.tryParse(eventId == null ? "" : eventId);
        Identifier parsedTarget = Identifier.tryParse(targetId == null ? "" : targetId);
        java.util.LinkedHashMap<String, String> metadata = new java.util.LinkedHashMap<>();
        if (context != null) {
            metadata.putAll(context);
        }
        metadata.put("player", playerId == null ? "" : playerId);
        metadata.put("target", parsedTarget == null ? "" : parsedTarget.toString());
        metadata.put("amount", Integer.toString(Math.max(0, amount)));
        return new EchoRuntimeSpineEvent(
                parsedEvent == null ? Identifier.fromNamespaceAndPath("echocore", "runtime_spine_event") : parsedEvent,
                sourceModId,
                parsedTarget == null ? "" : parsedTarget.toString(),
                metadata);
    }

    public Identifier eventId() {
        return id;
    }

    public String sourceModule() {
        return source;
    }

    public Identifier targetId() {
        Identifier parsed = Identifier.tryParse(phase);
        return parsed == null ? id : parsed;
    }

    public Map<String, String> context() {
        return metadata;
    }

    public String contextValue(String key) {
        return metadata.getOrDefault(key, "");
    }

    public int amount() {
        try {
            return Integer.parseInt(metadata.getOrDefault("amount", "1"));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
