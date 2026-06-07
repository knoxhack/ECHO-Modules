package com.knoxhack.echo.npcore.network;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectDialogueOptionPacket(int entityId, String optionId) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "select_dialogue_option");
    public static final Type<SelectDialogueOptionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, SelectDialogueOptionPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.entityId);
                EchoPayloadCodecs.writeUtf(buf, packet.optionId, EchoPayloadCodecs.ID);
            },
            buf -> new SelectDialogueOptionPacket(buf.readVarInt(), EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID)));

    public SelectDialogueOptionPacket {
        optionId = optionId == null ? "" : optionId.trim();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
