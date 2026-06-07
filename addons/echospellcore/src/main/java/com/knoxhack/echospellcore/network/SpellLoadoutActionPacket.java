package com.knoxhack.echospellcore.network;

import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import com.knoxhack.echospellcore.EchoSpellCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SpellLoadoutActionPacket(String action, int slot, Identifier spellId, String modifierId)
        implements CustomPacketPayload {
    public static final Identifier ID = EchoSpellCore.id("spell_loadout_action");
    public static final Type<SpellLoadoutActionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, SpellLoadoutActionPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                EchoPayloadCodecs.writeUtf(buf, packet.action(), EchoPayloadCodecs.ID);
                buf.writeVarInt(packet.slot());
                EchoPayloadCodecs.writeIdentifier(buf, packet.spellId());
                EchoPayloadCodecs.writeUtf(buf, packet.modifierId(), EchoPayloadCodecs.ID);
            },
            buf -> new SpellLoadoutActionPacket(
                    EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID),
                    buf.readVarInt(),
                    EchoPayloadCodecs.readIdentifier(buf),
                    EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
