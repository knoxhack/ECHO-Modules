package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Network packet to persistently mark an archived intel entry as read.
 */
public record ArchiveIntelReadPacket(String intelId) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "archive_intel_read");

    public static final StreamCodec<FriendlyByteBuf, ArchiveIntelReadPacket> CODEC = StreamCodec.of(
        (buf, packet) -> EchoPayloadCodecs.writeUtf(buf, packet.intelId, EchoPayloadCodecs.ID),
        buf -> new ArchiveIntelReadPacket(EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID))
    );

    public static final Type<ArchiveIntelReadPacket> TYPE = new Type<>(ID);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
