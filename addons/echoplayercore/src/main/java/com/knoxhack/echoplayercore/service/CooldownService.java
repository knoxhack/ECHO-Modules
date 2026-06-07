package com.knoxhack.echoplayercore.service;

import com.knoxhack.echoplayercore.config.PlayerCoreConfig;
import com.knoxhack.echoplayercore.data.PlayerCoreSavedData;
import com.knoxhack.echoplayercore.teleport.TeleportAction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class CooldownService {
    private CooldownService() {
    }

    public static boolean isOnCooldown(ServerPlayer player, TeleportAction action) {
        if (player == null || action == null) {
            return false;
        }
        if (PlayerCoreConfig.opsBypassCooldowns() && player.createCommandSourceStack().permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
            return false;
        }
        long now = System.currentTimeMillis();
        long last = getLastUse(player, action);
        int seconds = cooldownSeconds(action);
        return last > 0 && (now - last) < (seconds * 1000L);
    }

    public static long getCooldownRemaining(ServerPlayer player, TeleportAction action) {
        if (player == null || action == null) {
            return 0L;
        }
        if (PlayerCoreConfig.opsBypassCooldowns() && player.createCommandSourceStack().permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
            return 0L;
        }
        long now = System.currentTimeMillis();
        long last = getLastUse(player, action);
        int seconds = cooldownSeconds(action);
        long remaining = (last + (seconds * 1000L)) - now;
        return Math.max(0L, remaining);
    }

    public static void applyCooldown(ServerPlayer player, TeleportAction action) {
        if (player == null || action == null) {
            return;
        }
        if (PlayerCoreConfig.opsBypassCooldowns() && player.createCommandSourceStack().permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }
        PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld())
                .getOrCreate(player.getUUID())
                .setCooldown(action.cooldownKey(), System.currentTimeMillis());
    }

    public static void clearCooldown(ServerPlayer player, TeleportAction action) {
        if (player == null || action == null) {
            return;
        }
        PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld())
                .getOrCreate(player.getUUID())
                .clearCooldown(action.cooldownKey());
    }

    private static long getLastUse(ServerPlayer player, TeleportAction action) {
        return PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld())
                .getOrCreate(player.getUUID())
                .getCooldown(action.cooldownKey());
    }

    private static int cooldownSeconds(TeleportAction action) {
        return switch (action) {
            case HOME -> 0; // no cooldown for home by default
            case RTP -> PlayerCoreConfig.rtpCooldownSeconds();
            case BACK -> PlayerCoreConfig.backCooldownSeconds();
            case SPAWN -> PlayerCoreConfig.spawnCooldownSeconds();
            case TPA, TPA_HERE -> PlayerCoreConfig.tpaCooldownSeconds();
            default -> 0;
        };
    }
}
