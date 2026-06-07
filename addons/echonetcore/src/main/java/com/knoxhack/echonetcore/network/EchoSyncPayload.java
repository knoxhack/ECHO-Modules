package com.knoxhack.echonetcore.network;

import com.knoxhack.echonetcore.EchoNetCore;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EchoSyncPayload(
        EchoSyncType syncType,
        Identifier channelId,
        BlockPos pos,
        CompoundTag payload) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoNetCore.MODID, "clientbound_sync");
    public static final Type<EchoSyncPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, EchoSyncPayload> CODEC =
            StreamCodec.of(EchoSyncPayload::write, EchoSyncPayload::read);

    public EchoSyncPayload {
        syncType = syncType == null ? EchoSyncType.PLAYER_DATA : syncType;
        channelId = channelId == null ? Identifier.fromNamespaceAndPath(EchoNetCore.MODID, "unknown") : channelId;
        payload = payload == null ? new CompoundTag() : payload.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, EchoSyncPayload packet) {
        EchoPayloadCodecs.writeEnum(buffer, packet.syncType, EchoSyncType.PLAYER_DATA);
        EchoPayloadCodecs.writeIdentifier(buffer, packet.channelId);
        EchoPayloadCodecs.writeOptionalBlockPos(buffer, packet.pos);
        buffer.writeNbt(packet.payload);
    }

    private static EchoSyncPayload read(FriendlyByteBuf buffer) {
        EchoSyncType type = EchoPayloadCodecs.readEnum(buffer, EchoSyncType.class, EchoSyncType.PLAYER_DATA);
        Identifier channelId = EchoPayloadCodecs.readIdentifier(buffer);
        BlockPos pos = EchoPayloadCodecs.readOptionalBlockPos(buffer);
        return new EchoSyncPayload(type, channelId, pos, buffer.readNbt());
    }
}
