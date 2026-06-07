package com.knoxhack.echoplayercore.api;

import com.knoxhack.echoplayercore.config.PlayerCoreConfig;
import com.knoxhack.echoplayercore.data.HomeLocation;
import com.knoxhack.echoplayercore.data.PlayerCoreSavedData;
import com.knoxhack.echoplayercore.data.TeleportLocation;
import com.knoxhack.echoplayercore.service.CooldownService;
import com.knoxhack.echoplayercore.teleport.TeleportAction;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class EchoPlayerCoreApi {
    private EchoPlayerCoreApi() {
    }

    public static Optional<HomeLocation> getHome(ServerPlayer player, String name) {
        if (player == null) {
            return Optional.empty();
        }
        return PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld())
                .getOrCreate(player.getUUID())
                .home(name);
    }

    public static Collection<HomeLocation> getHomes(ServerPlayer player) {
        if (player == null) {
            return java.util.List.of();
        }
        return PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld())
                .getOrCreate(player.getUUID())
                .homes()
                .values();
    }

    public static boolean setHome(ServerPlayer player, String name) {
        if (player == null || !PlayerCoreConfig.homesEnabled()) {
            return false;
        }
        String clean = HomeLocation.cleanName(name);
        if (!HomeLocation.validName(clean)) {
            return false;
        }
        long now = System.currentTimeMillis();
        HomeLocation home = new HomeLocation(
                clean,
                player.level().dimension(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                now, now
        );
        var data = PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld()).getOrCreate(player.getUUID());
        int limit = maxHomes(player);
        if (!data.home(clean).isPresent() && data.homeCount() >= limit) {
            return false;
        }
        data.setHome(home);
        PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld()).markDirty();
        return true;
    }

    public static boolean deleteHome(ServerPlayer player, String name) {
        if (player == null || !PlayerCoreConfig.homesEnabled()) {
            return false;
        }
        String clean = HomeLocation.cleanName(name);
        boolean removed = PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld())
                .getOrCreate(player.getUUID())
                .deleteHome(clean);
        if (removed) {
            PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld()).markDirty();
        }
        return removed;
    }

    public static Optional<TeleportLocation> getBackLocation(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        return PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld())
                .getOrCreate(player.getUUID())
                .lastBackLocation();
    }

    public static void setBackLocation(ServerPlayer player, TeleportLocation location) {
        if (player == null || location == null) {
            return;
        }
        PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld())
                .getOrCreate(player.getUUID())
                .setLastBackLocation(location);
        PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld()).markDirty();
    }

    public static Optional<TeleportLocation> getLastDeathLocation(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        return PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld())
                .getOrCreate(player.getUUID())
                .lastDeathLocation();
    }

    public static void setLastRtpLocation(ServerPlayer player, TeleportLocation location) {
        if (player == null || location == null) {
            return;
        }
        PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld())
                .getOrCreate(player.getUUID())
                .setLastRtpLocation(location);
        PlayerCoreSavedData.get((ServerLevel) player.level().getServer().overworld()).markDirty();
    }

    public static boolean teleportHome(ServerPlayer player, String name) {
        return com.knoxhack.echoplayercore.command.PlayerCoreCommands.home(player, name) == 1;
    }

    public static boolean teleportSpawn(ServerPlayer player) {
        return com.knoxhack.echoplayercore.command.PlayerCoreCommands.spawn(player) == 1;
    }

    public static boolean teleportBack(ServerPlayer player) {
        return com.knoxhack.echoplayercore.command.PlayerCoreCommands.back(player) == 1;
    }

    public static boolean randomTeleport(ServerPlayer player) {
        return com.knoxhack.echoplayercore.command.PlayerCoreCommands.rtp(player) == 1;
    }

    public static boolean isOnCooldown(ServerPlayer player, TeleportAction action) {
        return CooldownService.isOnCooldown(player, action);
    }

    public static long getCooldownRemaining(ServerPlayer player, TeleportAction action) {
        return CooldownService.getCooldownRemaining(player, action);
    }

    private static int maxHomes(ServerPlayer player) {
        if (PlayerCoreConfig.opsBypassHomeLimit() && player.createCommandSourceStack().permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
            return PlayerCoreConfig.maxHomesOp();
        }
        return PlayerCoreConfig.maxHomesDefault();
    }
}
