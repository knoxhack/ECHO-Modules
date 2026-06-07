package com.knoxhack.echorecovery.api;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface RecoverySignalProvider {
    Optional<String> signalStatus(ServerPlayer player, RecoveryGraveSnapshot snapshot);
}
