package com.knoxhack.echomultiblockcore.network;

import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echomultiblockcore.Config;
import com.knoxhack.echomultiblockcore.api.RobotPoseSnapshot;
import com.knoxhack.echonetcore.api.EchoNetSend;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class RobotAnimationSync {
    private RobotAnimationSync() {
    }

    public static void play(ServerLevel level, BlockPos controllerPos, BlockPos robotPos, String animationId,
            BlockPos targetPos, int durationTicks, Identifier taskId) {
        if (level == null || robotPos == null || !animationsEnabled()) {
            return;
        }
        RobotAnimationPacket packet = new RobotAnimationPacket(controllerPos, robotPos, animationId, targetPos, durationTicks, taskId);
        send(level, robotPos, packet);
    }

    public static void play(ServerLevel level, BlockPos controllerPos, BlockPos robotPos, String animationId,
            BlockPos targetPos, int durationTicks, Identifier taskId, Identifier animationProfile, RobotPoseSnapshot pose, int flags) {
        if (level == null || robotPos == null || !animationsEnabled()) {
            return;
        }
        RobotAnimationPacket packet = new RobotAnimationPacket(controllerPos, robotPos, animationId, targetPos, durationTicks,
                taskId, RobotAnimationPacket.CURRENT_VERSION, animationProfile, pose, flags);
        send(level, robotPos, packet);
    }

    private static void send(ServerLevel level, BlockPos robotPos, RobotAnimationPacket packet) {
        EchoNetSend.toPlayers(level.players().stream()
                .filter(player -> closeEnough(player, robotPos))
                .toList(), packet, EchoPacketKind.CLIENTBOUND_SYNC);
    }

    private static boolean closeEnough(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < 4096.0D;
    }

    private static boolean animationsEnabled() {
        try {
            return Config.ENABLE_ROBOTIC_ANIMATIONS.get();
        } catch (RuntimeException exception) {
            return true;
        }
    }
}
