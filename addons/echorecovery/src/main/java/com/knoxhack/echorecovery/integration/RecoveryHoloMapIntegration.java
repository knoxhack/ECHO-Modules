package com.knoxhack.echorecovery.integration;

import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.world.HoloMapWaypointSavedData;
import com.knoxhack.echorecovery.EchoRecovery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class RecoveryHoloMapIntegration {
    private RecoveryHoloMapIntegration() {}

    public static void registerCommon() {
        EchoRecovery.LOGGER.info("HoloMap integration registered.");
    }

    public static void onGraveCreated(ServerPlayer player, BlockPos pos, String graveId) {
        if (player == null || pos == null || player.level().getServer() == null) {
            return;
        }
        try {
            HoloMapWaypointSavedData data = HoloMapWaypointSavedData.get(player.level().getServer());
            String dim = player.level().dimension().identifier().toString();
            long time = player.level().getServer().overworld().getGameTime();
            HoloMapWaypoint waypoint = new HoloMapWaypoint(
                waypointId(player, graveId),
                player.getUUID(),
                HoloMapWaypoint.Scope.PERSONAL,
                dim,
                pos.getX(), pos.getY(), pos.getZ(),
                "Recovery Cache " + shortId(graveId),
                0xFFFF6666,
                "recovery_grave",
                true,
                time,
                time
            );
            data.upsert(player, waypoint, false);
        } catch (Exception e) {
            EchoRecovery.LOGGER.error("Failed to create HoloMap grave waypoint", e);
        }
    }

    public static void onGraveRecovered(ServerPlayer player, BlockPos pos, String graveId) {
        deleteWaypoint(player, graveId);
    }

    public static void onGraveDeleted(ServerPlayer player, BlockPos pos, String graveId) {
        deleteWaypoint(player, graveId);
    }

    private static void deleteWaypoint(ServerPlayer player, String graveId) {
        if (player == null || player.level().getServer() == null) {
            return;
        }
        try {
            HoloMapWaypointSavedData data = HoloMapWaypointSavedData.get(player.level().getServer());
            data.delete(player, waypointId(player, graveId), false);
        } catch (Exception e) {
            EchoRecovery.LOGGER.debug("Failed to delete HoloMap grave waypoint: {}", e.getMessage());
        }
    }

    private static Identifier waypointId(ServerPlayer player, String graveId) {
        String id = graveId == null || graveId.isBlank() ? "unknown" : graveId.toLowerCase(java.util.Locale.ROOT);
        return Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "grave/" + player.getUUID() + "/" + id);
    }

    private static String shortId(String graveId) {
        if (graveId == null || graveId.isBlank()) {
            return "unknown";
        }
        return graveId.substring(0, Math.min(8, graveId.length()));
    }
}
