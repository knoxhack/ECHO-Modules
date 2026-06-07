package com.knoxhack.echomultiblockcore.network;

import com.knoxhack.echomultiblockcore.Config;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.MultiblockBuildAssistCell;
import com.knoxhack.echomultiblockcore.api.MultiblockBuildAssistSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockMaterialSummary;
import com.knoxhack.echomultiblockcore.api.StructureBlockRequirement;
import com.knoxhack.echomultiblockcore.content.MultiblockContent;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MultiblockBuildAssistPacket(List<MultiblockBuildAssistSnapshot> snapshots) implements CustomPacketPayload {
    private static final int MAX_DEFINITIONS = 512;
    private static final int MAX_CELLS = 8192;
    private static final int MAX_MATERIALS = 96;
    public static final Identifier ID = EchoMultiblockCore.id("build_assist_metadata");
    public static final Type<MultiblockBuildAssistPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockBuildAssistPacket> CODEC =
            StreamCodec.of(MultiblockBuildAssistPacket::write, MultiblockBuildAssistPacket::read);

    public MultiblockBuildAssistPacket {
        snapshots = List.copyOf(snapshots == null ? List.of() : snapshots.stream().limit(MAX_DEFINITIONS).toList());
    }

    public static MultiblockBuildAssistPacket current() {
        int maxVolume;
        try {
            maxVolume = Config.MAX_VALIDATION_VOLUME.get();
        } catch (RuntimeException exception) {
            maxVolume = 4096;
        }
        final int validationVolume = maxVolume;
        return new MultiblockBuildAssistPacket(MultiblockContent.definitions().stream()
                .map(definition -> MultiblockBuildAssistSnapshot.from(definition, validationVolume))
                .toList());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, MultiblockBuildAssistPacket packet) {
        buffer.writeVarInt(packet.snapshots().size());
        for (MultiblockBuildAssistSnapshot snapshot : packet.snapshots()) {
            EchoPayloadCodecs.writeIdentifier(buffer, snapshot.definitionId());
            buffer.writeUtf(snapshot.displayName(), 160);
            buffer.writeVarInt(snapshot.width());
            buffer.writeVarInt(snapshot.height());
            buffer.writeVarInt(snapshot.depth());
            buffer.writeBlockPos(snapshot.controllerLocalPos());
            buffer.writeInt(snapshot.previewColor());
            buffer.writeBoolean(snapshot.rotationsAllowed());
            buffer.writeBoolean(snapshot.mirrorable());
            buffer.writeBoolean(snapshot.complete());
            buffer.writeUtf(snapshot.warning(), 240);
            List<MultiblockBuildAssistCell> cells = snapshot.cells().stream().limit(MAX_CELLS).toList();
            buffer.writeVarInt(cells.size());
            for (MultiblockBuildAssistCell cell : cells) {
                buffer.writeBlockPos(cell.localPos());
                buffer.writeEnum(cell.kind());
                buffer.writeUtf(cell.expected(), 160);
                buffer.writeBoolean(cell.optional());
                buffer.writeBoolean(cell.air());
                buffer.writeBoolean(cell.wildcard());
            }
            List<MultiblockMaterialSummary.Entry> entries = snapshot.materials().entries().stream().limit(MAX_MATERIALS).toList();
            buffer.writeVarInt(entries.size());
            for (MultiblockMaterialSummary.Entry entry : entries) {
                buffer.writeEnum(entry.kind());
                buffer.writeUtf(entry.expected(), 160);
                buffer.writeVarInt(entry.count());
                buffer.writeBoolean(entry.optional());
                buffer.writeBoolean(entry.placeable());
            }
        }
    }

    private static MultiblockBuildAssistPacket read(RegistryFriendlyByteBuf buffer) {
        int count = Math.max(0, Math.min(MAX_DEFINITIONS, buffer.readVarInt()));
        List<MultiblockBuildAssistSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Identifier id = EchoPayloadCodecs.readIdentifier(buffer);
            String displayName = buffer.readUtf(160);
            int width = buffer.readVarInt();
            int height = buffer.readVarInt();
            int depth = buffer.readVarInt();
            BlockPos controllerLocal = buffer.readBlockPos();
            int previewColor = buffer.readInt();
            boolean rotationsAllowed = buffer.readBoolean();
            boolean mirrorable = buffer.readBoolean();
            boolean complete = buffer.readBoolean();
            String warning = buffer.readUtf(240);
            int cellCount = Math.max(0, Math.min(MAX_CELLS, buffer.readVarInt()));
            List<MultiblockBuildAssistCell> cells = new ArrayList<>();
            for (int c = 0; c < cellCount; c++) {
                cells.add(new MultiblockBuildAssistCell(
                        buffer.readBlockPos(),
                        buffer.readEnum(StructureBlockRequirement.SlotKind.class),
                        buffer.readUtf(160),
                        buffer.readBoolean(),
                        buffer.readBoolean(),
                        buffer.readBoolean()));
            }
            int materialCount = Math.max(0, Math.min(MAX_MATERIALS, buffer.readVarInt()));
            List<MultiblockMaterialSummary.Entry> materials = new ArrayList<>();
            for (int m = 0; m < materialCount; m++) {
                materials.add(new MultiblockMaterialSummary.Entry(
                        buffer.readEnum(StructureBlockRequirement.SlotKind.class),
                        buffer.readUtf(160),
                        buffer.readVarInt(),
                        buffer.readBoolean(),
                        buffer.readBoolean()));
            }
            snapshots.add(new MultiblockBuildAssistSnapshot(id, displayName, width, height, depth,
                    controllerLocal, previewColor, rotationsAllowed, mirrorable, complete, warning,
                    cells, new MultiblockMaterialSummary(id, materials)));
        }
        return new MultiblockBuildAssistPacket(snapshots);
    }
}
