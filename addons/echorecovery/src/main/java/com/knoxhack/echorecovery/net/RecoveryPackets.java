package com.knoxhack.echorecovery.net;

import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;
import com.knoxhack.echorecovery.EchoRecovery;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class RecoveryPackets {
    public static final Identifier GRAVE_SYNC_ID = Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "grave_sync");
    public static final Identifier COMPASS_SYNC_ID = Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "compass_sync");

    private RecoveryPackets() {}

    public static void register(Object modEventBus) {
        registerPayloads(modEventBus);
    }

    public static void registerPayloads(Object event) {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        EchoNetPayloads.serverboundAction(registrar, RecoverAllPacket.TYPE, RecoverAllPacket.CODEC,
                EchoNetPayloads.defaultActionPolicy("recover_all"), RecoverAllPacket::handle);
    }

    public record GraveSyncPacket() implements CustomPacketPayload {
        public static final Type<GraveSyncPacket> TYPE = new Type<>(GRAVE_SYNC_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, GraveSyncPacket> CODEC = StreamCodec.unit(new GraveSyncPacket());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
