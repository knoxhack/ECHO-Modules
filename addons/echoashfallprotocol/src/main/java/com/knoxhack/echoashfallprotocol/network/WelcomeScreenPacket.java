package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server prompt for the physical client to show the first-join ECHO-7 welcome.
 */
public record WelcomeScreenPacket() implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "welcome_screen");
    public static final Type<WelcomeScreenPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, WelcomeScreenPacket> CODEC =
            StreamCodec.unit(new WelcomeScreenPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
