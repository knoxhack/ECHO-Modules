package com.knoxhack.echoholomap.map;

import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.HoloMapIds;
import com.knoxhack.echoholomap.api.HoloMapOverlayKind;
import com.knoxhack.echoholomap.api.HoloMapZoneData;
import com.knoxhack.echoholomap.api.HoloMapZonePattern;
import com.knoxhack.echoholomap.api.HoloMapZoneShape;
import com.knoxhack.echoholomap.network.HoloMapSnapshotPacket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;

public final class HoloMapSnapshotBuilder {
    private HoloMapSnapshotBuilder() {
    }

    public static HoloMapSnapshotPacket from(ServerPlayer player) {
        HoloMapService service = HoloMapService.INSTANCE;
        HoloMapInterestWindow window = HoloMapInterestWindow.from(player);
        var markers = service.richMarkers(player, window.query()).stream()
                .filter(window::eligible)
                .limit(HoloMapSnapshotPacket.maxMarkers())
                .map(HoloMapSnapshotPacket.MarkerData::from)
                .toList();
        var routes = mergeRoutes(service.richRoutes(player, window.query()).stream()
                .map(window::trimRoute)
                .filter(Objects::nonNull)
                .map(HoloMapSnapshotPacket.RouteData::from)
                .toList(), markers);
        var overlays = mergeOverlays(service.richOverlays(player, window.query()).stream()
                .filter(window::eligible)
                .map(HoloMapSnapshotPacket.OverlayData::from)
                .toList(), markers);
        var zones = mergeZones(service.richZones(player, window.query()).stream()
                .filter(window::eligible)
                .map(HoloMapSnapshotPacket.ZoneData::from)
                .toList(), overlays);
        Set<Identifier> activeLayers = activeLayerIds(markers, routes, overlays, zones);
        var layers = service.richLayers(player).stream()
                .filter(layer -> activeLayers.contains(layer.id()))
                .map(HoloMapSnapshotPacket.LayerData::from)
                .toList();
        var diagnostics = service.diagnostics(player).stream()
                .limit(HoloMapSnapshotPacket.maxDiagnostics())
                .map(HoloMapSnapshotPacket.ProviderDiagnosticData::from)
                .toList();
        long gameTime = player == null ? 0L : player.level().getGameTime();
        String status = "HoloMap synced nearby discovered: " + markers.size() + " marker(s), "
                + routes.size() + " route(s), " + overlays.size() + " overlay(s), "
                + zones.size() + " zone(s).";
        return new HoloMapSnapshotPacket(layers, markers, routes, overlays, zones, diagnostics, status, gameTime);
    }

    private static Set<Identifier> activeLayerIds(
            List<HoloMapSnapshotPacket.MarkerData> markers,
            List<HoloMapSnapshotPacket.RouteData> routes,
            List<HoloMapSnapshotPacket.OverlayData> overlays,
            List<HoloMapSnapshotPacket.ZoneData> zones) {
        LinkedHashSet<Identifier> ids = new LinkedHashSet<>();
        for (HoloMapSnapshotPacket.MarkerData marker : markers) {
            ids.add(marker.layerId());
        }
        for (HoloMapSnapshotPacket.RouteData route : routes) {
            ids.add(route.layerId());
        }
        for (HoloMapSnapshotPacket.OverlayData overlay : overlays) {
            ids.add(overlay.layerId());
        }
        for (HoloMapSnapshotPacket.ZoneData zone : zones) {
            ids.add(zone.layerId());
        }
        return Set.copyOf(ids);
    }

