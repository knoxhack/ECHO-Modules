package com.knoxhack.echoholomap.network;

import com.knoxhack.echoholomap.EchoHoloMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HoloMapSyncRequestPacket() implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "sync_request");
    public static final Type<HoloMapSyncRequestPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, HoloMapSyncRequestPacket> CODEC =
            StreamCodec.unit(new HoloMapSyncRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
