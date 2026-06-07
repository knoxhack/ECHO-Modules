package com.knoxhack.echoholomap.map;

import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.api.HoloMapMarkerData;
import com.knoxhack.echoholomap.api.HoloMapOverlayData;
import com.knoxhack.echoholomap.api.HoloMapQuery;
import com.knoxhack.echoholomap.api.HoloMapRouteData;
import com.knoxhack.echoholomap.api.HoloMapRoutePoint;
import com.knoxhack.echoholomap.api.HoloMapZoneData;
import com.knoxhack.echoholomap.api.HoloMapZonePoint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class HoloMapInterestWindow {
    private final HoloMapQuery query;

    private HoloMapInterestWindow(HoloMapQuery query) {
        this.query = query;
    }

    public static HoloMapInterestWindow from(Player player) {
        return new HoloMapInterestWindow(HoloMapQuery.from(player, Config.mapInterestRadiusBlocks()));
    }

    public static HoloMapInterestWindow from(ServerPlayer player) {
        return from((Player) player);
    }

    public HoloMapQuery query() {
        return query;
    }

    public boolean visibleState(IMapMarker.MarkerState state) {
        return HoloMapVisibility.visibleInNormalView(state);
    }

    public boolean eligible(HoloMapMarkerData marker) {
        return marker != null
                && visibleState(marker.state())
                && query.intersectsCircle(marker.dimension(), marker.x(), marker.z(), marker.radius());
    }

    public boolean eligible(HoloMapOverlayData overlay) {
        return overlay != null
                && visibleState(overlay.state())
                && query.intersectsCircle(overlay.dimension(), overlay.x(), overlay.z(), overlay.radius());
    }

    public boolean eligible(HoloMapZoneData zone) {
        if (zone == null || !visibleState(zone.state())) {
            return false;
        }
        return switch (zone.shape()) {
            case CIRCLE -> query.intersectsCircle(zone.dimension(), zone.x(), zone.z(), zone.radius());
            case RECT -> query.intersectsRect(zone.dimension(), zone.x(), zone.z(), zone.width(), zone.depth());
            case POLYGON -> polygonIntersects(zone);
            case CORRIDOR -> corridorIntersects(zone);
        };
    }

    public HoloMapRouteData trimRoute(HoloMapRouteData route) {
        if (route == null || !visibleState(route.state()) || route.points().size() < 2) {
            return null;
        }
        boolean[] keep = new boolean[route.points().size()];
        boolean[] inside = new boolean[route.points().size()];
        boolean intersects = false;
        for (int i = 0; i < route.points().size(); i++) {
            HoloMapRoutePoint point = route.points().get(i);
            if (query.intersectsPoint(point.dimension(), point.x(), point.z())) {
                intersects = true;
                inside[i] = true;
            }
        }
        for (int i = 0; i < inside.length; i++) {
            if (inside[i]) {
                keepWithNeighbors(keep, i);
            }
        }
        for (int i = 1; i < route.points().size(); i++) {
            if (segmentIntersects(route.points().get(i - 1), route.points().get(i), 0.0D)) {
                intersects = true;
                if (!inside[i - 1] && !inside[i]) {
                    keep[i - 1] = true;
                    keep[i] = true;
                }
            }
        }
        if (!intersects) {
            return null;
        }
        List<HoloMapRoutePoint> points = new ArrayList<>();
        for (int i = 0; i < route.points().size(); i++) {
            if (keep[i]) {
                points.add(route.points().get(i));
            }
        }
        if (points.size() < 2) {
            return null;
        }
        return new HoloMapRouteData(route.id(), route.layerId(), route.sourceId(), route.title(), route.summary(),
                route.dimension(), route.color(), route.state(), points);
    }

    private boolean polygonIntersects(HoloMapZoneData zone) {
        if (zone.points().isEmpty()) {
            return query.intersectsRect(zone.dimension(), zone.x(), zone.z(), zone.width(), zone.depth());
        }
        Bounds bounds = bounds(zone.points(), 0.0D);
        return query.intersectsBounds(zone.dimension(), bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ());
    }

    private boolean corridorIntersects(HoloMapZoneData zone) {
        if (zone.points().isEmpty()) {
            return query.intersectsCircle(zone.dimension(), zone.x(), zone.z(), zone.radius());
        }
        double pad = Math.max(zone.radius(), Math.max(8.0D, zone.width() / 2.0D));
        Bounds bounds = bounds(zone.points(), pad);
        if (!query.intersectsBounds(zone.dimension(), bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ())) {
            return false;
        }
        for (int i = 1; i < zone.points().size(); i++) {
            if (segmentIntersects(zone.points().get(i - 1), zone.points().get(i), pad)) {
                return true;
            }
        }
        return zone.points().stream().anyMatch(point -> query.intersectsCircle(point.dimension(), point.x(), point.z(), pad));
    }

    private boolean segmentIntersects(HoloMapRoutePoint previous, HoloMapRoutePoint current, double pad) {
        if (!query.matchesDimension(previous.dimension()) || !query.matchesDimension(current.dimension())) {
            return false;
        }
        return distanceToSegmentSquared(previous.x(), previous.z(), current.x(), current.z(),
                query.centerX(), query.centerZ()) <= square(query.radius() + pad);
    }

    private boolean segmentIntersects(HoloMapZonePoint previous, HoloMapZonePoint current, double pad) {
        if (!query.matchesDimension(previous.dimension()) || !query.matchesDimension(current.dimension())) {
            return false;
        }
        return distanceToSegmentSquared(previous.x(), previous.z(), current.x(), current.z(),
                query.centerX(), query.centerZ()) <= square(query.radius() + pad);
    }

    private static void keepWithNeighbors(boolean[] keep, int index) {
        if (index > 0) {
            keep[index - 1] = true;
        }
        keep[index] = true;
        if (index + 1 < keep.length) {
            keep[index + 1] = true;
        }
    }

    private static Bounds bounds(List<HoloMapZonePoint> points, double pad) {
        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (HoloMapZonePoint point : points) {
            minX = Math.min(minX, point.x());
            minZ = Math.min(minZ, point.z());
            maxX = Math.max(maxX, point.x());
            maxZ = Math.max(maxZ, point.z());
        }
        return new Bounds(minX - pad, minZ - pad, maxX + pad, maxZ + pad);
    }

    private static double distanceToSegmentSquared(double x0, double z0, double x1, double z1,
            double x, double z) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared <= 0.000001D) {
            return square(x - x0) + square(z - z0);
        }
        double t = ((x - x0) * dx + (z - z0) * dz) / lengthSquared;
        t = Math.max(0.0D, Math.min(1.0D, t));
        double closestX = x0 + t * dx;
        double closestZ = z0 + t * dz;
        return square(x - closestX) + square(z - closestZ);
    }

    private static double square(double value) {
        return value * value;
    }

    private record Bounds(double minX, double minZ, double maxX, double maxZ) {
    }
}
