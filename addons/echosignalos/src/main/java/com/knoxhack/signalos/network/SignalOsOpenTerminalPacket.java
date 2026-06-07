package com.knoxhack.signalos.network;

import com.knoxhack.signalos.SignalOS;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SignalOsOpenTerminalPacket() implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(SignalOS.MODID, "open_terminal");
    public static final Type<SignalOsOpenTerminalPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SignalOsOpenTerminalPacket> CODEC =
            StreamCodec.unit(new SignalOsOpenTerminalPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
