package com.knoxhack.signalos.api;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface TerminalActionHandler {
    void handle(ServerPlayer player, String payload);
}
