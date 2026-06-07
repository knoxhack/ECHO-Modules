package com.knoxhack.echoplayercore.service;

import com.knoxhack.echoplayercore.data.WarpLocation;
import com.knoxhack.echoplayercore.data.WarpSavedData;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WarpService {
    private WarpService() {
    }

    public static Optional<WarpLocation> getWarp(ServerLevel level, String id) {
        return WarpSavedData.get(level).getWarp(id);
    }

    public static List<WarpLocation> listWarps(ServerLevel level) {
        return WarpSavedData.get(level).warpList();
    }

    public static boolean setWarp(ServerLevel level, WarpLocation warp) {
        return WarpSavedData.get(level).setWarp(warp);
    }

    public static boolean deleteWarp(ServerLevel level, String id) {
        return WarpSavedData.get(level).deleteWarp(id);
    }

    public static boolean canSetWarp(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return player.createCommandSourceStack().permissions().hasPermission(
                net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean canDeleteWarp(ServerPlayer player) {
        return canSetWarp(player);
    }
}
