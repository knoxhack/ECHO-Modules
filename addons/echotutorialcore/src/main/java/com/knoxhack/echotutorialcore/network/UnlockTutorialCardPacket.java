package com.knoxhack.echotutorialcore.network;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UnlockTutorialCardPacket(Identifier cardId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UnlockTutorialCardPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "unlock_card"));

    public static final StreamCodec<ByteBuf, UnlockTutorialCardPacket> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, UnlockTutorialCardPacket::cardId,
            UnlockTutorialCardPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
