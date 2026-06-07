package com.knoxhack.echotutorialcore.network;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetGuideModePacket(String modeName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetGuideModePacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "set_guide_mode"));

    public static final StreamCodec<ByteBuf, SetGuideModePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetGuideModePacket::modeName,
            SetGuideModePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
