package com.knoxhack.echoholomap.network;

import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint.Scope;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class HoloMapWaypointClientState {
    private static volatile List<HoloMapWaypoint> serverWaypoints = List.of();
    private static volatile List<HoloMapWaypoint> localWaypoints = List.of();
    private static volatile long lastSyncGameTime = 0L;
    private static volatile long revision = 0L;

    private HoloMapWaypointClientState() {
    }

    public static synchronized void apply(HoloMapWaypointSyncPacket packet) {
        if (packet == null) {
            return;
        }
        serverWaypoints = packet.waypoints();
        lastSyncGameTime = packet.gameTime();
        revision++;
    }

    public static synchronized void setLocalWaypoints(List<HoloMapWaypoint> waypoints) {
        localWaypoints = copy(waypoints, Scope.LOCAL);
        revision++;
    }

    public static synchronized void upsertLocal(HoloMapWaypoint waypoint) {
        if (waypoint == null || waypoint.scope() != Scope.LOCAL) {
            return;
        }
        Map<String, HoloMapWaypoint> merged = localMap();
        merged.put(waypoint.id().toString(), waypoint);
        localWaypoints = sorted(merged.values().stream().toList());
        revision++;
    }

    public static synchronized boolean removeLocal(Identifier id) {
        if (id == null) {
            return false;
        }
        Map<String, HoloMapWaypoint> merged = localMap();
        boolean removed = merged.remove(id.toString()) != null;
        if (removed) {
            localWaypoints = sorted(merged.values().stream().toList());
            revision++;
        }
        return removed;
    }

    public static synchronized List<HoloMapWaypoint> localWaypoints() {
        return localWaypoints;
    }

    public static synchronized List<HoloMapWaypoint> serverWaypoints() {
        return serverWaypoints;
    }

    public static synchronized List<HoloMapWaypoint> waypoints() {
        Map<String, HoloMapWaypoint> merged = new LinkedHashMap<>();
        for (HoloMapWaypoint waypoint : localWaypoints) {
            merged.put(waypoint.id().toString(), waypoint);
        }
        for (HoloMapWaypoint waypoint : serverWaypoints) {
            merged.putIfAbsent(waypoint.id().toString(), waypoint);
        }
        return sorted(new ArrayList<>(merged.values()));
    }

    public static synchronized long lastSyncGameTime() {
        return lastSyncGameTime;
    }

    public static synchronized long revision() {
        return revision;
    }

    public static synchronized void clearForTests() {
        serverWaypoints = List.of();
        localWaypoints = List.of();
        lastSyncGameTime = 0L;
        revision++;
    }

    private static List<HoloMapWaypoint> copy(List<HoloMapWaypoint> waypoints, Scope requiredScope) {
        if (waypoints == null || waypoints.isEmpty()) {
            return List.of();
        }
        return sorted(waypoints.stream()
                .filter(waypoint -> waypoint != null && (requiredScope == null || waypoint.scope() == requiredScope))
                .toList());
    }

    private static List<HoloMapWaypoint> sorted(List<HoloMapWaypoint> waypoints) {
        return waypoints.stream()
                .sorted(Comparator.comparing(HoloMapWaypoint::scope)
                        .thenComparing(waypoint -> !waypoint.isDeathpoint())
                        .thenComparing(HoloMapWaypoint::title, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(waypoint -> waypoint.id().toString()))
                .toList();
    }

    private static Map<String, HoloMapWaypoint> localMap() {
        Map<String, HoloMapWaypoint> map = new LinkedHashMap<>();
        for (HoloMapWaypoint waypoint : localWaypoints) {
            map.put(waypoint.id().toString(), waypoint);
        }
        return map;
    }
}
