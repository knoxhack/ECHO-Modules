package com.knoxhack.echoorbitalremnants.network;

import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OrbitalEventVisualPayload(
        String eventName,
        int overlayColor,
        int particleColor,
        float intensity,
        long seed
) implements CustomPacketPayload {
    public static final Type<OrbitalEventVisualPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "orbital_event_visual"));
    public static final StreamCodec<FriendlyByteBuf, OrbitalEventVisualPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.eventName);
                buffer.writeInt(payload.overlayColor);
                buffer.writeInt(payload.particleColor);
                buffer.writeFloat(payload.intensity);
                buffer.writeLong(payload.seed);
            },
            buffer -> new OrbitalEventVisualPayload(
                    buffer.readUtf(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readFloat(),
                    buffer.readLong()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
