package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.boss.BossHudProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BossNavigationPacket(
        boolean active,
        String bossId,
        String title,
        String subtitle,
        String dimension,
        int x,
        int y,
        int z,
        int phase,
        float healthPercent,
        int accentColor,
        String compassLabel,
        String category,
        String targetKind
) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "boss_navigation");
    public static final Type<BossNavigationPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, BossNavigationPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBoolean(packet.active);
                buf.writeUtf(packet.bossId);
                buf.writeUtf(packet.title);
                buf.writeUtf(packet.subtitle);
                buf.writeUtf(packet.dimension);
                buf.writeInt(packet.x);
                buf.writeInt(packet.y);
                buf.writeInt(packet.z);
                buf.writeInt(packet.phase);
                buf.writeFloat(packet.healthPercent);
                buf.writeInt(packet.accentColor);
                buf.writeUtf(packet.compassLabel);
                buf.writeUtf(packet.category);
                buf.writeUtf(packet.targetKind);
            },
            buf -> new BossNavigationPacket(
                    buf.readBoolean(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readFloat(),
                    buf.readInt(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf()
            )
    );

    public static BossNavigationPacket active(BossHudProfile profile, String dimension, BlockPos pos,
                                              int phase, float healthPercent, String titleOverride) {
        return active(profile, dimension, pos, phase, healthPercent, titleOverride, phase <= 0 ? "ENTRANCE" : "LIVE");
    }

    public static BossNavigationPacket active(BossHudProfile profile, String dimension, BlockPos pos,
                                              int phase, float healthPercent, String titleOverride, String targetKind) {
        String title = titleOverride == null || titleOverride.isBlank() ? profile.title() : titleOverride;
        return new BossNavigationPacket(
                true,
                profile.bossId(),
                title,
                profile.subtitle(),
                dimension,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                Math.max(0, phase),
                Math.max(0.0F, Math.min(1.0F, healthPercent)),
                profile.accentColor(),
                profile.compassLabel(),
                profile.category().name(),
                targetKind == null || targetKind.isBlank() ? "LIVE" : targetKind
        );
    }

    public static BossNavigationPacket inactive() {
        return inactive("");
    }

    public static BossNavigationPacket inactive(String bossId) {
        return new BossNavigationPacket(false, bossId == null ? "" : bossId, "", "", "", 0, 0, 0,
                0, 0.0F, 0, "", "", "");
    }

    public BlockPos position() {
        return new BlockPos(x, y, z);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
