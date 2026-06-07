package com.knoxhack.signalos.api;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface TerminalActionResultHandler {
    SignalOsActionResult handle(ServerPlayer player, String payload);
}
