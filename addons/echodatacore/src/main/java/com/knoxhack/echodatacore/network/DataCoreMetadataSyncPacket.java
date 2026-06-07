package com.knoxhack.echodatacore.network;

import com.knoxhack.echocore.api.DataKeyMetadata;
import com.knoxhack.echocore.api.DataScope;
import com.knoxhack.echocore.api.DataValueKind;
import com.knoxhack.echodatacore.EchoDataCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DataCoreMetadataSyncPacket(
        long revision,
        List<DataKeyMetadata> metadata) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 4096;
    private static final int MAX_ID = 192;
    private static final int MAX_TEXT = 512;
    private static final int MAX_SOURCE = 192;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoDataCore.MODID, "metadata_sync");
    public static final Type<DataCoreMetadataSyncPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, DataCoreMetadataSyncPacket> CODEC =
            StreamCodec.of(DataCoreMetadataSyncPacket::write, DataCoreMetadataSyncPacket::read);

    public DataCoreMetadataSyncPacket {
        metadata = metadata == null ? List.of() : metadata.stream()
                .filter(meta -> meta != null && meta.id() != null)
                .limit(MAX_ENTRIES)
                .toList();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, DataCoreMetadataSyncPacket packet) {
        buffer.writeVarLong(packet.revision());
        buffer.writeVarInt(Math.min(MAX_ENTRIES, packet.metadata().size()));
        for (DataKeyMetadata meta : packet.metadata().stream().limit(MAX_ENTRIES).toList()) {
            buffer.writeUtf(meta.id().toString(), MAX_ID);
            buffer.writeUtf(meta.scope().name(), 32);
            buffer.writeUtf(meta.kind().name(), 32);
            buffer.writeBoolean(meta.synced());
            buffer.writeUtf(meta.title(), MAX_TEXT);
            buffer.writeUtf(meta.description(), MAX_TEXT);
            buffer.writeUtf(meta.owner(), MAX_TEXT);
            buffer.writeUtf(meta.legacyRoot(), MAX_TEXT);
            buffer.writeUtf(meta.legacyField(), MAX_TEXT);
            buffer.writeUtf(meta.defaultValue(), MAX_TEXT);
            buffer.writeUtf(meta.source(), MAX_SOURCE);
        }
    }

    private static DataCoreMetadataSyncPacket read(FriendlyByteBuf buffer) {
        long revision = buffer.readVarLong();
        int count = Math.max(0, Math.min(MAX_ENTRIES, buffer.readVarInt()));
        List<DataKeyMetadata> metadata = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Identifier id = Identifier.tryParse(buffer.readUtf(MAX_ID));
            DataScope scope = safeScope(buffer.readUtf(32));
            DataValueKind kind = safeKind(buffer.readUtf(32));
            boolean synced = buffer.readBoolean();
            String title = buffer.readUtf(MAX_TEXT);
            String description = buffer.readUtf(MAX_TEXT);
            String owner = buffer.readUtf(MAX_TEXT);
            String legacyRoot = buffer.readUtf(MAX_TEXT);
            String legacyField = buffer.readUtf(MAX_TEXT);
            String defaultValue = buffer.readUtf(MAX_TEXT);
            String source = buffer.readUtf(MAX_SOURCE);
            if (id != null) {
                metadata.add(new DataKeyMetadata(id, scope, kind, synced, title, description,
                        owner, legacyRoot, legacyField, defaultValue, source));
            }
        }
        return new DataCoreMetadataSyncPacket(revision, metadata);
    }

    private static DataScope safeScope(String value) {
        try {
            return DataScope.valueOf(value);
        } catch (RuntimeException ignored) {
            return DataScope.PLAYER;
        }
    }

    private static DataValueKind safeKind(String value) {
        try {
            return DataValueKind.valueOf(value);
        } catch (RuntimeException ignored) {
            return DataValueKind.RECORD;
        }
    }
}
