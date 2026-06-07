package com.knoxhack.echonetcore.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class EchoDebugCommandRegistry {
    private static final Map<Identifier, Handler> HANDLERS = new ConcurrentHashMap<>();

    private EchoDebugCommandRegistry() {
    }

    public static void register(Identifier commandId, Handler handler) {
        if (commandId != null && handler != null) {
            HANDLERS.put(commandId, handler);
        }
    }

    public static boolean handle(ServerPlayer player, Identifier commandId, CompoundTag payload) {
        Handler handler = HANDLERS.get(commandId);
        if (handler == null) {
            return false;
        }
        handler.handle(player, payload == null ? new CompoundTag() : payload.copy());
        return true;
    }

    public static void clearForTests() {
        HANDLERS.clear();
    }

    @FunctionalInterface
    public interface Handler {
        void handle(ServerPlayer player, CompoundTag payload);
    }
}
