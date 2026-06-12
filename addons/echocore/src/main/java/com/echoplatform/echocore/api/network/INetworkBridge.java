package com.echoplatform.echocore.api.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public interface INetworkBridge {
    INetworkBridge NOOP = new INetworkBridge() {
    };

    default void syncFactionData(ServerPlayer player, CompoundTag factionRoot) {
    }

    default void sendDiscoveryToast(ServerPlayer player, EchoDiscoveryToast toast) {
    }

    default void syncPlayerData(ServerPlayer player, Identifier channelId, CompoundTag payload) {
    }

    default void syncWorldData(ServerPlayer player, Identifier channelId, CompoundTag payload) {
    }

    default void syncMissionProgress(ServerPlayer player, Identifier missionId, CompoundTag payload) {
    }

    default void syncVisualState(ServerPlayer player, Identifier subjectId, CompoundTag payload) {
    }

    default void syncMachineState(ServerPlayer player, BlockPos pos, Identifier machineId, CompoundTag payload) {
    }

    default void sendDebugData(ServerPlayer player, Identifier debugId, CompoundTag payload) {
    }
}
