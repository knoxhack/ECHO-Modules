package com.knoxhack.echotutorialcore.network;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ShowTutorialHintPacket(Identifier hintId, String typeName, String title, String message, String details) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShowTutorialHintPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "show_hint"));

    public static final StreamCodec<ByteBuf, ShowTutorialHintPacket> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ShowTutorialHintPacket::hintId,
            ByteBufCodecs.STRING_UTF8, ShowTutorialHintPacket::typeName,
            ByteBufCodecs.STRING_UTF8, ShowTutorialHintPacket::title,
            ByteBufCodecs.STRING_UTF8, ShowTutorialHintPacket::message,
            ByteBufCodecs.STRING_UTF8, ShowTutorialHintPacket::details,
            ShowTutorialHintPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
