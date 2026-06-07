package com.knoxhack.echoholomap.api;

import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.world.HoloMapWaypointSavedData;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class HoloMapWaypointApi {
    private HoloMapWaypointApi() {
    }

    public static boolean upsert(ServerPlayer actor, HoloMapWaypoint waypoint, boolean mayEditShared) {
        if (actor == null || waypoint == null) {
            return false;
        }
        MinecraftServer server = actor.level().getServer();
        return server != null && HoloMapWaypointSavedData.get(server).upsert(actor, waypoint, mayEditShared);
    }

    public static boolean delete(ServerPlayer actor, Identifier waypointId, boolean mayEditShared) {
        if (actor == null || waypointId == null) {
            return false;
        }
        MinecraftServer server = actor.level().getServer();
        return server != null && HoloMapWaypointSavedData.get(server).delete(actor, waypointId, mayEditShared);
    }

    public static HoloMapWaypoint recordDeathpoint(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        MinecraftServer server = player.level().getServer();
        return server == null ? null : HoloMapWaypointSavedData.get(server).recordDeathpoint(player, deathpointLimit());
    }

    public static List<HoloMapWaypoint> visibleWaypoints(ServerPlayer player, int limit) {
        if (player == null) {
            return List.of();
        }
        MinecraftServer server = player.level().getServer();
        return server == null ? List.of() : HoloMapWaypointSavedData.get(server).waypointsFor(player, limit);
    }

    public static int waypointCount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        MinecraftServer server = player.level().getServer();
        return server == null ? 0 : HoloMapWaypointSavedData.get(server).countFor(player.getUUID());
    }

    public static boolean canEdit(HoloMapWaypoint waypoint, ServerPlayer actor, boolean mayEditShared) {
        if (waypoint == null || actor == null) {
            return false;
        }
        return switch (waypoint.scope()) {
            case SHARED -> mayEditShared;
            case PERSONAL -> waypoint.owner().equals(actor.getUUID());
            case LOCAL -> true;
        };
    }

    private static int deathpointLimit() {
        try {
            return Math.max(0, Math.min(128, Config.DEATHPOINTS_MAX_PER_PLAYER.get()));
        } catch (RuntimeException exception) {
            return 10;
        }
    }
}
