package com.knoxhack.echodatacore.network;

import com.knoxhack.echocore.api.DataScope;
import com.knoxhack.echocore.api.DataValueKind;
import com.knoxhack.echodatacore.EchoDataCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DataCoreSyncPacket(
        DataScope scope,
        String ownerId,
        boolean fullSnapshot,
        long revision,
        List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_OWNER = 160;
    private static final int MAX_KEY = 192;
    private static final int MAX_ENTRIES = 512;
    private static final int MAX_NBT_DEBUG_CHARS = 16_384;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoDataCore.MODID, "data_sync");
    public static final Type<DataCoreSyncPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, DataCoreSyncPacket> CODEC =
            StreamCodec.of(DataCoreSyncPacket::write, DataCoreSyncPacket::read);

    public DataCoreSyncPacket {
        scope = scope == null ? DataScope.PLAYER : scope;
        ownerId = ownerId == null ? "" : ownerId;
        entries = copyEntries(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, DataCoreSyncPacket packet) {
        buffer.writeUtf(packet.scope().name());
        buffer.writeUtf(packet.ownerId(), MAX_OWNER);
        buffer.writeBoolean(packet.fullSnapshot());
        buffer.writeVarLong(packet.revision());
        buffer.writeVarInt(Math.min(MAX_ENTRIES, packet.entries().size()));
        for (Entry entry : packet.entries().stream().limit(MAX_ENTRIES).toList()) {
            buffer.writeUtf(entry.keyId().toString(), MAX_KEY);
            buffer.writeUtf(entry.kind().name());
            buffer.writeBoolean(entry.clear());
            buffer.writeNbt(entry.data());
        }
    }

    private static DataCoreSyncPacket read(FriendlyByteBuf buffer) {
        DataScope scope = safeScope(buffer.readUtf(32));
        String ownerId = buffer.readUtf(MAX_OWNER);
        boolean fullSnapshot = buffer.readBoolean();
        long revision = buffer.readVarLong();
        int count = Math.max(0, Math.min(MAX_ENTRIES, buffer.readVarInt()));
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Identifier keyId = Identifier.tryParse(buffer.readUtf(MAX_KEY));
            DataValueKind kind = safeKind(buffer.readUtf(32));
            boolean clear = buffer.readBoolean();
            CompoundTag data = buffer.readNbt();
            if (keyId != null && data != null) {
                entries.add(new Entry(keyId, kind, data, clear));
            }
        }
        return new DataCoreSyncPacket(scope, ownerId, fullSnapshot, revision, entries);
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

    private static List<Entry> copyEntries(List<Entry> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream().filter(entry -> entry != null && entry.keyId() != null).limit(MAX_ENTRIES).toList();
    }

    public record Entry(Identifier keyId, DataValueKind kind, CompoundTag data, boolean clear) {
        public Entry(Identifier keyId, DataValueKind kind, CompoundTag data) {
            this(keyId, kind, data, false);
        }

        public Entry {
            kind = kind == null ? DataValueKind.RECORD : kind;
            data = safeData(data, clear);
        }

        private static CompoundTag safeData(CompoundTag source, boolean clear) {
            if (clear || source == null) {
                return new CompoundTag();
            }
            CompoundTag copy = source.copy();
            if (copy.toString().length() > MAX_NBT_DEBUG_CHARS) {
                CompoundTag clipped = new CompoundTag();
                clipped.putString("error", "DataCore entry exceeded sync payload guard.");
                return clipped;
            }
            return copy;
        }
    }
}
