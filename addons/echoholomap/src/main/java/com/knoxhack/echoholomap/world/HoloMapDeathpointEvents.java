package com.knoxhack.echoholomap.world;

import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.network.HoloMapSync;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

public final class HoloMapDeathpointEvents {
    private HoloMapDeathpointEvents() {
    }

    public static void onPlayerDeath(Object event) {
        if (!deathpointsEnabled() || !(entity(event) instanceof ServerPlayer player)
                || player.level().getServer() == null) {
            return;
        }
        HoloMapWaypointSavedData.get(player.level().getServer())
                .recordDeathpoint(player, maxDeathpointsPerPlayer());
        HoloMapSync.send(player);
    }

    public static void onPlayerLoggedIn(Object event) {
        if (entity(event) instanceof ServerPlayer player) {
            HoloMapSync.send(player);
        }
    }

    public static void onPlayerRespawn(Object event) {
        if (entity(event) instanceof ServerPlayer player) {
            HoloMapSync.send(player);
        }
    }

    public static boolean deathpointsEnabled() {
        try {
            return Config.DEATHPOINTS_ENABLED.get();
        } catch (RuntimeException exception) {
            return true;
        }
    }

    public static int maxDeathpointsPerPlayer() {
        try {
            return Math.max(0, Math.min(128, Config.DEATHPOINTS_MAX_PER_PLAYER.get()));
        } catch (RuntimeException exception) {
            return 10;
        }
    }

    private static Entity entity(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object entity = event.getClass().getMethod("getEntity").invoke(event);
            return entity instanceof Entity value ? value : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
