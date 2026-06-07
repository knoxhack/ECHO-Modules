package com.knoxhack.echoholomap.map;

import com.knoxhack.echoholomap.HoloMapIds;
import com.knoxhack.echoholomap.api.HoloMapChunkActionResult;
import com.knoxhack.echoholomap.api.HoloMapChunkSelection;
import com.knoxhack.echoholomap.api.IHoloMapChunkActionProvider;
import com.knoxhack.echoholomap.integration.HoloMapSoundHooks;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint.Scope;
import com.knoxhack.echoholomap.world.HoloMapWaypointSavedData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public enum BuiltinHoloMapChunkActionProvider implements IHoloMapChunkActionProvider {
    INSTANCE;

    public static final Identifier PROVIDER_ID = HoloMapIds.id("chunk_actions/waypoints");
    public static final Identifier CREATE_PERSONAL_WAYPOINT =
            HoloMapIds.id("action/create_personal_waypoint");

    @Override
    public Identifier providerId() {
        return PROVIDER_ID;
    }

    @Override
    public HoloMapChunkActionResult handle(ServerPlayer player, HoloMapChunkSelection selection, Identifier actionId) {
        if (player == null || selection == null) {
            return HoloMapChunkActionResult.failure("Waypoint Failed", "No player or mapped chunk was available.");
        }
        if (!CREATE_PERSONAL_WAYPOINT.equals(actionId)) {
            return HoloMapChunkActionResult.failure("Waypoint Failed", "Unknown HoloMap chunk action " + actionId + ".");
        }
        if (player.level().getServer() == null) {
            return HoloMapChunkActionResult.failure("Waypoint Failed", "Waypoint storage is not available.");
        }

        long now = player.level().getGameTime();
        HoloMapWaypoint waypoint = new HoloMapWaypoint(
                waypointId(player, selection),
                player.getUUID(),
                Scope.PERSONAL,
                selection.dimensionId().toString(),
                selection.centerX(),
                Math.floor(player.getY()),
                selection.centerZ(),
                "Mapped chunk " + selection.chunkX() + ", " + selection.chunkZ(),
                0xFF66E9A6,
                "diamond",
                true,
                now,
                now);
        boolean changed = HoloMapWaypointSavedData.get(player.level().getServer()).upsert(player, waypoint, false);
        if (!changed) {
            return HoloMapChunkActionResult.failure("Waypoint Failed", "The selected chunk could not be saved.");
        }
        HoloMapSoundHooks.play(player, HoloMapSoundHooks.WAYPOINT_CREATE);
        return HoloMapChunkActionResult.success("Waypoint Saved",
                "Personal waypoint added at chunk " + selection.chunkX() + ", " + selection.chunkZ() + ".");
    }

    private static Identifier waypointId(ServerPlayer player, HoloMapChunkSelection selection) {
        return HoloMapIds.id("waypoint/personal/chunk/"
                + player.getUUID() + "/"
                + selection.dimensionId() + "/"
                + selection.chunkX() + "_" + selection.chunkZ());
    }
}
