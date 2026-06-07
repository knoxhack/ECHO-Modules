package com.knoxhack.echospellcore.network;

import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import com.knoxhack.echospellcore.EchoSpellCore;
import com.knoxhack.echospellcore.entity.SpellProjectileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record SpellProjectileSyncPacket(int entityId, int kind, Identifier spellId,
        double x, double y, double z, double velocityX, double velocityY, double velocityZ, int life)
        implements CustomPacketPayload {
    public static final Identifier ID = EchoSpellCore.id("spell_projectile_sync");
    public static final Type<SpellProjectileSyncPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, SpellProjectileSyncPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.entityId());
                buf.writeVarInt(packet.kind());
                EchoPayloadCodecs.writeIdentifier(buf, packet.spellId());
                buf.writeDouble(packet.x());
                buf.writeDouble(packet.y());
                buf.writeDouble(packet.z());
                buf.writeDouble(packet.velocityX());
                buf.writeDouble(packet.velocityY());
                buf.writeDouble(packet.velocityZ());
                buf.writeVarInt(packet.life());
            },
            buf -> new SpellProjectileSyncPacket(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    EchoPayloadCodecs.readIdentifier(buf),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readVarInt()));

    public static SpellProjectileSyncPacket from(SpellProjectileEntity projectile, Identifier spellId) {
        Vec3 velocity = projectile.getDeltaMovement();
        return new SpellProjectileSyncPacket(
                projectile.getId(),
                projectile.projectileKind().ordinal(),
                spellId,
                projectile.getX(),
                projectile.getY(),
                projectile.getZ(),
                velocity.x,
                velocity.y,
                velocity.z,
                projectile.remainingLife());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
