package com.knoxhack.echoworldcore.integration;

import com.knoxhack.echocore.api.EchoMapLayer;
import com.knoxhack.echocore.api.EchoMapMarker;
import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echocore.api.WorldHazardSnapshot;
import com.knoxhack.echocore.api.WorldMarker;
import com.knoxhack.echocore.api.WorldMarkerType;
import com.knoxhack.echocore.api.WorldRegionInstance;
import com.knoxhack.echocore.api.WorldRegionType;
import com.knoxhack.echoworldcore.EchoWorldCore;
import com.knoxhack.echoworldcore.service.WorldRegionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum WorldCoreMapDataProvider implements IMapDataProvider {
    INSTANCE;

    private static final Identifier PROVIDER_ID = id("provider/map_data");
    private static final Identifier SOURCE_ID = id("worldcore");

    private static final Identifier CRASH_SITES = holomapLayer("crash_sites");
    private static final Identifier ROUTES = holomapLayer("routes");
    private static final Identifier HAZARDS = holomapLayer("hazards");
    private static final Identifier BASES_OUTPOSTS = holomapLayer("bases_outposts");
    private static final Identifier ORBITAL_SCANS = holomapLayer("orbital_scans");
    private static final Identifier NEXUS_ANOMALY = holomapLayer("nexus_anomaly");
    private static final int MAP_PROVIDER_RADIUS_BLOCKS = 512;

    @Override
    public Identifier providerId() {
        return PROVIDER_ID;
    }

    @Override
    public List<IMapLayer> layers(Player player) {
        return List.of(
                new EchoMapLayer(CRASH_SITES, "Crash Sites", 10, 0xFFFFA05B, true),
                new EchoMapLayer(ROUTES, "Routes", 20, 0xFF92F7A6, true),
                new EchoMapLayer(HAZARDS, "Hazards", 30, 0xFFFF5C7A, true),
                new EchoMapLayer(BASES_OUTPOSTS, "Bases/Outposts", 50, 0xFFFFD166, true),
                new EchoMapLayer(ORBITAL_SCANS, "Orbital Scans", 60, 0xFFA58BFF, true),
                new EchoMapLayer(NEXUS_ANOMALY, "Nexus/Anomaly", 70, 0xFFFF8FEA, true));
    }

    @Override
    public List<IMapMarker> markers(Player player) {
        if (player == null) {
            return List.of();
        }
        List<IMapMarker> markers = new ArrayList<>();
        markers.addAll(worldMarkers(player));
        markers.addAll(regionMarkers(player));
        markers.addAll(hazardMarkers(player));
        return List.copyOf(markers);
    }

    private static List<IMapMarker> worldMarkers(Player player) {
        List<IMapMarker> markers = new ArrayList<>();
        for (WorldMarker marker : WorldRegionService.INSTANCE.nearbyMarkers(
                player.level(), player.blockPosition(), MAP_PROVIDER_RADIUS_BLOCKS)) {
            if (!marker.discovered()) {
                continue;
            }
            Identifier layerId = layerForWorldMarker(marker.type());
            markers.add(new EchoMapMarker(
                    id("map/world_marker/" + marker.id().getNamespace() + "/" + sanitize(marker.id().getPath())),
                    layerId,
                    SOURCE_ID,
                    kindForWorldMarker(marker.type()),
                    marker.discovered() ? IMapMarker.MarkerState.DISCOVERED : IMapMarker.MarkerState.LOCKED,
                    marker.discovered() ? marker.displayName() : lockedTitle(marker.type()),
                    marker.discovered() ? markerSummary(marker) : lockedSummary(marker.type()),
                    marker.dimension(),
                    marker.pos().getX() + 0.5D,
                    marker.pos().getY(),
                    marker.pos().getZ() + 0.5D,
                    marker.radius(),
                    icon("world/" + marker.type().name().toLowerCase(Locale.ROOT)),
                    null,
                    -1,
                    true));
        }
        return markers;
    }

    private static List<IMapMarker> regionMarkers(Player player) {
        List<IMapMarker> markers = new ArrayList<>();
        for (WorldRegionInstance region : WorldRegionService.INSTANCE.nearbyRegions(
                player, MAP_PROVIDER_RADIUS_BLOCKS)) {
            if (!region.discovered()) {
                continue;
            }
            Identifier layerId = layerForRegion(region.type());
            markers.add(new EchoMapMarker(
                    id("map/region/" + region.id().getNamespace() + "/" + sanitize(region.id().getPath())),
                    layerId,
                    SOURCE_ID,
                    kindForRegion(region.type()),
                    region.discovered() ? IMapMarker.MarkerState.DISCOVERED : IMapMarker.MarkerState.LOCKED,
                    region.discovered() ? region.displayName() : "Undiscovered Region",
                    regionSummary(region),
                    region.dimension(),
                    region.center().getX() + 0.5D,
                    region.center().getY(),
                    region.center().getZ() + 0.5D,
                    region.radius(),
                    icon("region/" + region.type().name().toLowerCase(Locale.ROOT)),
                    null,
                    -1,
                    true));
        }
        return markers;
    }

    private static List<IMapMarker> hazardMarkers(Player player) {
        WorldHazardSnapshot hazard = WorldRegionService.INSTANCE.hazardSnapshot(player);
        if (hazard.safeZone()) {
            return List.of();
        }
        BlockPos pos = player.blockPosition();
        return List.of(new EchoMapMarker(
                id("map/hazard/world_snapshot"),
                HAZARDS,
                SOURCE_ID,
                IMapMarker.MarkerKind.HAZARD,
                IMapMarker.MarkerState.DISCOVERED,
                "World Hazard Overlay",
                hazard.summary(),
                player.level().dimension(),
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                Math.max(48.0F, hazard.severity() * 2.0F),
                icon("hazard/world_snapshot"),
                null,
                -1,
                true));
    }

    private static Identifier layerForWorldMarker(WorldMarkerType type) {
        return switch (type == null ? WorldMarkerType.STRUCTURE : type) {
            case CRASH_SITE, STRUCTURE, REGION_CENTER -> CRASH_SITES;
            case ROUTE_START, ROUTE_CHECKPOINT, ROUTE_DESTINATION -> ROUTES;
            case HAZARD -> HAZARDS;
            case ORBITAL_DEBRIS -> ORBITAL_SCANS;
            case OUTPOST -> BASES_OUTPOSTS;
            case ANOMALY -> NEXUS_ANOMALY;
        };
    }

    private static Identifier layerForRegion(WorldRegionType type) {
        return switch (type == null ? WorldRegionType.ANOMALY_ZONE : type) {
            case CONVOY_ROUTE -> ROUTES;
            case SECURE_OUTPOST -> BASES_OUTPOSTS;
            case ORBITAL_DEBRIS_FIELD -> ORBITAL_SCANS;
            case NEXUS_SCAR, ANOMALY_ZONE -> NEXUS_ANOMALY;
            case CRASH_ZONE -> CRASH_SITES;
            default -> HAZARDS;
        };
    }

    private static IMapMarker.MarkerKind kindForWorldMarker(WorldMarkerType type) {
        return switch (type == null ? WorldMarkerType.STRUCTURE : type) {
            case ROUTE_START, ROUTE_CHECKPOINT, ROUTE_DESTINATION -> IMapMarker.MarkerKind.ROUTE;
            case HAZARD -> IMapMarker.MarkerKind.HAZARD;
            case ORBITAL_DEBRIS -> IMapMarker.MarkerKind.ORBITAL_SCAN;
            case OUTPOST -> IMapMarker.MarkerKind.BASE_OUTPOST;
            case ANOMALY -> IMapMarker.MarkerKind.NEXUS_ANOMALY;
            case CRASH_SITE, STRUCTURE, REGION_CENTER -> IMapMarker.MarkerKind.CRASH_SITE;
        };
    }

    private static IMapMarker.MarkerKind kindForRegion(WorldRegionType type) {
        return switch (type == null ? WorldRegionType.ANOMALY_ZONE : type) {
            case CRASH_ZONE -> IMapMarker.MarkerKind.CRASH_SITE;
            case CONVOY_ROUTE -> IMapMarker.MarkerKind.ROUTE;
            case ORBITAL_DEBRIS_FIELD -> IMapMarker.MarkerKind.ORBITAL_SCAN;
            case SECURE_OUTPOST -> IMapMarker.MarkerKind.BASE_OUTPOST;
            case NEXUS_SCAR, ANOMALY_ZONE -> IMapMarker.MarkerKind.NEXUS_ANOMALY;
            case TOXIC_SWAMP, RADIATION_ZONE, CRYOGENIC_RUINS -> IMapMarker.MarkerKind.HAZARD;
            case RUINED_CITY -> IMapMarker.MarkerKind.REGION;
        };
    }

    private static String markerSummary(WorldMarker marker) {
        String summary = marker.summary() == null || marker.summary().isBlank()
                ? "WorldCore marker telemetry."
                : marker.summary();
        if (marker.regionId() != null) {
            return readable(marker.type().name()) + " / " + marker.regionId() + " / " + summary;
        }
        return readable(marker.type().name()) + " / " + summary;
    }

    private static String regionSummary(WorldRegionInstance region) {
        String type = readable(region.type().name());
        if (region.hazardIds().isEmpty()) {
            return type + " overlay / no active hazard references.";
        }
        return type + " overlay / hazards " + region.hazardIds();
    }

    private static String lockedTitle(WorldMarkerType type) {
        return switch (type == null ? WorldMarkerType.STRUCTURE : type) {
            case ROUTE_START, ROUTE_CHECKPOINT, ROUTE_DESTINATION -> "Locked Route Marker";
            case HAZARD -> "Locked Hazard Marker";
            case ORBITAL_DEBRIS -> "Locked Orbital Marker";
            case OUTPOST -> "Locked Outpost Marker";
            case ANOMALY -> "Locked Anomaly Marker";
            case CRASH_SITE -> "Locked Crash Marker";
            case REGION_CENTER, STRUCTURE -> "Unresolved Field Marker";
        };
    }

    private static String lockedSummary(WorldMarkerType type) {
        return switch (type == null ? WorldMarkerType.STRUCTURE : type) {
            case ROUTE_START, ROUTE_CHECKPOINT, ROUTE_DESTINATION ->
                    "Route telemetry is present, but checkpoint details are still locked.";
            case HAZARD -> "Hazard telemetry is present, but exposure details are still locked.";
            case ORBITAL_DEBRIS -> "Orbital telemetry is present, but debris details are still locked.";
            case OUTPOST -> "Outpost telemetry is present, but access details are still locked.";
            case ANOMALY -> "Anomaly telemetry is present, but field details are still locked.";
            case CRASH_SITE -> "Crash-site telemetry is present, but recovery details are still locked.";
            case REGION_CENTER, STRUCTURE ->
                    "Field telemetry has found this marker, but the local record is still locked.";
        };
    }

    private static String readable(String value) {
        String clean = value == null ? "marker" : value.toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String part : clean.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? "Marker" : builder.toString();
    }

    private static Identifier icon(String path) {
        return Identifier.fromNamespaceAndPath("echoholomap", "icon/" + sanitize(path));
    }

    private static Identifier holomapLayer(String path) {
        return Identifier.fromNamespaceAndPath("echoholomap", "layer/" + sanitize(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoWorldCore.MODID, sanitize(path));
    }

    private static String sanitize(String value) {
        String clean = value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
        clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        return clean.isBlank() ? "unknown" : clean;
    }
}
