package com.knoxhack.echorecovery.net;

import com.knoxhack.echonetcore.api.EchoServerActionGuards;
import com.knoxhack.echonetcore.api.EchoPayloadContext;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.block.entity.GraveBlockEntity;
import com.knoxhack.echorecovery.grave.GraveAccessResult;
import com.knoxhack.echorecovery.grave.GraveManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record RecoverAllPacket(BlockPos gravePos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RecoverAllPacket> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "recover_all"));

    public static final StreamCodec<ByteBuf, RecoverAllPacket> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, RecoverAllPacket::gravePos,
        RecoverAllPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RecoverAllPacket packet, EchoPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                handle(packet, player, context);
            }
        });
    }

    public static void handle(RecoverAllPacket packet, ServerPlayer player, EchoPayloadContext context) {
        EchoServerActionGuards.GuardResult<BlockPos> nearby =
                EchoServerActionGuards.requireWithin(player, packet.gravePos(), 64.0D);
        if (nearby.rejected()) {
            EchoServerActionGuards.logRejected(TYPE, player, nearby);
            player.sendSystemMessage(Component.literal("Recovery failed: grave is too far away."));
            return;
        }
        EchoServerActionGuards.GuardResult<GraveBlockEntity> graveResult =
                EchoServerActionGuards.requireLoadedBlockEntity(player, packet.gravePos(), GraveBlockEntity.class);
        if (graveResult.rejected()) {
            EchoServerActionGuards.logRejected(TYPE, player, graveResult);
            player.sendSystemMessage(Component.literal("Recovery failed: grave is unavailable."));
            return;
        }
        GraveBlockEntity grave = graveResult.value();
        boolean admin = EchoServerActionGuards.requireOp(player).accepted();
        GraveAccessResult result = GraveManager.accessGrave(grave, player, admin);
        if (result == GraveAccessResult.ALLOWED) {
            if (!GraveManager.recoverGrave(grave, player)) {
                player.sendSystemMessage(Component.literal("Recovery incomplete: make room or enable overflow drops."));
            }
        } else {
            EchoServerActionGuards.logRejected(TYPE, player, EchoServerActionGuards.reject("access-denied"));
            player.sendSystemMessage(Component.literal("Recovery failed: access denied."));
        }
    }
}
