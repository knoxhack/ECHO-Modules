package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Network packet to request companion drone commands from the ECHO Terminal.
 */
public record DroneCommandPacket(String command) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "drone_command");

    public static final StreamCodec<FriendlyByteBuf, DroneCommandPacket> CODEC = StreamCodec.of(
        (buf, packet) -> EchoPayloadCodecs.writeUtf(buf, packet.command, EchoPayloadCodecs.ID),
        buf -> new DroneCommandPacket(EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID))
    );

    public static final Type<DroneCommandPacket> TYPE = new Type<>(ID);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
