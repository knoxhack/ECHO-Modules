package com.echoplatform.echocore.api;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface EchoRecoveryService {
    boolean recover(ServerPlayer player, String recoveryId);
}
