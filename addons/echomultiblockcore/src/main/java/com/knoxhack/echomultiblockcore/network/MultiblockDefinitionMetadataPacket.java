package com.knoxhack.echomultiblockcore.network;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.content.MultiblockContent;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MultiblockDefinitionMetadataPacket(List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 512;
    public static final Identifier ID = EchoMultiblockCore.id("definition_metadata");
    public static final Type<MultiblockDefinitionMetadataPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockDefinitionMetadataPacket> CODEC =
            StreamCodec.of(MultiblockDefinitionMetadataPacket::write, MultiblockDefinitionMetadataPacket::read);

    public MultiblockDefinitionMetadataPacket {
        entries = List.copyOf(entries == null ? List.of() : entries.stream().limit(MAX_ENTRIES).toList());
    }

    public static MultiblockDefinitionMetadataPacket current() {
        return new MultiblockDefinitionMetadataPacket(MultiblockContent.definitions().stream().map(Entry::from).toList());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, MultiblockDefinitionMetadataPacket packet) {
        buffer.writeVarInt(packet.entries().size());
        for (Entry entry : packet.entries()) {
            EchoPayloadCodecs.writeIdentifier(buffer, entry.id());
            buffer.writeUtf(entry.displayName(), 160);
            buffer.writeUtf(entry.role(), 64);
            buffer.writeUtf(entry.category(), 64);
            buffer.writeVarInt(entry.width());
            buffer.writeVarInt(entry.height());
            buffer.writeVarInt(entry.depth());
            buffer.writeInt(entry.previewColor());
        }
    }

    private static MultiblockDefinitionMetadataPacket read(RegistryFriendlyByteBuf buffer) {
        int count = Math.max(0, Math.min(MAX_ENTRIES, buffer.readVarInt()));
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(
                    EchoPayloadCodecs.readIdentifier(buffer),
                    buffer.readUtf(160),
                    buffer.readUtf(64),
                    buffer.readUtf(64),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readInt()));
        }
        return new MultiblockDefinitionMetadataPacket(entries);
    }

    public record Entry(Identifier id, String displayName, String role, String category, int width, int height, int depth, int previewColor) {
        public Entry {
            displayName = displayName == null || displayName.isBlank() ? id.getPath() : displayName.strip();
            role = role == null || role.isBlank() ? "INFRASTRUCTURE" : role.strip();
            category = category == null || category.isBlank() ? "general" : category.strip();
            width = Math.max(1, width);
            height = Math.max(1, height);
            depth = Math.max(1, depth);
        }

        public static Entry from(MultiblockDefinition definition) {
            return new Entry(definition.id(), definition.displayName(), definition.role().name(), definition.category(),
                    definition.width(), definition.height(), definition.depth(), definition.previewColor());
        }
    }
}
