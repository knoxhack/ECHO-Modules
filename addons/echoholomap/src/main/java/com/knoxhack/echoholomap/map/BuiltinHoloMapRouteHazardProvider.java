package com.knoxhack.echoholomap.map;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoHazardTelemetry;
import com.echoplatform.echocore.api.EchoRouteRecord;
import com.echoplatform.echocore.api.IMapMarker;
import com.echoplatform.echocore.api.WorldHazardSnapshot;
import com.echoplatform.echocore.api.WorldMarker;
import com.echoplatform.echocore.api.WorldMarkerType;
import com.echoplatform.echocore.api.WorldRegionInstance;
import com.echoplatform.echocore.api.WorldRegionType;
import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.HoloMapIds;
import com.knoxhack.echoholomap.api.HoloMapOverlayData;
import com.knoxhack.echoholomap.api.HoloMapOverlayKind;
import com.knoxhack.echoholomap.api.HoloMapPrecision;
import com.knoxhack.echoholomap.api.HoloMapQuery;
import com.knoxhack.echoholomap.api.HoloMapRouteData;
import com.knoxhack.echoholomap.api.HoloMapRoutePoint;
import com.knoxhack.echoholomap.api.HoloMapZoneData;
import com.knoxhack.echoholomap.api.HoloMapZonePattern;
import com.knoxhack.echoholomap.api.HoloMapZonePoint;
import com.knoxhack.echoholomap.api.HoloMapZoneShape;
import com.knoxhack.echoholomap.api.IHoloMapDataProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Emits explicit route and hazard overlay payloads from Core and WorldCore state.
 */
public final class BuiltinHoloMapRouteHazardProvider implements IHoloMapDataProvider {
    public static final BuiltinHoloMapRouteHazardProvider INSTANCE = new BuiltinHoloMapRouteHazardProvider();

    private static final int ROUTE_COLOR = 0xFF92F7A6;
    private static final int ROUTE_FILL = 0x2292F7A6;
    private static final int ROUTE_OUTLINE = 0xAA92F7A6;
    private static final int HAZARD_COLOR = 0x66FF5C7A;

    private BuiltinHoloMapRouteHazardProvider() {
    }

    @Override
    public Identifier providerId() {
        return HoloMapIds.id("core_route_hazard_overlays");
    }

    @Override
    public List<HoloMapRouteData> routes(Player player, HoloMapQuery query) {
        Map<Identifier, HoloMapRouteData> routes = new LinkedHashMap<>();
        for (HoloMapRouteData route : worldCoreRoutes(player)) {
            routes.putIfAbsent(route.id(), route);
        }
        int index = 0;
        for (EchoRouteRecord record : EchoCoreServices.routeRecords(player)) {
            HoloMapRouteData route = routeFromRecord(player, record, index++);
            routes.putIfAbsent(route.id(), route);
        }
        return routes.values().stream()
                .filter(route -> routeIntersects(query, route))
                .toList();
    }

    @Override
    public List<HoloMapOverlayData> overlays(Player player, HoloMapQuery query) {
        List<HoloMapOverlayData> overlays = new ArrayList<>();
        overlays.addAll(worldRegionHazards(player));
        overlays.addAll(worldMarkerHazards(player));
        liveHazardOverlay(player).ifPresent(overlays::add);
        worldHazardSnapshotOverlay(player).ifPresent(overlays::add);
        return overlays.stream()
                .filter(overlay -> overlayIntersects(query, overlay))
                .toList();
    }

    @Override
    public List<HoloMapZoneData> zones(Player player, HoloMapQuery query) {
        return routes(player, query).stream()
                .filter(route -> route.points().size() >= 2)
                .map(BuiltinHoloMapRouteHazardProvider::routeCorridor)
                .toList();
    }

