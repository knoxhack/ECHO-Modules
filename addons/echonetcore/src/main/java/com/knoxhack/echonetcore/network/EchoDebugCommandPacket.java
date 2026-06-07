package com.knoxhack.echonetcore.network;

import com.knoxhack.echonetcore.EchoNetCore;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EchoDebugCommandPacket(Identifier commandId, CompoundTag payload) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoNetCore.MODID, "debug_command");
    public static final Type<EchoDebugCommandPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, EchoDebugCommandPacket> CODEC =
            StreamCodec.of(EchoDebugCommandPacket::write, EchoDebugCommandPacket::read);

    public EchoDebugCommandPacket {
        commandId = commandId == null ? Identifier.fromNamespaceAndPath(EchoNetCore.MODID, "unknown") : commandId;
        payload = payload == null ? new CompoundTag() : payload.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, EchoDebugCommandPacket packet) {
        EchoPayloadCodecs.writeIdentifier(buffer, packet.commandId);
        buffer.writeNbt(packet.payload);
    }

    private static EchoDebugCommandPacket read(FriendlyByteBuf buffer) {
        return new EchoDebugCommandPacket(EchoPayloadCodecs.readIdentifier(buffer), buffer.readNbt());
    }
}
