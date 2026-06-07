package com.knoxhack.echolens.network;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensScanMode;
import com.knoxhack.echolens.api.LensTargetKind;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LensScanRequestPacket(
        int requestId,
        LensScanMode scanMode,
        LensTargetKind targetKind,
        BlockPos blockPos,
        int entityId,
        Identifier targetId) implements CustomPacketPayload {
    private static final int MAX_ID = 192;

    public static final Identifier ID = EchoLens.id("deep_scan_request");
    public static final Type<LensScanRequestPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, LensScanRequestPacket> CODEC =
            StreamCodec.of(LensScanRequestPacket::write, LensScanRequestPacket::read);

    public LensScanRequestPacket {
        scanMode = scanMode == null ? LensScanMode.DEEP : scanMode;
        targetKind = targetKind == null ? LensTargetKind.MISS : targetKind;
        entityId = Math.max(-1, entityId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, LensScanRequestPacket packet) {
        buffer.writeVarInt(packet.requestId());
        buffer.writeEnum(packet.scanMode());
        buffer.writeEnum(packet.targetKind());
        buffer.writeBoolean(packet.blockPos() != null);
        if (packet.blockPos() != null) {
            buffer.writeBlockPos(packet.blockPos());
        }
        buffer.writeVarInt(packet.entityId());
        buffer.writeUtf(packet.targetId() == null ? "" : packet.targetId().toString(), MAX_ID);
    }

    private static LensScanRequestPacket read(FriendlyByteBuf buffer) {
        int requestId = buffer.readVarInt();
        LensScanMode scanMode = buffer.readEnum(LensScanMode.class);
        LensTargetKind targetKind = buffer.readEnum(LensTargetKind.class);
        BlockPos blockPos = buffer.readBoolean() ? buffer.readBlockPos() : null;
        int entityId = buffer.readVarInt();
        Identifier targetId = Identifier.tryParse(buffer.readUtf(MAX_ID));
        return new LensScanRequestPacket(requestId, scanMode, targetKind, blockPos, entityId, targetId);
    }
}