    private static List<HoloMapRouteData> worldCoreRoutes(Player player) {
        if (player == null) {
            return List.of();
        }
        Map<Identifier, List<WorldMarker>> grouped = new LinkedHashMap<>();
        for (WorldMarker marker : nearbyWorldMarkers(player)) {
            if (!marker.discovered() || !isRouteMarker(marker.type()) || marker.regionId() == null) {
                continue;
            }
            grouped.computeIfAbsent(marker.regionId(), ignored -> new ArrayList<>()).add(marker);
        }
        List<HoloMapRouteData> routes = new ArrayList<>();
        for (Map.Entry<Identifier, List<WorldMarker>> entry : grouped.entrySet()) {
            List<WorldMarker> markers = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(BuiltinHoloMapRouteHazardProvider::routeMarkerOrder)
                            .thenComparing(marker -> marker.id().toString()))
                    .toList();
            if (markers.size() < 2) {
                continue;
            }
            List<HoloMapRoutePoint> points = new ArrayList<>();
            for (int index = 0; index < markers.size(); index++) {
                WorldMarker marker = markers.get(index);
                points.add(new HoloMapRoutePoint(
                        marker.dimension(),
                        marker.pos().getX() + 0.5D,
                        marker.pos().getY(),
                        marker.pos().getZ() + 0.5D,
                        index,
                        marker.displayName(),
                        HoloMapPrecision.PRECISE));
            }
            WorldMarker first = markers.getFirst();
            Identifier routeId = entry.getKey();
            routes.add(new HoloMapRouteData(
                    routeId,
                    HoloMapIds.ROUTES,
                    HoloMapIds.WORLD_SOURCE,
                    first.displayName(),
                    "WorldCore route with " + points.size() + " discovered checkpoint(s).",
                    first.dimension(),
                    ROUTE_COLOR,
                    IMapMarker.MarkerState.DISCOVERED,
                    points));
        }
        return routes;
    }

    private static HoloMapRouteData routeFromRecord(Player player, EchoRouteRecord record, int index) {
        ResourceKey<Level> dimension = routeDimension(player, record);
        double centerX = virtualCoordinate(record.id(), 103 + index);
        double centerZ = virtualCoordinate(record.id(), 197 + index);
        double spreadX = 40.0D + Math.floorMod(Objects.hash(record.id(), "x"), 48);
        double spreadZ = 32.0D + Math.floorMod(Objects.hash(record.id(), "z"), 48);
        List<HoloMapRoutePoint> points = List.of(
                new HoloMapRoutePoint(dimension, centerX - spreadX, 64.0D, centerZ - spreadZ, 0,
                        "Route start", HoloMapPrecision.ESTIMATED),
                new HoloMapRoutePoint(dimension, centerX, 64.0D, centerZ, 1,
                        record.title(), HoloMapPrecision.ESTIMATED),
                new HoloMapRoutePoint(dimension, centerX + spreadZ, 64.0D, centerZ + spreadX, 2,
                        "Route destination", HoloMapPrecision.ESTIMATED));
        return new HoloMapRouteData(
                record.id(),
                HoloMapIds.ROUTES,
                HoloMapIds.ROUTE_SOURCE,
                record.title(),
                record.status() + " / " + record.summary(),
                dimension,
                record.complete() ? 0xFF7CF7D4 : ROUTE_COLOR,
                routeState(record),
                points);
    }

    private static List<HoloMapOverlayData> worldRegionHazards(Player player) {
        if (player == null) {
            return List.of();
        }
        List<HoloMapOverlayData> overlays = new ArrayList<>();
        for (WorldRegionInstance region : EchoCoreServices.worldRegions().activeRegions(player)) {
            if (!region.discovered() || !isHazardRegion(region)) {
                continue;
            }
            overlays.add(new HoloMapOverlayData(
                    HoloMapIds.id("overlay/world_region/" + region.id()),
                    HoloMapIds.HAZARDS,
                    HoloMapIds.WORLD_SOURCE,
                    overlayKind(region.type()),
                    IMapMarker.MarkerState.DISCOVERED,
                    region.displayName(),
                    regionSummary(region),
                    region.dimension(),
                    region.center().getX() + 0.5D,
                    region.center().getY(),
                    region.center().getZ() + 0.5D,
                    region.radius(),
                    HAZARD_COLOR,
                    HoloMapPrecision.ESTIMATED));
        }
        return overlays;
    }

    private static List<HoloMapOverlayData> worldMarkerHazards(Player player) {
        if (player == null) {
            return List.of();
        }
        List<HoloMapOverlayData> overlays = new ArrayList<>();
        for (WorldMarker marker : nearbyWorldMarkers(player)) {
            if (!marker.discovered() || marker.type() != WorldMarkerType.HAZARD) {
                continue;
            }
            overlays.add(new HoloMapOverlayData(
                    HoloMapIds.id("overlay/world_marker/" + marker.id()),
                    HoloMapIds.HAZARDS,
                    HoloMapIds.WORLD_SOURCE,
                    HoloMapOverlayKind.HAZARD,
                    IMapMarker.MarkerState.DISCOVERED,
                    marker.displayName(),
                    marker.summary(),
                    marker.dimension(),
                    marker.pos().getX() + 0.5D,
                    marker.pos().getY(),
                    marker.pos().getZ() + 0.5D,
                    marker.radius(),
                    HAZARD_COLOR,
                    HoloMapPrecision.PRECISE));
        }
        return overlays;
    }

    private static java.util.Optional<HoloMapOverlayData> liveHazardOverlay(Player player) {
        if (player == null) {
            return java.util.Optional.empty();
        }
        EchoHazardTelemetry telemetry = EchoCoreServices.hazardTelemetry(player);
        if (!telemetry.warning()) {
            return java.util.Optional.empty();
        }
        int severity = List.of(
                100 - telemetry.hydration(),
                telemetry.radiation(),
                telemetry.toxicAir(),
                100 - telemetry.oxygen(),
                100 - telemetry.pressure(),
                telemetry.cold(),
                telemetry.heat(),
                telemetry.exposure()).stream().mapToInt(Integer::intValue).max().orElse(0);
        return java.util.Optional.of(new HoloMapOverlayData(
                overlayIdForMarker(HoloMapIds.id("hazard/live_vitals")),
                HoloMapIds.HAZARDS,
                HoloMapIds.HAZARD_SOURCE,
                HoloMapOverlayKind.HAZARD,
                IMapMarker.MarkerState.DISCOVERED,
                "Live Hazard Telemetry",
                telemetry.statusLine(),
                player.level().dimension(),
                player.getX(),
                player.getY(),
                player.getZ(),
                Math.max(32.0F, severity * 1.5F),
                HAZARD_COLOR,
                HoloMapPrecision.PRECISE));
    }

    private static java.util.Optional<HoloMapOverlayData> worldHazardSnapshotOverlay(Player player) {
        if (player == null) {
            return java.util.Optional.empty();
        }
        WorldHazardSnapshot snapshot = EchoCoreServices.hazardService().hazardSnapshot(player);
        if (snapshot.safeZone()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new HoloMapOverlayData(
                overlayIdForMarker(HoloMapIds.id("hazard/world_snapshot")),
                HoloMapIds.HAZARDS,
                HoloMapIds.HAZARD_SOURCE,
                HoloMapOverlayKind.HAZARD,
                IMapMarker.MarkerState.DISCOVERED,
                "World Hazard Overlay",
                snapshot.summary(),
                player.level().dimension(),
                player.getX(),
                player.getY(),
                player.getZ(),
                Math.max(48.0F, snapshot.severity() * 2.0F),
                HAZARD_COLOR,
                HoloMapPrecision.ESTIMATED));
    }

    private static HoloMapZoneData routeCorridor(HoloMapRouteData route) {
        List<HoloMapZonePoint> points = route.points().stream()
                .map(point -> new HoloMapZonePoint(point.dimension(), point.x(), point.y(), point.z(), point.order()))
                .toList();
        double x = route.points().stream().mapToDouble(HoloMapRoutePoint::x).average().orElse(0.0D);
        double y = route.points().stream().mapToDouble(HoloMapRoutePoint::y).average().orElse(64.0D);
        double z = route.points().stream().mapToDouble(HoloMapRoutePoint::z).average().orElse(0.0D);
        return new HoloMapZoneData(
                HoloMapIds.id("zone/route_corridor/" + route.id()),
                route.layerId(),
                route.sourceId(),
                HoloMapZoneShape.CORRIDOR,
                HoloMapZonePattern.ROUTE_BANDS,
                route.state(),
                route.title(),
                route.summary(),
                route.dimension(),
                x,
                y,
                z,
                24.0F,
                48.0F,
                48.0F,
                ROUTE_FILL,
                ROUTE_OUTLINE,
                HoloMapPrecision.ESTIMATED,
                55,
                points);
    }

    private static List<WorldMarker> nearbyWorldMarkers(Player player) {
        if (player == null) {
            return List.of();
        }
        return EchoCoreServices.worldMarkerService().nearbyMarkers(
                player.level(), player.blockPosition(), Config.mapInterestRadiusBlocks());
    }

    private static boolean routeIntersects(HoloMapQuery query, HoloMapRouteData route) {
        return true;
    }

    private static boolean overlayIntersects(HoloMapQuery query, HoloMapOverlayData overlay) {
        return query == null || query.intersectsCircle(overlay.dimension(), overlay.x(), overlay.z(), overlay.radius());
    }

    private static boolean isRouteMarker(WorldMarkerType type) {
        return type == WorldMarkerType.ROUTE_START
                || type == WorldMarkerType.ROUTE_CHECKPOINT
                || type == WorldMarkerType.ROUTE_DESTINATION;
    }

    private static int routeMarkerOrder(WorldMarker marker) {
        return switch (marker.type()) {
            case ROUTE_START -> 0;
            case ROUTE_CHECKPOINT -> 100;
            case ROUTE_DESTINATION -> 1000;
            default -> 500;
        };
    }

    private static boolean isHazardRegion(WorldRegionInstance region) {
        return switch (region.type()) {
            case TOXIC_SWAMP, RADIATION_ZONE, CRYOGENIC_RUINS, NEXUS_SCAR, ANOMALY_ZONE -> true;
            default -> !region.hazardIds().isEmpty();
        };
    }

    private static HoloMapOverlayKind overlayKind(WorldRegionType type) {
        return switch (type) {
            case NEXUS_SCAR, ANOMALY_ZONE -> HoloMapOverlayKind.REGION;
            default -> HoloMapOverlayKind.HAZARD;
        };
    }

    private static String regionSummary(WorldRegionInstance region) {
        String type = region.type().displayName();
        if (region.hazardIds().isEmpty()) {
            return type + " overlay from WorldCore region state.";
        }
        return type + " overlay from WorldCore region state / hazards " + region.hazardIds();
    }

    private static ResourceKey<Level> routeDimension(Player player, EchoRouteRecord record) {
        Identifier parsed = Identifier.tryParse(record.dimensionHint());
        if (parsed != null) {
            return ResourceKey.create(Registries.DIMENSION, parsed);
        }
        return player == null ? Level.OVERWORLD : player.level().dimension();
    }

    private static IMapMarker.MarkerState routeState(EchoRouteRecord record) {
        if (record.complete()) {
            return IMapMarker.MarkerState.CHECKED;
        }
        String status = record.status().toLowerCase(java.util.Locale.ROOT);
        if (status.contains("locked") || status.contains("sealed") || status.contains("pending")
                || status.contains("waiting") || status.contains("blocked")) {
            return IMapMarker.MarkerState.LOCKED;
        }
        return IMapMarker.MarkerState.DISCOVERED;
    }

    private static Identifier overlayIdForMarker(Identifier markerId) {
        return HoloMapIds.id("overlay/" + markerId);
    }

    private static double virtualCoordinate(Identifier id, int salt) {
        int hash = Objects.hash(id == null ? "unknown" : id.toString(), salt);
        double base = Math.floorMod(hash, 2200) - 1100;
        return base * virtualScale();
    }

    private static double virtualScale() {
        try {
            return Config.VIRTUAL_MAP_SCALE.get();
        } catch (RuntimeException exception) {
            return 1.0D;
        }
    }
}
