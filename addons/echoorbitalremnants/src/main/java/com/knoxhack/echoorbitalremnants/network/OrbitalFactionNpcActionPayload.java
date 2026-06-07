package com.knoxhack.echoorbitalremnants.network;

import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OrbitalFactionNpcActionPayload(int entityId, String actionId, String targetId) implements CustomPacketPayload {
    public static final Type<OrbitalFactionNpcActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "orbital_faction_npc_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OrbitalFactionNpcActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entityId);
                EchoPayloadCodecs.writeUtf(buffer, payload.actionId, EchoPayloadCodecs.ID);
                EchoPayloadCodecs.writeUtf(buffer, payload.targetId, EchoPayloadCodecs.ID);
            },
            buffer -> new OrbitalFactionNpcActionPayload(
                    buffer.readVarInt(),
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.ID),
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.ID)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
