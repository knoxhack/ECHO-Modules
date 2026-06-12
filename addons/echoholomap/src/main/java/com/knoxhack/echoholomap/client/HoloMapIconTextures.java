package com.knoxhack.echoholomap.client;

import com.echoplatform.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.network.HoloMapSnapshotPacket;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint.Scope;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

final class HoloMapIconTextures {
    static final Identifier SELECTED_RING = icon("selected_ring");
    static final Identifier EDGE_INDICATOR = icon("edge_indicator");
    static final Identifier PLAYER = icon("player");

    private static final Identifier CRASH_SITE = icon("crash_site");
    private static final Identifier ROUTE = icon("route");
    private static final Identifier HAZARD = icon("hazard");
    private static final Identifier MISSION = icon("mission");
    private static final Identifier BASE_OUTPOST = icon("base_outpost");
    private static final Identifier ORBITAL_SCAN = icon("orbital_scan");
    private static final Identifier NEXUS_ANOMALY = icon("nexus_anomaly");
    private static final Identifier DRONE_SCAN = icon("drone_scan");
    private static final Identifier REGION = icon("region");
    private static final Identifier GENERIC = icon("generic");
    private static final Identifier WAYPOINT_LOCAL = icon("waypoint_local");
    private static final Identifier WAYPOINT_PERSONAL = icon("waypoint_personal");
    private static final Identifier WAYPOINT_SHARED = icon("waypoint_shared");
    private static final Identifier DEATHPOINT = icon("deathpoint");
    private static final Identifier LOCKED = icon("locked");
    private static final Identifier CHECKED = icon("checked");
    private static final Map<Identifier, Boolean> RESOURCE_CACHE = new ConcurrentHashMap<>();
    private static volatile ResourceManager cachedResourceManager;

    private HoloMapIconTextures() {
    }

    static Identifier marker(HoloMapSnapshotPacket.MarkerData marker) {
        if (marker == null) {
            return GENERIC;
        }
        if (marker.state() == IMapMarker.MarkerState.LOCKED) {
            return LOCKED;
        }
        if (marker.state() == IMapMarker.MarkerState.CHECKED) {
            return CHECKED;
        }
        Identifier custom = texture(marker.icon());
        if (custom != null) {
            return custom;
        }
        return markerKind(marker.kind());
    }

    static Identifier markerKind(IMapMarker.MarkerKind kind) {
        return switch (kind == null ? IMapMarker.MarkerKind.GENERIC : kind) {
            case CRASH_SITE -> CRASH_SITE;
            case ROUTE -> ROUTE;
            case HAZARD -> HAZARD;
            case MISSION -> MISSION;
            case BASE_OUTPOST -> BASE_OUTPOST;
            case ORBITAL_SCAN -> ORBITAL_SCAN;
            case NEXUS_ANOMALY -> NEXUS_ANOMALY;
            case DRONE_SCAN -> DRONE_SCAN;
            case REGION -> REGION;
            case GENERIC, STRUCTURE, FACTION -> GENERIC;
        };
    }

    static Identifier markerState(IMapMarker.MarkerKind kind, IMapMarker.MarkerState state) {
        if (state == IMapMarker.MarkerState.LOCKED) {
            return LOCKED;
        }
        if (state == IMapMarker.MarkerState.CHECKED) {
            return CHECKED;
        }
        return markerKind(kind);
    }

    static Identifier waypoint(HoloMapWaypoint waypoint) {
        if (waypoint != null && waypoint.isDeathpoint()) {
            return DEATHPOINT;
        }
        return waypointScope(waypoint == null ? Scope.LOCAL : waypoint.scope());
    }

    static Identifier waypointScope(Scope scope) {
        return switch (scope == null ? Scope.LOCAL : scope) {
            case SHARED -> WAYPOINT_SHARED;
            case PERSONAL -> WAYPOINT_PERSONAL;
            case LOCAL -> WAYPOINT_LOCAL;
        };
    }

    static boolean available(Identifier texture) {
        if (texture == null) {
            return false;
        }
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (cachedResourceManager != resourceManager) {
            RESOURCE_CACHE.clear();
            cachedResourceManager = resourceManager;
        }
        return RESOURCE_CACHE.computeIfAbsent(texture, key -> resourceAvailable(resourceManager, key));
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath(EchoHoloMap.MODID,
                "textures/gui/holomap/icons/" + name + ".png");
    }

    private static Identifier texture(Identifier requested) {
        if (requested == null) {
            return null;
        }
        String path = requested.getPath();
        if (!path.startsWith("textures/")) {
            return null;
        }
        Identifier texture = path.endsWith(".png")
                ? requested
                : Identifier.fromNamespaceAndPath(requested.getNamespace(), path + ".png");
        return available(texture) ? texture : null;
    }

    private static boolean resourceAvailable(ResourceManager resourceManager, Identifier texture) {
        try {
            return resourceManager.getResource(texture).isPresent();
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }
}
