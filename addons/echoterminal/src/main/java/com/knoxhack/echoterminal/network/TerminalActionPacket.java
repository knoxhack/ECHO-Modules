package com.knoxhack.echoterminal.network;

import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.TerminalApiIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TerminalActionPacket(Identifier tabId, Identifier actionId, String payload) implements CustomPacketPayload {
    private static final int MAX_ID = 160;
    private static final int MAX_PAYLOAD = 4096;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "terminal_action");
    public static final Type<TerminalActionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalActionPacket> CODEC =
            StreamCodec.of(TerminalActionPacket::write, TerminalActionPacket::read);

    public TerminalActionPacket {
        TerminalApiIds.requireLowercase(tabId, "Terminal action tab");
        TerminalApiIds.requireLowercase(actionId, "Terminal action");
        payload = payload == null ? "" : payload;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, TerminalActionPacket packet) {
        buffer.writeUtf(packet.tabId().toString(), MAX_ID);
        buffer.writeUtf(packet.actionId().toString(), MAX_ID);
        buffer.writeUtf(packet.payload(), MAX_PAYLOAD);
    }

    private static TerminalActionPacket read(RegistryFriendlyByteBuf buffer) {
        return new TerminalActionPacket(
                Identifier.parse(buffer.readUtf(MAX_ID)),
                Identifier.parse(buffer.readUtf(MAX_ID)),
                buffer.readUtf(MAX_PAYLOAD));
    }
}
