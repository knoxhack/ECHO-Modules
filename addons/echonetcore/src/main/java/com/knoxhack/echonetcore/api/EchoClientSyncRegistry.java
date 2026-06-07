package com.knoxhack.echonetcore.api;

import com.knoxhack.echonetcore.EchoNetCore;
import com.knoxhack.echonetcore.network.EchoSyncPayload;
import com.knoxhack.echonetcore.network.EchoSyncType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;

public final class EchoClientSyncRegistry {
    private static final Map<Key, List<Consumer<EchoSyncPayload>>> CONSUMERS = new ConcurrentHashMap<>();

    private EchoClientSyncRegistry() {
    }

    public static void register(EchoSyncType type, Identifier channelId, Consumer<EchoSyncPayload> consumer) {
        subscribe(type, channelId, consumer);
    }

    public static AutoCloseable subscribe(EchoSyncType type, Identifier channelId, Consumer<EchoSyncPayload> consumer) {
        if (type == null || channelId == null || consumer == null) {
            return () -> {
            };
        }
        Key key = new Key(type, channelId);
        List<Consumer<EchoSyncPayload>> consumers =
                CONSUMERS.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>());
        consumers.add(consumer);
        return () -> {
            consumers.remove(consumer);
            if (consumers.isEmpty()) {
                CONSUMERS.remove(key, consumers);
            }
        };
    }

    public static void dispatch(EchoSyncPayload payload) {
        if (payload == null) {
            return;
        }
        for (Consumer<EchoSyncPayload> consumer : CONSUMERS.getOrDefault(new Key(payload.syncType(), payload.channelId()), List.of())) {
            try {
                consumer.accept(payload);
            } catch (RuntimeException exception) {
                EchoNetCore.LOGGER.warn("ECHO client sync consumer failed for {}; continuing.",
                        payload.channelId(), exception);
            }
        }
    }

    public static void clearForTests() {
        CONSUMERS.clear();
    }

    private record Key(EchoSyncType type, Identifier channelId) {
    }
}
