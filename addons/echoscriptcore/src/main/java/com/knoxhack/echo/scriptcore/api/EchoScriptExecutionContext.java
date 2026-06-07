package com.knoxhack.echo.scriptcore.api;

import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public record EchoScriptExecutionContext(
        Optional<ServerPlayer> player,
        Optional<MinecraftServer> server,
        String reason,
        Map<String, Object> data) {
    public EchoScriptExecutionContext {
        player = player == null ? Optional.empty() : player;
        server = server == null ? Optional.empty() : server;
        reason = reason == null ? "" : reason;
        data = Map.copyOf(data == null ? Map.of() : data);
    }

    public static EchoScriptExecutionContext empty() {
        return new EchoScriptExecutionContext(Optional.empty(), Optional.empty(), "", Map.of());
    }
}
