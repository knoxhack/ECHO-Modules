package com.knoxhack.echonetcore.api;

import com.knoxhack.echocore.api.network.EchoPacketDirection;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.network.EchoNetDebug;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class EchoServerActionGuards {
    private EchoServerActionGuards() {
    }

    public static GuardResult<ServerPlayer> requireOp(ServerPlayer player) {
        if (player == null) {
            return reject("missing-player");
        }
        if (!player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return reject("permission-denied");
        }
        return accept(player);
    }

    public static GuardResult<BlockPos> requireWithin(ServerPlayer player, BlockPos pos, double maxDistanceSq) {
        if (player == null) {
            return reject("missing-player");
        }
        if (pos == null) {
            return reject("missing-position");
        }
        double distanceSq = player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (distanceSq > Math.max(0.0D, maxDistanceSq)) {
            return reject("too-far");
        }
        return accept(pos);
    }

    public static <T extends BlockEntity> GuardResult<T> requireLoadedBlockEntity(
            ServerPlayer player, BlockPos pos, Class<T> type) {
        if (player == null) {
            return reject("missing-player");
        }
        if (pos == null) {
            return reject("missing-position");
        }
        if (type == null) {
            return reject("missing-block-entity-type");
        }
        if (!player.level().isLoaded(pos)) {
            return reject("chunk-not-loaded");
        }
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!type.isInstance(blockEntity)) {
            return reject("wrong-block-entity");
        }
        return accept(type.cast(blockEntity));
    }

    public static GuardResult<Level> requireSameLevel(ServerPlayer player, Level level) {
        if (player == null) {
            return reject("missing-player");
        }
        if (level == null) {
            return reject("missing-level");
        }
        if (player.level() != level) {
            return reject("wrong-level");
        }
        return accept(level);
    }

    public static <T> GuardResult<T> accept(T value) {
        return new GuardResult<>(true, "", value);
    }

    public static <T> GuardResult<T> reject(String reason) {
        return new GuardResult<>(false, reason == null || reason.isBlank() ? "rejected" : reason, null);
    }

    public static void logRejected(CustomPacketPayload.Type<?> type, ServerPlayer player, GuardResult<?> result) {
        if (type == null || result == null || result.accepted()) {
            return;
        }
        EchoNetDebug.emit(type.id(), EchoPacketDirection.SERVERBOUND, EchoPacketKind.SERVERBOUND_ACTION,
                player == null ? "" : player.getScoreboardName(), false, result.reason());
    }

    public record GuardResult<T>(boolean accepted, String reason, T value) {
        public GuardResult {
            reason = reason == null ? "" : reason;
        }

        public boolean rejected() {
            return !accepted;
        }
    }
}
