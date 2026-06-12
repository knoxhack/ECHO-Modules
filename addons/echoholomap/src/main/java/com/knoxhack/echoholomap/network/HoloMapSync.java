package com.knoxhack.echoholomap.network;

import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.integration.HoloMapMissionHooks;
import com.knoxhack.echoholomap.integration.HoloMapSoundHooks;
import com.knoxhack.echoholomap.integration.runtimeguard.HoloMapRuntimeGuardHooks;
import com.knoxhack.echoholomap.api.HoloMapOverlayKind;
import net.minecraft.server.level.ServerPlayer;

public final class HoloMapSync {
    private HoloMapSync() {
    }

    public static void send(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            HoloMapSnapshotPacket snapshot = HoloMapSnapshotPacket.from(player);
            HoloMapWaypointSyncPacket waypoints = HoloMapWaypointSyncPacket.from(player);
            HoloMapRuntimeGuardHooks.recordSync("snapshot", estimateSnapshotBytes(snapshot), "UI_OPEN");
            HoloMapRuntimeGuardHooks.recordSync("waypoint_sync", estimateWaypointBytes(waypoints), "UI_OPEN");
            EchoNetSend.toPlayer(player, snapshot, EchoPacketKind.CLIENTBOUND_SYNC);
            EchoNetSend.toPlayer(player, waypoints, EchoPacketKind.CLIENTBOUND_SYNC);
            if (!snapshot.markers().isEmpty() || !snapshot.routes().isEmpty() || !waypoints.waypoints().isEmpty()) {
                HoloMapMissionHooks.recordRouteSynced(player,
                        snapshot.markers().size() + snapshot.routes().size() + waypoints.waypoints().size());
            }
            if (!snapshot.routes().isEmpty()) {
                HoloMapSoundHooks.play(player, HoloMapSoundHooks.ROUTE_SYNC);
            }
            if (snapshot.overlays().stream().anyMatch(overlay -> overlay.kind() == HoloMapOverlayKind.HAZARD)) {
                HoloMapSoundHooks.play(player, HoloMapSoundHooks.HAZARD_OVERLAY);
            }
        } catch (RuntimeException exception) {
            EchoHoloMap.LOGGER.debug("Skipped optional HoloMap sync for {}: {}",
                    player.getName().getString(), exception.getMessage());
        }
    }

    private static int estimateSnapshotBytes(HoloMapSnapshotPacket packet) {
        if (packet == null) {
            return 0;
        }
        return 64
                + packet.layers().size() * 80
                + packet.markers().size() * 208
                + packet.routes().size() * 256
                + packet.overlays().size() * 128
                + packet.zones().size() * 192
                + packet.diagnostics().size() * 96;
    }

    private static int estimateWaypointBytes(HoloMapWaypointSyncPacket packet) {
        if (packet == null) {
            return 0;
        }
        return 32 + packet.waypoints().size() * 96;
    }
}
