package com.knoxhack.echonetcore.network;

import com.knoxhack.echocore.EchoCore;
import com.knoxhack.echocore.api.network.EchoDiscoveryToast;
import com.knoxhack.echonetcore.EchoNetCore;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DiscoveryToastPacket(EchoDiscoveryToast toast) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoCore.MODID, "discovery_toast");
    public static final Type<DiscoveryToastPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, DiscoveryToastPacket> CODEC =
            StreamCodec.of(DiscoveryToastPacket::write, DiscoveryToastPacket::read);

    public DiscoveryToastPacket {
        toast = normalize(toast);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, DiscoveryToastPacket packet) {
        EchoDiscoveryToast toast = packet.toast;
        EchoPayloadCodecs.writeIdentifier(buffer, toast.featureId());
        EchoPayloadCodecs.writeUtf(buffer, toast.category(), EchoPayloadCodecs.SMALL_TEXT);
        EchoPayloadCodecs.writeUtf(buffer, toast.title(), EchoPayloadCodecs.SMALL_TEXT);
        EchoPayloadCodecs.writeUtf(buffer, toast.subtitle(), EchoPayloadCodecs.SMALL_TEXT);
        EchoPayloadCodecs.writeUtf(buffer, toast.iconArt(), EchoPayloadCodecs.SMALL_TEXT);
        EchoPayloadCodecs.writeUtf(buffer, toast.heroArt(), EchoPayloadCodecs.SMALL_TEXT);
        buffer.writeInt(toast.accentColor());
    }

    private static DiscoveryToastPacket read(FriendlyByteBuf buffer) {
        return new DiscoveryToastPacket(new EchoDiscoveryToast(
                EchoPayloadCodecs.readIdentifier(buffer),
                EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                buffer.readInt()));
    }

    private static EchoDiscoveryToast normalize(EchoDiscoveryToast toast) {
        if (toast == null) {
            return new EchoDiscoveryToast(Identifier.fromNamespaceAndPath(EchoNetCore.MODID, "unknown"),
                    "", "", "", "", "", 0);
        }
        Identifier featureId = toast.featureId() == null
                ? Identifier.fromNamespaceAndPath(EchoNetCore.MODID, "unknown")
                : toast.featureId();
        return new EchoDiscoveryToast(featureId, toast.category(), toast.title(), toast.subtitle(),
                toast.iconArt(), toast.heroArt(), toast.accentColor());
    }
}
