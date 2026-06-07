package com.knoxhack.echorecovery.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public interface RecoveryEventHooks {
    default void graveCreated(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
    }

    default void graveOpened(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
    }

    default void graveRecovered(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
    }

    default void graveExpired(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
    }

    default void graveDeleted(ServerPlayer player, BlockPos pos, String graveId) {
    }

    default void remoteRecoveryRequested(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
    }

    default void remoteRecoveryCompleted(ServerPlayer player, RecoveryGraveSnapshot snapshot, boolean success) {
    }
}
