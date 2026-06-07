package com.knoxhack.echolens.network;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensInfoSection;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LensScanResponsePacket(
        int requestId,
        LensServerScanStatus status,
        String targetSignature,
        List<LensNetworkSection> sections,
        String message) implements CustomPacketPayload {
    private static final int MAX_SIGNATURE = 192;
    private static final int MAX_MESSAGE = 160;
    private static final int MAX_SECTIONS = 12;

    public static final Identifier ID = EchoLens.id("deep_scan_response");
    public static final Type<LensScanResponsePacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, LensScanResponsePacket> CODEC =
            StreamCodec.of(LensScanResponsePacket::write, LensScanResponsePacket::read);

    public LensScanResponsePacket {
        status = status == null ? LensServerScanStatus.UNAVAILABLE : status;
        targetSignature = safe(targetSignature, MAX_SIGNATURE);
        message = safe(message, MAX_MESSAGE);
        sections = List.copyOf(sections == null ? List.of() : sections.stream()
                .filter(section -> section != null)
                .limit(MAX_SECTIONS)
                .toList());
    }

    public static LensScanResponsePacket of(int requestId, LensServerScanStatus status, String targetSignature,
            List<LensInfoSection> sections, String message) {
        return new LensScanResponsePacket(requestId, status, targetSignature,
                sections == null ? List.of() : sections.stream().map(LensNetworkSection::from).toList(), message);
    }

    public List<LensInfoSection> toSections() {
        return sections.stream().map(LensNetworkSection::toSection).toList();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, LensScanResponsePacket packet) {
        buffer.writeVarInt(packet.requestId());
        buffer.writeEnum(packet.status());
        buffer.writeUtf(packet.targetSignature(), MAX_SIGNATURE);
        buffer.writeVarInt(packet.sections().size());
        for (LensNetworkSection section : packet.sections()) {
            LensNetworkSection.write(buffer, section);
        }
        buffer.writeUtf(packet.message(), MAX_MESSAGE);
    }

    private static LensScanResponsePacket read(FriendlyByteBuf buffer) {
        int requestId = buffer.readVarInt();
        LensServerScanStatus status = buffer.readEnum(LensServerScanStatus.class);
        String targetSignature = buffer.readUtf(MAX_SIGNATURE);
        int count = Math.max(0, Math.min(MAX_SECTIONS, buffer.readVarInt()));
        java.util.ArrayList<LensNetworkSection> sections = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            sections.add(LensNetworkSection.read(buffer));
        }
        String message = buffer.readUtf(MAX_MESSAGE);
        return new LensScanResponsePacket(requestId, status, targetSignature, sections, message);
    }

    private static String safe(String value, int maxLength) {
        String safe = value == null ? "" : value.strip();
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }
}