    private static List<HoloMapSnapshotPacket.RouteData> mergeRoutes(
            List<HoloMapSnapshotPacket.RouteData> richRoutes,
            List<HoloMapSnapshotPacket.MarkerData> markers) {
        Map<Identifier, HoloMapSnapshotPacket.RouteData> routes = new LinkedHashMap<>();
        if (richRoutes != null) {
            for (HoloMapSnapshotPacket.RouteData route : richRoutes) {
                if (route != null && route.id() != null) {
                    routes.putIfAbsent(route.id(), route);
                }
            }
        }

        Map<String, List<HoloMapSnapshotPacket.MarkerData>> routeMarkers = new LinkedHashMap<>();
        for (HoloMapSnapshotPacket.MarkerData marker : markers) {
            if (marker == null || marker.routeId().isBlank()) {
                continue;
            }
            routeMarkers.computeIfAbsent(marker.routeId(), ignored -> new ArrayList<>()).add(marker);
        }
        for (Map.Entry<String, List<HoloMapSnapshotPacket.MarkerData>> entry : routeMarkers.entrySet()) {
            Identifier routeId = routeId(entry.getKey());
            if (routes.containsKey(routeId)) {
                continue;
            }
            List<HoloMapSnapshotPacket.MarkerData> points = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(HoloMapSnapshotPacket.MarkerData::routeOrder)
                            .thenComparing(HoloMapSnapshotPacket.MarkerData::title)
                            .thenComparing(marker -> marker.id().toString()))
                    .toList();
            HoloMapSnapshotPacket.MarkerData first = points.getFirst();
            List<HoloMapSnapshotPacket.RoutePointData> routePoints = new ArrayList<>();
            for (int i = 0; i < points.size(); i++) {
                HoloMapSnapshotPacket.MarkerData point = points.get(i);
                int order = point.routeOrder() >= 0 ? point.routeOrder() : i;
                routePoints.add(new HoloMapSnapshotPacket.RoutePointData(point.dimension(),
                        point.x(), point.y(), point.z(), order, point.title(), point.precision()));
            }
            routes.put(routeId, new HoloMapSnapshotPacket.RouteData(routeId, first.layerId(), first.sourceId(),
                    first.title(), first.summary(), first.dimension(), 0xFF92F7A6, first.state(), routePoints));
        }
        return routes.values().stream()
                .limit(HoloMapSnapshotPacket.maxRoutes())
                .toList();
    }

    private static List<HoloMapSnapshotPacket.OverlayData> mergeOverlays(
            List<HoloMapSnapshotPacket.OverlayData> richOverlays,
            List<HoloMapSnapshotPacket.MarkerData> markers) {
        Map<Identifier, HoloMapSnapshotPacket.OverlayData> overlays = new LinkedHashMap<>();
        if (richOverlays != null) {
            for (HoloMapSnapshotPacket.OverlayData overlay : richOverlays) {
                if (overlay != null && overlay.id() != null) {
                    overlays.putIfAbsent(overlay.id(), overlay);
                }
            }
        }
        for (HoloMapSnapshotPacket.MarkerData marker : markers) {
            if (marker == null || marker.radius() <= 0.0F
                    || !HoloMapVisibility.visibleInNormalView(marker.state())
                    || !HoloMapVisibility.markerCanGenerateField(marker.kind())) {
                continue;
            }
            Identifier overlayId = HoloMapIds.id("overlay/" + marker.id());
            overlays.putIfAbsent(overlayId, new HoloMapSnapshotPacket.OverlayData(overlayId,
                    marker.layerId(), marker.sourceId(), overlayKind(marker), marker.state(), marker.title(),
                    marker.summary(), marker.dimension(), marker.x(), marker.y(), marker.z(), marker.radius(),
                    overlayColor(marker), marker.precision()));
        }
        return overlays.values().stream()
                .limit(HoloMapSnapshotPacket.maxOverlays())
                .toList();
    }

    private static List<HoloMapSnapshotPacket.ZoneData> mergeZones(
            List<HoloMapSnapshotPacket.ZoneData> richZones,
            List<HoloMapSnapshotPacket.OverlayData> overlays) {
        Map<Identifier, HoloMapSnapshotPacket.ZoneData> zones = new LinkedHashMap<>();
        if (richZones != null) {
            for (HoloMapSnapshotPacket.ZoneData zone : richZones) {
                if (zone != null && zone.id() != null) {
                    zones.putIfAbsent(zone.id(), zone);
                }
            }
        }
        for (HoloMapSnapshotPacket.OverlayData overlay : overlays) {
            if (overlay == null || overlay.radius() <= 0.0F) {
                continue;
            }
            Identifier zoneId = HoloMapIds.id("zone/" + overlay.id());
            zones.putIfAbsent(zoneId, HoloMapSnapshotPacket.ZoneData.from(new HoloMapZoneData(
                    zoneId,
                    overlay.layerId(),
                    overlay.sourceId(),
                    HoloMapZoneShape.CIRCLE,
                    zonePattern(overlay),
                    overlay.state(),
                    overlay.title(),
                    overlay.summary(),
                    dimensionKey(overlay.dimension()),
                    overlay.x(),
                    overlay.y(),
                    overlay.z(),
                    overlay.radius(),
                    overlay.radius() * 2.0F,
                    overlay.radius() * 2.0F,
                    zoneFillColor(overlay),
                    zoneOutlineColor(overlay),
                    overlay.precision(),
                    zonePriority(overlay),
                    List.of())));
        }
        return zones.values().stream()
                .limit(HoloMapSnapshotPacket.maxZones())
                .toList();
    }

    private static Identifier routeId(String value) {
        Identifier parsed = Identifier.tryParse(value);
        return parsed == null ? HoloMapIds.id("route/" + value) : parsed;
    }

    private static HoloMapOverlayKind overlayKind(HoloMapSnapshotPacket.MarkerData marker) {
        if (marker.kind() == IMapMarker.MarkerKind.HAZARD
                || HoloMapIds.HAZARDS.equals(marker.layerId())
                || HoloMapIds.HAZARD_SOURCE.equals(marker.sourceId())) {
            return HoloMapOverlayKind.HAZARD;
        }
        if (marker.kind() == IMapMarker.MarkerKind.DRONE_SCAN
                || HoloMapIds.DRONES_SCANS.equals(marker.layerId())) {
            return HoloMapOverlayKind.SCAN;
        }
        return HoloMapOverlayKind.CIRCLE;
    }

    private static int overlayColor(HoloMapSnapshotPacket.MarkerData marker) {
        return switch (overlayKind(marker)) {
            case HAZARD -> 0x66FF5C7A;
            case SCAN -> 0x663EE7FF;
            case ROUTE_CORRIDOR -> 0x6692F7A6;
            case REGION -> 0x6680F0A0;
            case CIRCLE -> 0x66FFFFFF;
        };
    }

    private static HoloMapZonePattern zonePattern(HoloMapSnapshotPacket.OverlayData overlay) {
        return switch (overlay.kind()) {
            case HAZARD -> HoloMapZonePattern.HAZARD_STRIPES;
            case SCAN -> HoloMapZonePattern.SCAN_GRID;
            case ROUTE_CORRIDOR -> HoloMapZonePattern.ROUTE_BANDS;
            case REGION -> HoloMapZonePattern.SOLID;
            case CIRCLE -> HoloMapZonePattern.SOLID;
        };
    }

    private static int zoneFillColor(HoloMapSnapshotPacket.OverlayData overlay) {
        int alpha = overlay.precision() == com.knoxhack.echoholomap.api.HoloMapPrecision.PRECISE ? 0x38 : 0x28;
        return withAlpha(overlay.color(), alpha);
    }

    private static int zoneOutlineColor(HoloMapSnapshotPacket.OverlayData overlay) {
        int alpha = overlay.state() == IMapMarker.MarkerState.LOCKED ? 0x66 : 0xAA;
        return withAlpha(overlay.color(), alpha);
    }

    private static int zonePriority(HoloMapSnapshotPacket.OverlayData overlay) {
        return switch (overlay.kind()) {
            case HAZARD -> 90;
            case REGION -> 70;
            case SCAN -> 60;
            case ROUTE_CORRIDOR -> 55;
            case CIRCLE -> 25;
        };
    }

    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimensionKey(String value) {
        Identifier id = Identifier.tryParse(value);
        if (id == null) {
            return net.minecraft.world.level.Level.OVERWORLD;
        }
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
    }

    private static int withAlpha(int color, int alpha) {
        return ((Math.max(0, Math.min(255, alpha)) & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
}
