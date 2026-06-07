package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.block.EmergencyBunkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

public final class EmergencyBunkRespawnEvents {
    private EmergencyBunkRespawnEvents() {
    }

    public static void onPlayerRespawnPosition(Object event) {
        ServerPlayer player = playerFrom(event);
        if (player == null) {
            return;
        }
        ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
        if (respawnConfig == null) {
            return;
        }

        if (!(player.level() instanceof ServerLevel currentLevel)) {
            return;
        }
        ServerLevel level = currentLevel.getServer().getLevel(respawnConfig.respawnData().dimension());
        if (level == null) {
            return;
        }

        BlockPos savedPos = respawnConfig.respawnData().pos();
        BlockState savedState = level.getBlockState(savedPos);
        if (!(savedState.getBlock() instanceof EmergencyBunkBlock)) {
            return;
        }

        EmergencyBunkBlock.resolveRespawnPosition(
                        savedState,
                        EntityType.PLAYER,
                        level,
                        savedPos,
                        respawnConfig.respawnData().yaw())
                .ifPresent(respawn -> {
                    BlockPos headPos = EmergencyBunkBlock.headPos(savedState, savedPos);
                    TeleportTransition current = teleportTransitionFrom(event);
                    if (current == null) {
                        return;
                    }
                    setTeleportTransition(event, new TeleportTransition(
                            level,
                            respawn.position(),
                            Vec3.ZERO,
                            respawn.yaw(),
                            respawn.pitch(),
                            current.postTeleportTransition()));
                    player.setRespawnPosition(
                            new ServerPlayer.RespawnConfig(
                                    LevelData.RespawnData.of(
                                            level.dimension(),
                                            headPos,
                                            respawnConfig.respawnData().yaw(),
                                            respawnConfig.respawnData().pitch()),
                                    false),
                            false);
                    setCopyOriginalSpawnPosition(event);
                });
    }

    private static ServerPlayer playerFrom(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object value = event.getClass().getMethod("getEntity").invoke(event);
            return value instanceof ServerPlayer player ? player : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static TeleportTransition teleportTransitionFrom(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object value = event.getClass().getMethod("getTeleportTransition").invoke(event);
            return value instanceof TeleportTransition transition ? transition : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static void setTeleportTransition(Object event, TeleportTransition transition) {
        try {
            event.getClass().getMethod("setTeleportTransition", TeleportTransition.class)
                    .invoke(event, transition);
        } catch (ReflectiveOperationException ignored) {
            // Unsupported backend event; leave the original spawn transition unchanged.
        }
    }

    private static void setCopyOriginalSpawnPosition(Object event) {
        try {
            event.getClass().getMethod("setCopyOriginalSpawnPosition", boolean.class)
                    .invoke(event, true);
        } catch (ReflectiveOperationException ignored) {
            // Unsupported backend event; the respawn position was still updated on the player.
        }
    }
}
