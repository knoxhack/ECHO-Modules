package com.knoxhack.echoorbitalremnants.network;

import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenEchoTerminalPayload(EchoTerminalSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<OpenEchoTerminalPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "open_echo_terminal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEchoTerminalPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> payload.snapshot().write(buffer),
                    buffer -> new OpenEchoTerminalPayload(EchoTerminalSnapshot.read(buffer)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
