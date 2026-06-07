package com.knoxhack.echotutorialcore.network;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncTutorialProgressPacket(
        String guideMode,
        List<String> progressFlags,
        List<String> unlockedCardIds,
        List<String> unreadCardIds,
        List<String> completedFlowIds,
        String lastRecommendationReason) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 2048;
    private static final int MAX_TEXT = 256;

    public static final CustomPacketPayload.Type<SyncTutorialProgressPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "sync_progress"));

    public static final StreamCodec<FriendlyByteBuf, SyncTutorialProgressPacket> CODEC =
            StreamCodec.of(SyncTutorialProgressPacket::write, SyncTutorialProgressPacket::read);

    public SyncTutorialProgressPacket {
        guideMode = guideMode == null || guideMode.isBlank() ? "NORMAL" : guideMode;
        progressFlags = clean(progressFlags);
        unlockedCardIds = clean(unlockedCardIds);
        unreadCardIds = clean(unreadCardIds);
        completedFlowIds = clean(completedFlowIds);
        lastRecommendationReason = lastRecommendationReason == null ? "" : lastRecommendationReason;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buf, SyncTutorialProgressPacket packet) {
        buf.writeUtf(packet.guideMode(), 32);
        writeStrings(buf, packet.progressFlags());
        writeStrings(buf, packet.unlockedCardIds());
        writeStrings(buf, packet.unreadCardIds());
        writeStrings(buf, packet.completedFlowIds());
        buf.writeUtf(packet.lastRecommendationReason(), MAX_TEXT);
    }

    private static SyncTutorialProgressPacket read(FriendlyByteBuf buf) {
        return new SyncTutorialProgressPacket(
                buf.readUtf(32),
                readStrings(buf),
                readStrings(buf),
                readStrings(buf),
                readStrings(buf),
                buf.readUtf(MAX_TEXT));
    }

    private static void writeStrings(FriendlyByteBuf buf, List<String> values) {
        buf.writeVarInt(Math.min(MAX_ENTRIES, values.size()));
        for (String value : values.stream().limit(MAX_ENTRIES).toList()) {
            buf.writeUtf(value, MAX_TEXT);
        }
    }

    private static List<String> readStrings(FriendlyByteBuf buf) {
        int count = Math.max(0, Math.min(MAX_ENTRIES, buf.readVarInt()));
        List<String> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String value = buf.readUtf(MAX_TEXT);
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private static List<String> clean(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(MAX_ENTRIES)
                .toList();
    }
}
