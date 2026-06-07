package com.knoxhack.signalos.network;

import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.TerminalIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SignalOsActionPacket(Identifier pageId, Identifier actionId, String payload) implements CustomPacketPayload {
    private static final int MAX_ID = 160;
    private static final int MAX_PAYLOAD = 4096;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(SignalOS.MODID, "terminal_action");
    public static final Type<SignalOsActionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SignalOsActionPacket> CODEC =
            StreamCodec.of(SignalOsActionPacket::write, SignalOsActionPacket::read);

    public SignalOsActionPacket {
        TerminalIds.requireLowercase(pageId, "SignalOS action page");
        TerminalIds.requireLowercase(actionId, "SignalOS action");
        payload = payload == null ? "" : payload;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, SignalOsActionPacket packet) {
        buffer.writeUtf(packet.pageId().toString(), MAX_ID);
        buffer.writeUtf(packet.actionId().toString(), MAX_ID);
        buffer.writeUtf(packet.payload(), MAX_PAYLOAD);
    }

    private static SignalOsActionPacket read(RegistryFriendlyByteBuf buffer) {
        return new SignalOsActionPacket(
                Identifier.parse(buffer.readUtf(MAX_ID)),
                Identifier.parse(buffer.readUtf(MAX_ID)),
                buffer.readUtf(MAX_PAYLOAD));
    }
}
