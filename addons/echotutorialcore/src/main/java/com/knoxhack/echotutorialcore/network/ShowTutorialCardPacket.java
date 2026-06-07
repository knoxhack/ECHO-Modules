package com.knoxhack.echotutorialcore.network;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ShowTutorialCardPacket(Identifier cardId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShowTutorialCardPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "show_card"));

    public static final StreamCodec<ByteBuf, ShowTutorialCardPacket> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ShowTutorialCardPacket::cardId,
            ShowTutorialCardPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
