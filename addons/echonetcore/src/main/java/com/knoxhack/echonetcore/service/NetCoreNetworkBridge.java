package com.knoxhack.echonetcore.service;

import com.knoxhack.echocore.api.network.EchoDiscoveryToast;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echocore.api.network.INetworkBridge;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echonetcore.network.DiscoveryToastPacket;
import com.knoxhack.echonetcore.network.EchoFactionSyncPacket;
import com.knoxhack.echonetcore.network.EchoSyncPayload;
import com.knoxhack.echonetcore.network.EchoSyncType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class NetCoreNetworkBridge implements INetworkBridge {
    public static final NetCoreNetworkBridge INSTANCE = new NetCoreNetworkBridge();

    private NetCoreNetworkBridge() {
    }

    @Override
    public void syncFactionData(ServerPlayer player, CompoundTag factionRoot) {
        EchoNetSend.toPlayer(player, new EchoFactionSyncPacket(factionRoot), EchoPacketKind.CLIENTBOUND_SYNC);
    }

    @Override
    public void sendDiscoveryToast(ServerPlayer player, EchoDiscoveryToast toast) {
        EchoNetSend.toPlayer(player, new DiscoveryToastPacket(toast), EchoPacketKind.CLIENTBOUND_SYNC);
    }

    @Override
    public void syncPlayerData(ServerPlayer player, Identifier channelId, CompoundTag payload) {
        sync(player, EchoSyncType.PLAYER_DATA, channelId, null, payload);
    }

    @Override
    public void syncWorldData(ServerPlayer player, Identifier channelId, CompoundTag payload) {
        sync(player, EchoSyncType.WORLD_DATA, channelId, null, payload);
    }

    @Override
    public void syncMissionProgress(ServerPlayer player, Identifier missionId, CompoundTag payload) {
        sync(player, EchoSyncType.MISSION_PROGRESS, missionId, null, payload);
    }

    @Override
    public void syncVisualState(ServerPlayer player, Identifier subjectId, CompoundTag payload) {
        sync(player, EchoSyncType.VISUAL_STATE, subjectId, null, payload);
    }

    @Override
    public void syncMachineState(ServerPlayer player, BlockPos pos, Identifier machineId, CompoundTag payload) {
        sync(player, EchoSyncType.MACHINE_STATE, machineId, pos, payload);
    }

    @Override
    public void sendDebugData(ServerPlayer player, Identifier debugId, CompoundTag payload) {
        sync(player, EchoSyncType.DEBUG_DATA, debugId, null, payload);
    }

    private static void sync(ServerPlayer player, EchoSyncType type, Identifier channelId, BlockPos pos,
            CompoundTag payload) {
        EchoNetSend.toPlayer(player, new EchoSyncPayload(type, channelId, pos, payload), EchoPacketKind.CLIENTBOUND_SYNC);
    }
}
