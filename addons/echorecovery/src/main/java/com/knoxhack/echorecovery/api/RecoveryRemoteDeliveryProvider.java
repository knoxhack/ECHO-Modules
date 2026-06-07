package com.knoxhack.echorecovery.api;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface RecoveryRemoteDeliveryProvider {
    Optional<String> requestDelivery(ServerPlayer player, RecoveryGraveSnapshot snapshot);
}
