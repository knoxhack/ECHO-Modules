package com.knoxhack.echocursecore.network;

import com.knoxhack.echocursecore.EchoCurseCore;
import com.knoxhack.echocursecore.api.CurseCoreApi;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public record CurseHudSyncPacket(
        int echoRotStage,
        int glassVeinsStage,
        int riftHungerStage,
        int soulStaticStage,
        int phantomBurnStage,
        int bloodDebtStage,
        int voidMarkStage,
        int contractCount,
        int cleanseableCount,
        long lastChanged)
        implements CustomPacketPayload {
    public static final Identifier ID = EchoCurseCore.id("curse_hud_sync");
    public static final Type<CurseHudSyncPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, CurseHudSyncPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.echoRotStage());
                buf.writeVarInt(packet.glassVeinsStage());
                buf.writeVarInt(packet.riftHungerStage());
                buf.writeVarInt(packet.soulStaticStage());
                buf.writeVarInt(packet.phantomBurnStage());
                buf.writeVarInt(packet.bloodDebtStage());
                buf.writeVarInt(packet.voidMarkStage());
                buf.writeVarInt(packet.contractCount());
                buf.writeVarInt(packet.cleanseableCount());
                buf.writeLong(packet.lastChanged());
            },
            buf -> new CurseHudSyncPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readLong()));

    public static CurseHudSyncPacket from(Player player) {
        return new CurseHudSyncPacket(
                CurseCoreApi.stage(player, CurseCoreApi.ECHO_ROT),
                CurseCoreApi.stage(player, CurseCoreApi.GLASS_VEINS),
                CurseCoreApi.stage(player, CurseCoreApi.RIFT_HUNGER),
                CurseCoreApi.stage(player, CurseCoreApi.SOUL_STATIC),
                CurseCoreApi.stage(player, CurseCoreApi.PHANTOM_BURN),
                CurseCoreApi.stage(player, CurseCoreApi.BLOOD_DEBT),
                CurseCoreApi.stage(player, CurseCoreApi.VOID_MARK),
                CurseCoreApi.contractCount(player),
                CurseCoreApi.cleanseableCount(player),
                CurseCoreApi.lastChanged(player));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
