package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FactionNpcActionPacket(int entityId, String actionId, String targetId) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "faction_npc_action");

    public static final StreamCodec<FriendlyByteBuf, FactionNpcActionPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.entityId);
                EchoPayloadCodecs.writeUtf(buf, packet.actionId, EchoPayloadCodecs.ID);
                EchoPayloadCodecs.writeUtf(buf, packet.targetId, EchoPayloadCodecs.ID);
            },
            buf -> new FactionNpcActionPacket(buf.readVarInt(),
                    EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID),
                    EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID))
    );

    public static final Type<FactionNpcActionPacket> TYPE = new Type<>(ID);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
