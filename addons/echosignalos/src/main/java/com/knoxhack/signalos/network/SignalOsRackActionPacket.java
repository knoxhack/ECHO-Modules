package com.knoxhack.signalos.network;

import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.TerminalIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SignalOsRackActionPacket(BlockPos pos, int slot, Identifier actionId, String payload)
        implements CustomPacketPayload {
    private static final int MAX_ID = 160;
    private static final int MAX_PAYLOAD = 4096;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(SignalOS.MODID, "rack_action");
    public static final Type<SignalOsRackActionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SignalOsRackActionPacket> CODEC =
            StreamCodec.of(SignalOsRackActionPacket::write, SignalOsRackActionPacket::read);

    public SignalOsRackActionPacket {
        pos = pos == null ? BlockPos.ZERO : pos;
        TerminalIds.requireLowercase(actionId, "SignalOS rack action");
        payload = payload == null ? "" : payload;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, SignalOsRackActionPacket packet) {
        buffer.writeBlockPos(packet.pos());
        buffer.writeVarInt(packet.slot());
        buffer.writeUtf(packet.actionId().toString(), MAX_ID);
        buffer.writeUtf(packet.payload(), MAX_PAYLOAD);
    }

    private static SignalOsRackActionPacket read(RegistryFriendlyByteBuf buffer) {
        return new SignalOsRackActionPacket(
                buffer.readBlockPos(),
                buffer.readVarInt(),
                Identifier.parse(buffer.readUtf(MAX_ID)),
                buffer.readUtf(MAX_PAYLOAD));
    }
}
