package com.knoxhack.echoholomap.client;

import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.api.HoloMapPrecision;
import com.knoxhack.echoholomap.api.HoloMapZonePattern;
import com.knoxhack.echoholomap.api.HoloMapZoneShape;
import com.knoxhack.echoholomap.map.HoloMapTerrainTile;
import com.knoxhack.echoholomap.map.HoloMapVisibility;
import com.knoxhack.echoholomap.network.HoloMapSnapshotPacket;
import com.knoxhack.echoholomap.network.HoloMapTerrainClientState;
import com.knoxhack.echoholomap.network.HoloMapWaypointClientState;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class HoloMapRenderer {
    private static final int ZONE_DRAW_PLAN_CACHE_MAX_ENTRIES = 512;
    private static final Map<ZoneDrawPlanKey, ZoneDrawPlan> ZONE_DRAW_PLAN_CACHE =
            new LinkedHashMap<>(128, 0.75F, true);
    private static long zoneDrawPlanBuilds;

    public static final RenderBudget FULLSCREEN_BUDGET =
            new RenderBudget(384, 256, 128, 512, 48, 18, true, true);
    public static final RenderBudget FULLSCREEN_INTERACTIVE_BUDGET =
            new RenderBudget(384, 224, 96, 224, 18, 10, 6, true, true);
    public static final RenderBudget TERMINAL_BUDGET =
            new RenderBudget(384, 256, 128, 512, 48, 14, true, true);
    public static final RenderBudget MINIMAP_BUDGET =
            new RenderBudget(72, 96, 96, 192, 24, 0, false, false);

    private RenderCacheKey cacheKey;
    private TerrainCacheKey terrainCacheKey;
    private TerrainBuild terrainBuild = TerrainBuild.EMPTY;
    private HoloMapRenderModel model = HoloMapRenderModel.EMPTY;
    private int modelBuilds;
    private int terrainModelBuilds;

    public RenderResult render(GuiGraphicsExtractor graphics, Font font, HoloMapViewState state,
            HoloMapSnapshotPacket snapshot, List<HoloMapSnapshotPacket.MarkerData> markers,
            List<HoloMapWaypoint> waypoints, RenderBudget budget) {
        RenderBudget safeBudget = (budget == null ? FULLSCREEN_BUDGET : budget).forDetail(renderDetail());
        HoloMapSnapshotPacket safeSnapshot = snapshot == null ? HoloMapSnapshotPacket.empty() : snapshot;
        List<HoloMapSnapshotPacket.MarkerData> safeMarkers = markers == null ? List.of() : markers;
        List<HoloMapWaypoint> safeWaypoints = waypoints == null ? List.of() : waypoints;
        RenderCacheKey nextKey = RenderCacheKey.from(state, safeSnapshot, safeMarkers, safeWaypoints, safeBudget);
        if (!nextKey.equals(cacheKey)) {
            cacheKey = nextKey;
            model = buildModel(state, safeSnapshot, safeMarkers, safeWaypoints, safeBudget);
            modelBuilds++;
        }
        draw(graphics, font, state, model, safeBudget);
        return model.result();
    }

    public RenderResult prepareModelForTests(HoloMapViewState state, HoloMapSnapshotPacket snapshot,
            List<HoloMapSnapshotPacket.MarkerData> markers, List<HoloMapWaypoint> waypoints, RenderBudget budget) {
        RenderBudget safeBudget = (budget == null ? FULLSCREEN_BUDGET : budget).forDetail(renderDetail());
        HoloMapSnapshotPacket safeSnapshot = snapshot == null ? HoloMapSnapshotPacket.empty() : snapshot;
        List<HoloMapSnapshotPacket.MarkerData> safeMarkers = markers == null ? List.of() : markers;
        List<HoloMapWaypoint> safeWaypoints = waypoints == null ? List.of() : waypoints;
        RenderCacheKey nextKey = RenderCacheKey.from(state, safeSnapshot, safeMarkers, safeWaypoints, safeBudget);
        if (!nextKey.equals(cacheKey)) {
            cacheKey = nextKey;
            model = buildModel(state, safeSnapshot, safeMarkers, safeWaypoints, safeBudget);
            modelBuilds++;
        }
        return model.result();
    }

    public static boolean highDetailTerrainAllowed(double zoom, int visibleTileCount, boolean configEnabled) {
        return configEnabled && zoom >= 2.5D && visibleTileCount <= 64;
    }

    public static boolean highDetailTerrainEnabled() {
        try {
            return Config.MAP_HIGH_DETAIL_TERRAIN.get();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static Config.MapRenderDetail renderDetail() {
        try {
            return Config.MAP_RENDER_DETAIL.get();
        } catch (RuntimeException exception) {
            return Config.MapRenderDetail.BALANCED;
        }
    }

    public static String terrainStatusLabel(RenderResult result, int realChunks) {
        if (realChunks <= 0) {
            return "pending real chunks";
        }
        RenderResult safeResult = result == null
                ? new RenderResult(0, 0, 0, 0, 0, 0, List.of(), List.of(), false)
                : result;
        if (safeResult.syncedTerrainTiles() <= 0) {
            return "visible scan pending / " + realChunks + " remembered real chunks";
        }
        if (safeResult.culledTerrainTiles() <= 0) {
            return safeResult.terrainTiles() + " visible synced / " + realChunks + " remembered real chunks";
        }
        return safeResult.terrainTiles() + " rendered / "
                + safeResult.syncedTerrainTiles() + " visible synced / "
                + realChunks + " remembered real chunks";
    }

    public static int viewportBucketForTests(double centerX, double centerZ, int x, int y, int width, int height) {
        return new HoloMapViewState("minecraft:overworld", x, y, width, height, centerX, centerZ, 1.0D,
                true, true, "", "", 0, 0, centerX, centerZ, 0.0F).viewportBucket();
    }

    public static int cacheFingerprintForTests(HoloMapViewState state, HoloMapSnapshotPacket snapshot,
            List<HoloMapSnapshotPacket.MarkerData> markers, List<HoloMapWaypoint> waypoints, RenderBudget budget) {
        return RenderCacheKey.from(state, snapshot == null ? HoloMapSnapshotPacket.empty() : snapshot,
                markers == null ? List.of() : markers, waypoints == null ? List.of() : waypoints,
                budget == null ? FULLSCREEN_BUDGET : budget).hashCode();
    }

    public static List<Identifier> selectedMarkerIdsForTests(HoloMapViewState state,
            List<HoloMapSnapshotPacket.MarkerData> markers, int maxMarkers) {
        RenderBudget budget = new RenderBudget(0, Math.max(0, maxMarkers), 0, 0, 0, 0, false, true);
        return buildMarkers(state, markers == null ? List.of() : markers, budget).stream()
                .map(marker -> marker.marker().id())
                .toList();
    }

    public static List<Identifier> selectedZoneIdsForTests(HoloMapViewState state,
            List<HoloMapSnapshotPacket.ZoneData> zones, int maxZones) {
        RenderBudget budget = new RenderBudget(0, 0, 0, 0, 0, Math.max(0, maxZones), 0, false, true);
        return buildZones(state, zones == null ? List.of() : zones, budget).stream()
                .map(zone -> zone.zone().id())
                .toList();
    }

    public static List<Identifier> selectedZoneIdsForTests(HoloMapViewState state,
            List<HoloMapSnapshotPacket.ZoneData> zones, int maxZones, HoloMapVisibility.FieldMode fieldMode) {
        HoloMapViewState modeState = new HoloMapViewState(state.dimension(), state.x(), state.y(), state.width(),
                state.height(), state.centerX(), state.centerZ(), state.zoom(), state.showMarkers(), fieldMode,
                state.showWaypoints(), state.selectedMarkerId(), state.selectedWaypointId(),
                state.mouseX(), state.mouseY(), state.playerX(), state.playerZ(), state.playerYaw());
        return selectedZoneIdsForTests(modeState, zones, maxZones);
    }

    public static int edgeIndicatorSectorLimitForTests(int maxMarkers) {
        return edgeIndicatorLimit(new RenderBudget(0, Math.max(0, maxMarkers), 0, 0, 0, 0, false, true));
    }

    public static int fallbackGridLineCountForTests(HoloMapViewState state) {
        return buildFallbackGrid(state).size();
    }

    public static boolean zoneHitForTests(HoloMapViewState state, HoloMapSnapshotPacket.ZoneData zone,
            int mouseX, int mouseY) {
        ZonePrimitive primitive = zonePrimitive(state, zone);
        return primitive != null && zoneContains(primitive, mouseX, mouseY);
    }

    public static int tacticalZoneFillAlphaForTests(int baseColor, int screenSpan, boolean hovered) {
        return tacticalZoneFillAlpha(baseColor, Math.max(0, screenSpan), hovered);
    }

    public static int zoneFillStrideForTests(int radius) {
        return zoneFillStride(radius);
    }

    public int modelBuildsForTests() {
        return modelBuilds;
    }

    public int terrainModelBuildsForTests() {
        return terrainModelBuilds;
    }

    public static int terrainStyleCacheEntriesForTests() {
        return HoloMapTerrainRenderCache.entryCountForTests();
    }

    public static long terrainStyleCacheBuildsForTests() {
        return HoloMapTerrainRenderCache.buildsForTests();
    }

    public static void clearTerrainStyleCacheForTests() {
        HoloMapTerrainRenderCache.clearForTests();
    }

    public static int styledTerrainAverageForTests(HoloMapTerrainTile tile) {
        return HoloMapTerrainRenderCache.styled(tile).averageColor();
    }

    public static int zoneDrawSpanCountForTests(HoloMapViewState state, HoloMapSnapshotPacket.ZoneData zone) {
        ZonePrimitive primitive = zonePrimitive(state, zone);
        return primitive == null ? 0 : withDrawPlan(primitive, true).drawPlan().fillSpans().size();
    }

    public static int retainedZoneFillSpanCountForTests(HoloMapViewState state,
            HoloMapSnapshotPacket.ZoneData zone, int maxZones) {
        RenderBudget budget = new RenderBudget(0, 0, 0, 0, 0, Math.max(0, maxZones), 0, false, true);
        return buildZones(state, zone == null ? List.of() : List.of(zone), budget).stream()
                .mapToInt(primitive -> primitive.drawPlan().fillSpans().size())
                .sum();
    }

    public static int retainedZoneFillSpanCountForTests(HoloMapViewState state,
            List<HoloMapSnapshotPacket.ZoneData> zones, RenderBudget budget) {
        RenderBudget safeBudget = budget == null ? FULLSCREEN_BUDGET : budget;
        return buildZones(state, zones == null ? List.of() : zones, safeBudget).stream()
                .mapToInt(primitive -> primitive.drawPlan().fillSpans().size())
                .sum();
    }

    public static int zoneDrawPrimitiveCountForTests(HoloMapViewState state, HoloMapSnapshotPacket.ZoneData zone) {
        ZonePrimitive primitive = zonePrimitive(state, zone);
        if (primitive == null) {
            return 0;
        }
        ZoneDrawPlan plan = withDrawPlan(primitive, true).drawPlan();
        return plan.fillSpans().size() + plan.fillLines().size() + plan.patternLines().size()
                + plan.patternDots().size() + plan.contourLines().size() + plan.outlineLines().size();
    }

    public static int zonePatternPrimitiveCountForTests(HoloMapViewState state,
            HoloMapSnapshotPacket.ZoneData zone) {
        ZonePrimitive primitive = zonePrimitive(state, zone);
        if (primitive == null) {
            return 0;
        }
        ZoneDrawPlan plan = withDrawPlan(primitive, true).drawPlan();
        return plan.patternLines().size() + plan.patternDots().size();
    }

    public static boolean zoneDrawPlanClippedToViewportForTests(HoloMapViewState state,
            HoloMapSnapshotPacket.ZoneData zone) {
        ZonePrimitive primitive = zonePrimitive(state, zone);
        return primitive != null && drawPlanWithinBounds(withDrawPlan(primitive, true).drawPlan(),
                primitive.clipBounds());
    }

    public static void clearZoneDrawPlanCacheForTests() {
        synchronized (ZONE_DRAW_PLAN_CACHE) {
            ZONE_DRAW_PLAN_CACHE.clear();
            zoneDrawPlanBuilds = 0L;
        }
    }

    public static int zoneDrawPlanCacheEntriesForTests() {
        synchronized (ZONE_DRAW_PLAN_CACHE) {
            return ZONE_DRAW_PLAN_CACHE.size();
        }
    }

    public static long zoneDrawPlanBuildsForTests() {
        synchronized (ZONE_DRAW_PLAN_CACHE) {
            return zoneDrawPlanBuilds;
        }
    }

    public static int acceptedLabelCountForTests(int[][] rects) {
        LabelPlacer placer = new LabelPlacer(0, 0, 10_000, 10_000);
        int accepted = 0;
        for (int[] rect : rects == null ? new int[0][] : rects) {
            if (rect != null && rect.length >= 4 && placer.claim(rect[0], rect[1], rect[2], rect[3], false)) {
                accepted++;
            }
        }
        return accepted;
    }

    public static boolean zoneWorldPrefilterForTests(HoloMapViewState state,
            HoloMapSnapshotPacket.ZoneData zone) {
        return zonePotentiallyVisibleOrForced(state, zone, visibleWorldBounds(state, 64.0D));
    }

    private HoloMapRenderModel buildModel(HoloMapViewState state, HoloMapSnapshotPacket snapshot,
            List<HoloMapSnapshotPacket.MarkerData> markers, List<HoloMapWaypoint> waypoints, RenderBudget budget) {
        TerrainBuild terrain = buildTerrain(state, budget);
        List<ZonePrimitive> zones = state.showMarkers() && state.fieldMode() != HoloMapVisibility.FieldMode.OFF
                ? buildZones(state, snapshot.zones(), budget)
                : List.of();
        List<OverlayPrimitive> overlays = state.showMarkers() && state.fieldMode() != HoloMapVisibility.FieldMode.OFF
                ? buildOverlays(state, snapshot.overlays(), budget)
                : List.of();
        List<RouteSegmentPrimitive> routes = state.showMarkers()
                ? buildRoutes(state, snapshot.routes(), budget)
                : List.of();
        List<MarkerPrimitive> markerPrimitives = state.showMarkers()
                ? buildMarkers(state, markers, budget)
                : List.of();
        List<WaypointPrimitive> waypointPrimitives = state.showWaypoints()
                ? buildWaypoints(state, waypoints, budget)
                : List.of();
        RenderResult result = new RenderResult(
                terrain.primitives().size(),
                terrain.culledTiles(),
                Math.max(0, visibleOverlayCount(state, snapshot.overlays()) - overlays.size()),
                Math.max(0, visibleZoneCount(state, snapshot.zones()) - zones.size()),
                Math.max(0, visibleRouteSegmentCount(state, snapshot.routes()) - routes.size()),
                Math.max(0, visibleMarkerCount(state, markers) - markerPrimitives.size()),
                Math.max(0, visibleWaypointCount(state, waypoints) - waypointPrimitives.size()),
                markerPrimitives.stream().map(MarkerHit::from).toList(),
                waypointPrimitives.stream().map(WaypointHit::from).toList(),
                terrain.highDetail());
        List<LinePrimitive> grid = buildWorldGrid(state);
        List<LinePrimitive> fallbackGrid = terrain.primitives().isEmpty() ? buildFallbackGrid(state) : List.of();
        return new HoloMapRenderModel(terrain.primitives(), zones, overlays, routes, markerPrimitives, waypointPrimitives,
                grid, fallbackGrid, result);
    }

    private TerrainBuild buildTerrain(HoloMapViewState state, RenderBudget budget) {
        List<HoloMapTerrainTile> tiles = HoloMapTerrainClientState.tiles(state.dimension(),
                state.minChunkX(), state.maxChunkX(), state.minChunkZ(), state.maxChunkZ());
        if (tiles.isEmpty()) {
            TerrainCacheKey emptyKey = TerrainCacheKey.from(state, HoloMapTerrainClientState.revision(), budget, 0);
            terrainCacheKey = emptyKey;
            terrainBuild = TerrainBuild.EMPTY;
            return terrainBuild;
        }
        List<HoloMapTerrainTile> renderable = tiles.stream()
                .filter(HoloMapTerrainTile::renderableSurface)
                .toList();
        if (renderable.isEmpty()) {
            TerrainCacheKey emptyKey = TerrainCacheKey.from(state, HoloMapTerrainClientState.revision(), budget, 0);
            terrainCacheKey = emptyKey;
            terrainBuild = TerrainBuild.EMPTY;
            return terrainBuild;
        }
        boolean highDetail = highDetailTerrainAllowed(state.zoom(), renderable.size(), highDetailTerrainEnabled());
        TerrainCacheKey nextKey = TerrainCacheKey.from(state, HoloMapTerrainClientState.revision(), budget,
                renderable.size(), highDetail);
        if (nextKey.equals(terrainCacheKey)) {
            return terrainBuild;
        }
        List<HoloMapTerrainTile> retained = renderable;
        if (renderable.size() > budget.maxTerrainTiles()) {
            Comparator<HoloMapTerrainTile> nearestFirst = Comparator.comparingDouble(tile ->
                    distanceSquared(state.centerX(), state.centerZ(), tile.chunkX() * 16.0D + 8.0D,
                            tile.chunkZ() * 16.0D + 8.0D));
            retained = boundedTopN(renderable, budget.maxTerrainTiles(), nearestFirst);
        }
        TerrainBuild built = new TerrainBuild(retained.stream()
                .map(tile -> terrainPrimitive(state.zoom(), tile, highDetail))
                .toList(),
                Math.max(0, renderable.size() - retained.size()),
                renderable.size(),
                highDetail);
        terrainCacheKey = nextKey;
        terrainBuild = built;
        terrainModelBuilds++;
        return built;
    }

    private static TerrainPrimitive terrainPrimitive(double zoom, HoloMapTerrainTile tile,
            boolean highDetail) {
        int chunkSize = Math.max(1, (int) Math.ceil(16.0D * zoom));
        HoloMapTerrainRenderCache.StyledTile styled = HoloMapTerrainRenderCache.styled(tile);
        return new TerrainPrimitive(tile, chunkSize, Math.max(1, (int) Math.ceil(zoom)),
                styled.averageColor(), styled, highDetail);
    }

    private static List<ZonePrimitive> buildZones(HoloMapViewState state,
            List<HoloMapSnapshotPacket.ZoneData> zones, RenderBudget budget) {
        if (state.fieldMode() == HoloMapVisibility.FieldMode.OFF) {
            return List.of();
        }
        WorldBounds worldBounds = visibleWorldBounds(state, 64.0D);
        List<ZonePrimitive> result = new ArrayList<>();
        for (HoloMapSnapshotPacket.ZoneData zone : zones) {
            if (!zonePotentiallyVisibleOrForced(state, zone, worldBounds)) {
                continue;
            }
            ZonePrimitive primitive = zonePrimitive(state, zone);
            if (primitive != null && shouldBuildField(state, primitive)) {
                result.add(primitive);
            }
        }
        Comparator<ZonePrimitive> drawOrder = Comparator.comparingInt((ZonePrimitive primitive) ->
                        fieldPriority(state, primitive)).reversed()
                .thenComparingDouble(ZonePrimitive::distance)
                .thenComparing(primitive -> primitive.zone().id().toString());
        List<ZonePrimitive> retained = state.fieldMode() == HoloMapVisibility.FieldMode.AUTO_NEAR
                ? autoNearFieldSelection(state, result, budget, drawOrder)
                : boundedTopN(result, budget.maxZones(), drawOrder);
        ArrayList<ZonePrimitive> planned = new ArrayList<>();
        int filled = 0;
        int fillBudget = autoFieldFillBudget(budget);
        for (ZonePrimitive primitive : retained) {
            boolean forced = forceFullField(state, primitive);
            boolean small = zoneSpan(primitive) <= 120;
            boolean full = forced || small || state.fieldMode() == HoloMapVisibility.FieldMode.ALL
                    || filled < fillBudget;
            planned.add(withDrawPlan(primitive, full));
            if (state.fieldMode() == HoloMapVisibility.FieldMode.AUTO_NEAR && full && !forced) {
                filled++;
            }
        }
        return List.copyOf(planned);
    }

    private static int autoFieldFillBudget(RenderBudget budget) {
        return interactiveBudget(budget) ? 2 : HoloMapVisibility.AUTO_FIELD_FILL_BUDGET;
    }

    private static boolean interactiveBudget(RenderBudget budget) {
        return budget != null
                && budget.maxZones() <= 16
                && budget.maxOverlays() <= 27
                && budget.maxRouteSegments() <= 336;
    }

    private static boolean zonePotentiallyVisibleOrForced(HoloMapViewState state,
            HoloMapSnapshotPacket.ZoneData zone, WorldBounds worldBounds) {
        if (zone == null || state.fieldMode() == HoloMapVisibility.FieldMode.OFF
                || !state.dimension().equals(zone.dimension())
                || !HoloMapVisibility.visibleInNormalView(zone.state())) {
            return false;
        }
        if (selectedField(state.selectedMarkerId(), zone.id())
                || liveCriticalField(zone.id(), zone.sourceId(), zone.title())) {
            return true;
        }
        WorldBounds zoneBounds = zoneWorldBounds(zone);
        return zoneBounds == null || worldBounds.intersects(zoneBounds);
    }

    private static ZonePrimitive zonePrimitive(HoloMapViewState state, HoloMapSnapshotPacket.ZoneData zone) {
        if (!state.dimension().equals(zone.dimension()) || !HoloMapVisibility.visibleInNormalView(zone.state())) {
            return null;
        }
        int cx = state.worldToScreenX(zone.x());
        int cy = state.worldToScreenZ(zone.z());
        int radius = Math.max(0, Math.min(960, (int) Math.round(zone.radius() * state.zoom())));
        int halfW = Math.max(radius, (int) Math.round(zone.width() * state.zoom() / 2.0D));
        int halfH = Math.max(radius, (int) Math.round(zone.depth() * state.zoom() / 2.0D));
        List<ScreenPoint> points = zone.points().stream()
                .filter(point -> state.dimension().equals(point.dimension()))
                .map(point -> new ScreenPoint(state.worldToScreenX(point.x()), state.worldToScreenZ(point.z())))
                .toList();
        Bounds bounds = zoneBounds(zone.shape(), cx, cy, radius, halfW, halfH, points);
        if (bounds.maxX() < state.x() - 8 || bounds.minX() > state.x() + state.width() + 8
                || bounds.maxY() < state.y() - 8 || bounds.minY() > state.y() + state.height() + 8) {
            return null;
        }
        int fill = zone.fillColor();
        int outline = zone.outlineColor();
        Bounds clipBounds = viewportBounds(state);
        return new ZonePrimitive(zone, cx, cy, radius, halfW, halfH, fill, outline, points, bounds,
                clipBounds, distance(state.centerX(), state.centerZ(), zone.x(), zone.z()), zone.priority(),
                ZoneDrawPlan.EMPTY);
    }

    private static ZonePrimitive withDrawPlan(ZonePrimitive primitive, boolean full) {
        ZoneDrawPlan plan = cachedZoneDrawPlan(primitive, full);
        return new ZonePrimitive(primitive.zone(), primitive.x(), primitive.y(), primitive.radius(),
                primitive.halfWidth(), primitive.halfHeight(), primitive.fillColor(), primitive.outlineColor(),
                primitive.points(), primitive.bounds(), primitive.clipBounds(), primitive.distance(),
                primitive.priority(), plan);
    }

    private static ZoneDrawPlan cachedZoneDrawPlan(ZonePrimitive primitive, boolean full) {
        ZoneDrawPlanKey key = ZoneDrawPlanKey.from(primitive, full);
        synchronized (ZONE_DRAW_PLAN_CACHE) {
            ZoneDrawPlan cached = ZONE_DRAW_PLAN_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        ZoneDrawPlan built = full ? buildZoneDrawPlan(primitive) : buildZoneOutlinePlan(primitive);
        synchronized (ZONE_DRAW_PLAN_CACHE) {
            ZoneDrawPlan cached = ZONE_DRAW_PLAN_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
            ZONE_DRAW_PLAN_CACHE.put(key, built);
            zoneDrawPlanBuilds++;
            while (ZONE_DRAW_PLAN_CACHE.size() > ZONE_DRAW_PLAN_CACHE_MAX_ENTRIES) {
                ZONE_DRAW_PLAN_CACHE.remove(ZONE_DRAW_PLAN_CACHE.keySet().iterator().next());
            }
        }
        return built;
    }

    private static Bounds viewportBounds(HoloMapViewState state) {
        return new Bounds(state.x() + 4, state.y() + 4, state.x() + state.width() - 4,
                state.y() + state.height() - 4);
    }

    private static WorldBounds visibleWorldBounds(HoloMapViewState state, double paddingBlocks) {
        double left = state.screenToWorldX(state.x() + 4);
        double right = state.screenToWorldX(state.x() + state.width() - 4);
        double top = state.screenToWorldZ(state.y() + 4);
        double bottom = state.screenToWorldZ(state.y() + state.height() - 4);
        double padding = Math.max(0.0D, paddingBlocks);
        return new WorldBounds(Math.min(left, right) - padding, Math.min(top, bottom) - padding,
                Math.max(left, right) + padding, Math.max(top, bottom) + padding);
    }

    private static WorldBounds markerWorldBounds(HoloMapViewState state, RenderBudget budget) {
        double halfWidth = state.width() / (2.0D * Math.max(0.25D, state.zoom()));
        double halfHeight = state.height() / (2.0D * Math.max(0.25D, state.zoom()));
        double padding = budget.edgeIndicators()
                ? Math.max(512.0D, Math.max(halfWidth, halfHeight) * 2.0D)
                : 64.0D;
        return visibleWorldBounds(state, padding);
    }

    private static WorldBounds zoneWorldBounds(HoloMapSnapshotPacket.ZoneData zone) {
        if ((zone.shape() == HoloMapZoneShape.POLYGON || zone.shape() == HoloMapZoneShape.CORRIDOR)
                && !zone.points().isEmpty()) {
            double minX = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            int points = 0;
            for (HoloMapSnapshotPacket.ZonePointData point : zone.points()) {
                if (!zone.dimension().equals(point.dimension())) {
                    continue;
                }
                minX = Math.min(minX, point.x());
                minZ = Math.min(minZ, point.z());
                maxX = Math.max(maxX, point.x());
                maxZ = Math.max(maxZ, point.z());
                points++;
            }
            if (points > 0) {
                double padding = zone.shape() == HoloMapZoneShape.CORRIDOR
                        ? Math.max(8.0D, zone.radius())
                        : 4.0D;
                return new WorldBounds(minX - padding, minZ - padding, maxX + padding, maxZ + padding);
            }
        }
        double halfWidth = zone.shape() == HoloMapZoneShape.RECT
                ? Math.max(1.0D, zone.width() / 2.0D)
                : Math.max(1.0D, zone.radius());
        double halfHeight = zone.shape() == HoloMapZoneShape.RECT
                ? Math.max(1.0D, zone.depth() / 2.0D)
                : Math.max(1.0D, zone.radius());
        return new WorldBounds(zone.x() - halfWidth, zone.z() - halfHeight,
                zone.x() + halfWidth, zone.z() + halfHeight);
    }

    private static List<ZonePrimitive> autoNearFieldSelection(HoloMapViewState state, List<ZonePrimitive> candidates,
            RenderBudget budget, Comparator<ZonePrimitive> drawOrder) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        ArrayList<ZonePrimitive> forced = new ArrayList<>();
        ArrayList<ZonePrimitive> regular = new ArrayList<>();
        for (ZonePrimitive primitive : candidates) {
            if (forceFullField(state, primitive)) {
                forced.add(primitive);
            } else {
                regular.add(primitive);
            }
        }
        int regularLimit = Math.max(0, Math.min(budget.maxZones(), HoloMapVisibility.AUTO_FIELD_OUTLINE_BUDGET)
                - forced.size());
        ArrayList<ZonePrimitive> retained = new ArrayList<>(forced);
        retained.addAll(boundedTopN(regular, regularLimit, drawOrder));
        retained.sort(drawOrder);
        return List.copyOf(retained);
    }

    private static boolean shouldBuildField(HoloMapViewState state, ZonePrimitive primitive) {
        if (state.fieldMode() == HoloMapVisibility.FieldMode.ALL) {
            return true;
        }
        if (state.fieldMode() == HoloMapVisibility.FieldMode.OFF) {
            return false;
        }
        return forceFullField(state, primitive) || nearPlayerField(state, primitive);
    }

    private static boolean forceFullField(HoloMapViewState state, ZonePrimitive primitive) {
        return selectedField(state.selectedMarkerId(), primitive.zone().id())
                || liveCriticalField(primitive.zone().id(), primitive.zone().sourceId(), primitive.zone().title());
    }

    private static int fieldPriority(HoloMapViewState state, ZonePrimitive primitive) {
        int score = primitive.priority();
        if (selectedField(state.selectedMarkerId(), primitive.zone().id())) {
            score += 10_000;
        }
        if (liveCriticalField(primitive.zone().id(), primitive.zone().sourceId(), primitive.zone().title())) {
            score += 5_000;
        }
        if (nearPlayerField(state, primitive)) {
            score += 1_000;
        }
        return score;
    }

    private static boolean nearPlayerField(HoloMapViewState state, ZonePrimitive primitive) {
        double worldRadius = Math.max(primitive.zone().radius(),
                Math.max(primitive.zone().width(), primitive.zone().depth()) / 2.0D);
        return distance(state.playerX(), state.playerZ(), primitive.zone().x(), primitive.zone().z())
                <= HoloMapVisibility.AUTO_FIELD_RADIUS_BLOCKS + Math.max(8.0D, worldRadius);
    }

    private static boolean selectedField(String selectedMarkerId, Identifier fieldId) {
        if (selectedMarkerId == null || selectedMarkerId.isBlank() || fieldId == null) {
            return false;
        }
        String normalizedSelected = selectedMarkerId.replace(':', '/');
        String normalizedField = fieldId.toString().replace(':', '/');
        return normalizedField.equals(normalizedSelected)
                || normalizedField.endsWith("/" + normalizedSelected)
                || normalizedField.contains("/" + normalizedSelected + "/")
                || normalizedField.contains("/overlay/" + normalizedSelected)
                || normalizedField.contains("/zone/" + normalizedSelected);
    }

    private static boolean liveCriticalField(Identifier id, Identifier sourceId, String title) {
        String text = ((id == null ? "" : id.toString()) + " "
                + (sourceId == null ? "" : sourceId.toString()) + " "
                + (title == null ? "" : title)).toLowerCase(Locale.ROOT);
        return text.contains("live_hazard")
                || text.contains("live hazard")
                || text.contains("telemetry")
                || text.contains("world_snapshot")
                || text.contains("world hazard");
    }

    private static Bounds zoneBounds(HoloMapZoneShape shape, int cx, int cy, int radius, int halfW, int halfH,
            List<ScreenPoint> points) {
        if ((shape == HoloMapZoneShape.POLYGON || shape == HoloMapZoneShape.CORRIDOR) && !points.isEmpty()) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (ScreenPoint point : points) {
                minX = Math.min(minX, point.x());
                minY = Math.min(minY, point.y());
                maxX = Math.max(maxX, point.x());
                maxY = Math.max(maxY, point.y());
            }
            int pad = shape == HoloMapZoneShape.CORRIDOR ? Math.max(8, radius) : 4;
            return new Bounds(minX - pad, minY - pad, maxX + pad, maxY + pad);
        }
        int xRadius = shape == HoloMapZoneShape.RECT ? Math.max(1, halfW) : Math.max(1, radius);
        int yRadius = shape == HoloMapZoneShape.RECT ? Math.max(1, halfH) : Math.max(1, radius);
        return new Bounds(cx - xRadius, cy - yRadius, cx + xRadius, cy + yRadius);
    }

    private static List<OverlayPrimitive> buildOverlays(HoloMapViewState state,
            List<HoloMapSnapshotPacket.OverlayData> overlays, RenderBudget budget) {
        if (state.fieldMode() == HoloMapVisibility.FieldMode.OFF) {
            return List.of();
        }
        WorldBounds worldBounds = visibleWorldBounds(state, 64.0D);
        List<OverlayPrimitive> result = new ArrayList<>();
        for (HoloMapSnapshotPacket.OverlayData overlay : overlays) {
            if (!state.dimension().equals(overlay.dimension())
                    || !HoloMapVisibility.visibleInNormalView(overlay.state())
                    || !overlayPotentiallyVisibleOrForced(state, overlay, worldBounds)
                    || !shouldBuildOverlayField(state, overlay)) {
                continue;
            }
            int cx = state.worldToScreenX(overlay.x());
            int cy = state.worldToScreenZ(overlay.z());
            int radius = Math.max(4, Math.min(640, (int) Math.round(overlay.radius() * state.zoom())));
            if (cx + radius < state.x() || cx - radius > state.x() + state.width()
                    || cy + radius < state.y() || cy - radius > state.y() + state.height()) {
                continue;
            }
            int alpha = overlay.precision() == HoloMapPrecision.VIRTUAL ? 0x50 : 0x72;
            int color = withAlpha(overlay.color(), alpha);
            result.add(new OverlayPrimitive(cx, cy, radius, color, ringLines(cx, cy, radius, viewportBounds(state)),
                    overlay.precision() != HoloMapPrecision.PRECISE,
                    distance(state.centerX(), state.centerZ(), overlay.x(), overlay.z()),
                    overlayPriority(state, overlay)));
        }
        int limit = state.fieldMode() == HoloMapVisibility.FieldMode.AUTO_NEAR
                ? Math.min(budget.maxOverlays(), HoloMapVisibility.AUTO_FIELD_OUTLINE_BUDGET)
                : budget.maxOverlays();
        return boundedTopN(result, limit, Comparator.comparingInt(OverlayPrimitive::priority).reversed()
                .thenComparingDouble(OverlayPrimitive::distance)
                .thenComparingInt(OverlayPrimitive::radius));
    }

    private static boolean overlayPotentiallyVisibleOrForced(HoloMapViewState state,
            HoloMapSnapshotPacket.OverlayData overlay, WorldBounds worldBounds) {
        if (selectedField(state.selectedMarkerId(), overlay.id())
                || liveCriticalField(overlay.id(), overlay.sourceId(), overlay.title())) {
            return true;
        }
        return worldBounds.intersectsCircle(overlay.x(), overlay.z(), Math.max(8.0D, overlay.radius()));
    }

    private static boolean shouldBuildOverlayField(HoloMapViewState state, HoloMapSnapshotPacket.OverlayData overlay) {
        if (state.fieldMode() == HoloMapVisibility.FieldMode.ALL) {
            return true;
        }
        if (state.fieldMode() == HoloMapVisibility.FieldMode.OFF) {
            return false;
        }
        return selectedField(state.selectedMarkerId(), overlay.id())
                || liveCriticalField(overlay.id(), overlay.sourceId(), overlay.title())
                || distance(state.playerX(), state.playerZ(), overlay.x(), overlay.z())
                        <= HoloMapVisibility.AUTO_FIELD_RADIUS_BLOCKS + Math.max(8.0D, overlay.radius());
    }

    private static int overlayPriority(HoloMapViewState state, HoloMapSnapshotPacket.OverlayData overlay) {
        int score = switch (overlay.kind()) {
            case HAZARD -> 90;
            case REGION -> 70;
            case SCAN -> 60;
            case ROUTE_CORRIDOR -> 55;
            case CIRCLE -> 25;
        };
        if (selectedField(state.selectedMarkerId(), overlay.id())) {
            score += 10_000;
        }
        if (liveCriticalField(overlay.id(), overlay.sourceId(), overlay.title())) {
            score += 5_000;
        }
        return score;
    }

    private static List<RouteSegmentPrimitive> buildRoutes(HoloMapViewState state,
            List<HoloMapSnapshotPacket.RouteData> routes, RenderBudget budget) {
        WorldBounds worldBounds = visibleWorldBounds(state, 48.0D);
        List<RouteSegmentPrimitive> result = new ArrayList<>();
        for (HoloMapSnapshotPacket.RouteData route : routes) {
            if (!HoloMapVisibility.visibleInNormalView(route.state())) {
                continue;
            }
            int color = withAlpha(route.color(), 0xAA);
            HoloMapSnapshotPacket.RoutePointData previous = null;
            for (HoloMapSnapshotPacket.RoutePointData current : route.points()) {
                if (!state.dimension().equals(current.dimension())) {
                    continue;
                }
                if (previous == null) {
                    previous = current;
                    continue;
                }
                if (!worldBounds.intersectsSegment(previous.x(), previous.z(), current.x(), current.z(), 24.0D)) {
                    previous = current;
                    continue;
                }
                int x0 = state.worldToScreenX(previous.x());
                int y0 = state.worldToScreenZ(previous.z());
                int x1 = state.worldToScreenX(current.x());
                int y1 = state.worldToScreenZ(current.z());
                if ((x0 < state.x() - 16 && x1 < state.x() - 16)
                        || (x0 > state.x() + state.width() + 16 && x1 > state.x() + state.width() + 16)
                        || (y0 < state.y() - 16 && y1 < state.y() - 16)
                        || (y0 > state.y() + state.height() + 16 && y1 > state.y() + state.height() + 16)) {
                    previous = current;
                    continue;
                }
                result.add(new RouteSegmentPrimitive(x0, y0, x1, y1, color,
                        distance(state.centerX(), state.centerZ(), (previous.x() + current.x()) / 2.0D,
                                (previous.z() + current.z()) / 2.0D)));
                previous = current;
            }
        }
        return boundedTopN(result, budget.maxRouteSegments(), Comparator.comparingDouble(RouteSegmentPrimitive::distance));
    }

    private static List<MarkerPrimitive> buildMarkers(HoloMapViewState state,
            List<HoloMapSnapshotPacket.MarkerData> markers, RenderBudget budget) {
        Comparator<MarkerPrimitive> drawOrder = Comparator.comparingInt(MarkerPrimitive::priority).reversed()
                .thenComparingDouble(MarkerPrimitive::distance)
                .thenComparing(primitive -> primitive.marker().title());
        WorldBounds worldBounds = markerWorldBounds(state, budget);
        ArrayList<MarkerPrimitive> candidates = new ArrayList<>();
        for (HoloMapSnapshotPacket.MarkerData marker : markers) {
            if (!state.dimension().equals(marker.dimension())
                    || !HoloMapVisibility.visibleInNormalView(marker.state())
                    || !markerPotentiallyVisibleOrForced(state, marker, worldBounds)) {
                continue;
            }
            MarkerPrimitive primitive = markerPrimitive(state, marker, budget);
            if (primitive != null) {
                candidates.add(primitive);
            }
        }
        return boundedTopN(candidates, budget.maxMarkers(), drawOrder);
    }

    private static boolean markerPotentiallyVisibleOrForced(HoloMapViewState state,
            HoloMapSnapshotPacket.MarkerData marker, WorldBounds worldBounds) {
        if (marker.id().toString().equals(state.selectedMarkerId())) {
            return true;
        }
        return worldBounds.intersectsCircle(marker.x(), marker.z(), Math.max(8.0D, marker.radius()));
    }

    private static MarkerPrimitive markerPrimitive(HoloMapViewState state, HoloMapSnapshotPacket.MarkerData marker,
            RenderBudget budget) {
        int px = state.worldToScreenX(marker.x());
        int py = state.worldToScreenZ(marker.z());
        boolean onscreen = state.inViewport(px, py, 48);
        if (!onscreen && !budget.edgeIndicators()) {
            return null;
        }
        double distance = distance(state.centerX(), state.centerZ(), marker.x(), marker.z());
        boolean selected = marker.id().toString().equals(state.selectedMarkerId());
        int priority = marker.priority() + markerPriority(marker, selected, distance);
        int color = HoloMapVisualStyle.markerColor(null, marker);
        if (marker.precision() == HoloMapPrecision.VIRTUAL) {
            color = withAlpha(color, 0x72);
        }
        return new MarkerPrimitive(marker, px, py, clampToX(state, px), clampToY(state, py),
                onscreen, selected, color, priority, distance);
    }

    private static int markerPriority(HoloMapSnapshotPacket.MarkerData marker, boolean selected, double distance) {
        int score = selected ? 1000 : 0;
        if (distance <= 128.0D) {
            score += 120;
        }
        score += switch (marker.kind()) {
            case MISSION -> 80;
            case HAZARD -> 70;
            case ROUTE -> 60;
            case CRASH_SITE, BASE_OUTPOST -> 45;
            case REGION, ORBITAL_SCAN, NEXUS_ANOMALY, DRONE_SCAN -> 30;
            case GENERIC -> 10;
        };
        if (marker.precision() == HoloMapPrecision.VIRTUAL) {
            score -= 25;
        }
        return score;
    }

    private static List<WaypointPrimitive> buildWaypoints(HoloMapViewState state, List<HoloMapWaypoint> waypoints,
            RenderBudget budget) {
        Comparator<WaypointPrimitive> drawOrder = Comparator.comparing(WaypointPrimitive::selected).reversed()
                .thenComparing(WaypointPrimitive::deathpoint).reversed()
                .thenComparingDouble(WaypointPrimitive::distance)
                .thenComparing(primitive -> primitive.waypoint().title());
        WorldBounds worldBounds = markerWorldBounds(state, budget);
        ArrayList<WaypointPrimitive> candidates = new ArrayList<>();
        for (HoloMapWaypoint waypoint : waypoints) {
            if (!waypoint.visible()
                    || !waypoint.inDimension(state.dimension())
                    || !waypointPotentiallyVisibleOrForced(state, waypoint, worldBounds)) {
                continue;
            }
            WaypointPrimitive primitive = waypointPrimitive(state, waypoint, budget);
            if (primitive != null) {
                candidates.add(primitive);
            }
        }
        return boundedTopN(candidates, budget.maxWaypoints(), drawOrder);
    }

    private static boolean waypointPotentiallyVisibleOrForced(HoloMapViewState state,
            HoloMapWaypoint waypoint, WorldBounds worldBounds) {
        if (waypoint.id().toString().equals(state.selectedWaypointId())) {
            return true;
        }
        return worldBounds.contains(waypoint.x(), waypoint.z());
    }

    private static WaypointPrimitive waypointPrimitive(HoloMapViewState state, HoloMapWaypoint waypoint,
            RenderBudget budget) {
        int px = state.worldToScreenX(waypoint.x());
        int py = state.worldToScreenZ(waypoint.z());
        boolean onscreen = state.inViewport(px, py, 32);
        if (!onscreen && !budget.edgeIndicators()) {
            return null;
        }
        boolean selected = waypoint.id().toString().equals(state.selectedWaypointId());
        double distance = distance(state.centerX(), state.centerZ(), waypoint.x(), waypoint.z());
        return new WaypointPrimitive(waypoint, px, py, clampToX(state, px), clampToY(state, py),
                onscreen, selected, waypoint.color(), distance, waypoint.isDeathpoint());
    }

    private static void draw(GuiGraphicsExtractor graphics, Font font, HoloMapViewState state,
            HoloMapRenderModel model, RenderBudget budget) {
        graphics.fill(state.x() + 3, state.y() + 3, state.x() + state.width() - 3,
                state.y() + state.height() - 3, 0xCC061014);
        if (model.terrain().isEmpty()) {
            drawLines(graphics, model.fallbackGrid(), 0);
        } else {
            for (TerrainPrimitive terrain : model.terrain()) {
                drawTerrain(graphics, state, terrain);
            }
        }
        drawLines(graphics, model.grid(), 0);
        LabelPlacer labelPlacer = new LabelPlacer(state.x() + 4, state.y() + 4,
                state.width() - 8, state.height() - 8);
        reserveLabelQuietZones(labelPlacer, state, budget);
        ZonePrimitive hoveredZone = hoveredZone(state, model.zones());
        for (ZonePrimitive zone : model.zones()) {
            drawZone(graphics, zone, zone == hoveredZone);
        }
        if (font != null && hoveredZone != null) {
            int labelX = Math.max(state.x() + 8, Math.min(hoveredZone.bounds().minX() + 4,
                    state.x() + state.width() - 148));
            int labelY = Math.max(state.y() + 8, Math.min(hoveredZone.bounds().minY() + 4,
                    state.y() + state.height() - 16));
            labelPlacer.draw(graphics, font, hoveredZone.zone().title(), labelX, labelY, 142,
                    HoloMapVisualStyle.TEXT, true);
        }
        for (OverlayPrimitive overlay : model.overlays()) {
            drawLines(graphics, overlay.ring(), overlay.color());
            if (overlay.estimated() && overlay.radius() > 14) {
                drawRing(graphics, overlay.x(), overlay.y(), Math.max(4, overlay.radius() - 5),
                        withAlpha(overlay.color(), 0x34));
            }
        }
        for (RouteSegmentPrimitive route : model.routes()) {
            HoloMapGlyphRenderer.drawLine(graphics, route.x0(), route.y0(), route.x1(), route.y1(), route.color());
        }
        int labels = budget.labelLimit();
        int drawnLabels = 0;
        int[] markerEdgeSectors = new int[8];
        int edgeLimit = edgeIndicatorLimit(budget);
        for (MarkerPrimitive marker : model.markers()) {
            if (!marker.onscreen()) {
                if (marker.selected() || claimEdgeIndicator(state, marker.edgeX(), marker.edgeY(),
                        markerEdgeSectors, edgeLimit)) {
                    HoloMapGlyphRenderer.drawEdgeIndicator(graphics, marker.edgeX(), marker.edgeY(), marker.color());
                }
                continue;
            }
            boolean hovered = inside(state.mouseX(), state.mouseY(), marker.x() - 6, marker.y() - 6, 12, 12);
            HoloMapGlyphRenderer.drawMarker(graphics, marker.marker(), marker.x(), marker.y(),
                    marker.color(), 5, marker.selected() || hovered);
            boolean forced = marker.selected() || hovered;
            if (font != null && (forced || marker.marker().precise() && drawnLabels < labels)) {
                boolean placed = labelPlacer.draw(graphics, font, marker.marker().title(), marker.x() + 9,
                        marker.y() - 4, 132, marker.selected() ? HoloMapVisualStyle.TEXT : marker.color(), forced);
                if (placed && !forced) {
                    drawnLabels++;
                }
            }
        }
        int waypointLabels = Math.max(0, labels / 2);
        int drawnWaypointLabels = 0;
        int[] waypointEdgeSectors = new int[8];
        for (WaypointPrimitive waypoint : model.waypoints()) {
            if (!waypoint.onscreen()) {
                if (waypoint.selected() || claimEdgeIndicator(state, waypoint.edgeX(), waypoint.edgeY(),
                        waypointEdgeSectors, edgeLimit)) {
                    HoloMapGlyphRenderer.drawEdgeIndicator(graphics, waypoint.edgeX(), waypoint.edgeY(),
                            waypoint.color());
                }
                continue;
            }
            boolean hovered = inside(state.mouseX(), state.mouseY(), waypoint.x() - 7, waypoint.y() - 7, 14, 14);
            HoloMapGlyphRenderer.drawWaypoint(graphics, waypoint.waypoint(), waypoint.x(), waypoint.y(),
                    waypoint.color(), 5, waypoint.selected() || hovered);
            boolean forced = waypoint.selected() || hovered;
            if (font != null && (forced || drawnWaypointLabels < waypointLabels)) {
                boolean placed = labelPlacer.draw(graphics, font, waypoint.waypoint().title(), waypoint.x() + 9,
                        waypoint.y() - 4, 132, waypoint.selected() ? HoloMapVisualStyle.TEXT : waypoint.color(), forced);
                if (placed && !forced) {
                    drawnWaypointLabels++;
                }
            }
        }
        if (budget.drawPlayer()) {
            drawPlayer(graphics, state);
        }
    }

    private static void reserveLabelQuietZones(LabelPlacer labelPlacer, HoloMapViewState state, RenderBudget budget) {
        if (labelPlacer == null || state == null) {
            return;
        }
        int centerX = state.x() + state.width() / 2;
        int centerY = state.y() + state.height() / 2;
        labelPlacer.claim(centerX - 64, centerY - 26, 128, 52, false);
        if (budget.drawPlayer()) {
            int playerX = state.worldToScreenX(state.playerX());
            int playerY = state.worldToScreenZ(state.playerZ());
            if (state.inViewport(playerX, playerY, 24)) {
                labelPlacer.claim(playerX - 46, playerY - 30, 92, 60, false);
            }
        }
    }

    private static void drawZone(GuiGraphicsExtractor graphics, ZonePrimitive zone, boolean hovered) {
        ZoneVisualStyle style = tacticalZoneStyle(zone, hovered);
        ZoneDrawPlan plan = zone.drawPlan();
        for (FillSpan span : plan.fillSpans()) {
            graphics.fill(span.x0(), span.y0(), span.x1(), span.y1(), style.fillColor());
        }
        for (LinePrimitive line : plan.fillLines()) {
            HoloMapGlyphRenderer.drawLine(graphics, line.x0(), line.y0(), line.x1(), line.y1(), style.fillColor());
        }
        for (LinePrimitive line : plan.patternLines()) {
            HoloMapGlyphRenderer.drawLine(graphics, line.x0(), line.y0(), line.x1(), line.y1(), style.patternColor());
        }
        for (FillSpan dot : plan.patternDots()) {
            graphics.fill(dot.x0(), dot.y0(), dot.x1(), dot.y1(), style.patternColor());
        }
        for (LinePrimitive line : plan.contourLines()) {
            HoloMapGlyphRenderer.drawLine(graphics, line.x0(), line.y0(), line.x1(), line.y1(), style.contourColor());
        }
        for (LinePrimitive line : plan.outlineLines()) {
            HoloMapGlyphRenderer.drawLine(graphics, line.x0(), line.y0(), line.x1(), line.y1(), style.outlineColor());
        }
    }

    private static ZoneDrawPlan buildZoneDrawPlan(ZonePrimitive zone) {
        ArrayList<FillSpan> fillSpans = new ArrayList<>();
        ArrayList<LinePrimitive> fillLines = new ArrayList<>();
        ArrayList<LinePrimitive> patternLines = new ArrayList<>();
        ArrayList<FillSpan> patternDots = new ArrayList<>();
        ArrayList<LinePrimitive> contourLines = new ArrayList<>();
        ArrayList<LinePrimitive> outlineLines = new ArrayList<>();
        int span = zoneSpan(zone);
        int fillStride = zoneFillStride(Math.max(zone.radius(), span / 2));
        switch (zone.zone().shape()) {
            case POLYGON -> buildPolygonZoneDrawPlan(zone, fillStride, fillSpans, outlineLines);
            case CORRIDOR -> buildCorridorZoneDrawPlan(zone, fillStride, fillSpans, fillLines, outlineLines);
            case RECT -> buildRectZoneDrawPlan(zone, fillStride, fillSpans, outlineLines);
            case CIRCLE -> buildCircleZoneDrawPlan(zone, fillStride, fillSpans, contourLines, outlineLines);
        }
        buildZonePatternPlan(zone, zonePatternStep(zone), patternLines, patternDots);
        return new ZoneDrawPlan(List.copyOf(fillSpans), List.copyOf(fillLines), List.copyOf(patternLines),
                List.copyOf(patternDots), List.copyOf(contourLines), List.copyOf(outlineLines));
    }

    private static ZoneDrawPlan buildZoneOutlinePlan(ZonePrimitive zone) {
        ArrayList<LinePrimitive> patternLines = new ArrayList<>();
        ArrayList<FillSpan> patternDots = new ArrayList<>();
        ArrayList<LinePrimitive> contourLines = new ArrayList<>();
        ArrayList<LinePrimitive> outlineLines = new ArrayList<>();
        switch (zone.zone().shape()) {
            case POLYGON -> {
                List<ScreenPoint> points = zone.points();
                if (points.size() < 3) {
                    addCircleOutlines(zone, contourLines, outlineLines);
                } else {
                    for (int i = 0; i < points.size(); i++) {
                        ScreenPoint a = points.get(i);
                        ScreenPoint b = points.get((i + 1) % points.size());
                        addClippedLine(outlineLines, a.x(), a.y(), b.x(), b.y(), zone.clipBounds());
                    }
                }
            }
            case CORRIDOR -> {
                List<ScreenPoint> points = zone.points();
                if (points.size() < 2) {
                    addCircleOutlines(zone, contourLines, outlineLines);
                } else {
                    for (int i = 1; i < points.size(); i++) {
                        ScreenPoint previous = points.get(i - 1);
                        ScreenPoint current = points.get(i);
                        addClippedLine(outlineLines, previous.x(), previous.y(), current.x(), current.y(),
                                zone.clipBounds());
                    }
                }
            }
            case RECT -> {
                int left = zone.x() - Math.max(1, zone.halfWidth());
                int top = zone.y() - Math.max(1, zone.halfHeight());
                int right = zone.x() + Math.max(1, zone.halfWidth());
                int bottom = zone.y() + Math.max(1, zone.halfHeight());
                outlineLines.addAll(rectLines(left, top, right, bottom, zone.clipBounds()));
            }
            case CIRCLE -> addCircleOutlines(zone, contourLines, outlineLines);
        }
        if (zone.zone().pattern() != HoloMapZonePattern.SOLID) {
            buildZonePatternPlan(zone, Math.max(96, zonePatternStep(zone) * 2), patternLines, patternDots);
        }
        return new ZoneDrawPlan(List.of(), List.of(), List.copyOf(patternLines), List.copyOf(patternDots),
                List.copyOf(contourLines), List.copyOf(outlineLines));
    }

    private static void addCircleOutlines(ZonePrimitive zone, List<LinePrimitive> contourLines,
            List<LinePrimitive> outlineLines) {
        int radius = Math.max(1, zone.radius());
        if (radius > 220) {
            contourLines.addAll(ringLines(zone.x(), zone.y(), Math.max(4, radius * 2 / 3),
                    zone.clipBounds()));
        }
        if (radius > 480) {
            contourLines.addAll(ringLines(zone.x(), zone.y(), Math.max(4, radius / 3), zone.clipBounds()));
        }
        outlineLines.addAll(ringLines(zone.x(), zone.y(), radius, zone.clipBounds()));
    }

    private static void buildCircleZoneDrawPlan(ZonePrimitive zone, int stride, List<FillSpan> fillSpans,
            List<LinePrimitive> contourLines, List<LinePrimitive> outlineLines) {
        int radius = Math.max(1, zone.radius());
        Bounds clip = zone.clipBounds();
        int minDy = Math.max(-radius, clip.minY() - zone.y());
        int maxDy = Math.min(radius, clip.maxY() - zone.y());
        int startDy = alignedStart(minDy, -radius, stride);
        for (int dy = startDy; dy <= maxDy; dy += stride) {
            int half = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            int y = zone.y() + dy;
            int x0 = Math.max(zone.x() - half, clip.minX());
            int x1 = Math.min(zone.x() + half, clip.maxX());
            int y1 = Math.min(y + stride, clip.maxY());
            if (drawZoneFillBand(zone.zone().pattern(), x0, y, x1 - x0, stride) && y1 > y) {
                fillSpans.add(new FillSpan(x0, y, x1, y1));
            }
        }
        if (radius > 220) {
            contourLines.addAll(ringLines(zone.x(), zone.y(), Math.max(4, radius * 2 / 3), clip));
        }
        if (radius > 480) {
            contourLines.addAll(ringLines(zone.x(), zone.y(), Math.max(4, radius / 3), clip));
        }
        outlineLines.addAll(ringLines(zone.x(), zone.y(), radius, clip));
    }

    private static void buildRectZoneDrawPlan(ZonePrimitive zone, int stride, List<FillSpan> fillSpans,
            List<LinePrimitive> outlineLines) {
        int left = zone.x() - Math.max(1, zone.halfWidth());
        int top = zone.y() - Math.max(1, zone.halfHeight());
        int right = zone.x() + Math.max(1, zone.halfWidth());
        int bottom = zone.y() + Math.max(1, zone.halfHeight());
        Bounds clip = zone.clipBounds();
        int minY = Math.max(top, clip.minY());
        int maxY = Math.min(bottom, clip.maxY());
        int x0 = Math.max(left, clip.minX());
        int x1 = Math.min(right, clip.maxX());
        for (int y = alignedStart(minY, top, stride); y <= maxY; y += stride) {
            int y1 = Math.min(y + stride, clip.maxY());
            if (drawZoneFillBand(zone.zone().pattern(), x0, y, x1 - x0, stride) && y1 > y) {
                fillSpans.add(new FillSpan(x0, y, x1, y1));
            }
        }
        outlineLines.addAll(rectLines(left, top, right, bottom, clip));
    }

    private static void buildPolygonZoneDrawPlan(ZonePrimitive zone, int stride, List<FillSpan> fillSpans,
            List<LinePrimitive> outlineLines) {
        List<ScreenPoint> points = zone.points();
        if (points.size() < 3) {
            buildCircleZoneDrawPlan(zone, stride, fillSpans, new ArrayList<>(), outlineLines);
            return;
        }
        ArrayList<Integer> intersections = new ArrayList<>();
        Bounds clip = zone.clipBounds();
        int minY = Math.max(zone.bounds().minY(), clip.minY());
        int maxY = Math.min(zone.bounds().maxY(), clip.maxY());
        for (int y = alignedStart(minY, zone.bounds().minY(), stride); y <= maxY; y += stride) {
            intersections.clear();
            for (int i = 0; i < points.size(); i++) {
                ScreenPoint a = points.get(i);
                ScreenPoint b = points.get((i + 1) % points.size());
                if ((a.y() <= y && b.y() > y) || (b.y() <= y && a.y() > y)) {
                    double t = (y - a.y()) / (double) (b.y() - a.y());
                    intersections.add((int) Math.round(a.x() + t * (b.x() - a.x())));
                }
            }
            intersections.sort(Integer::compareTo);
            for (int i = 1; i < intersections.size(); i += 2) {
                int x0 = Math.max(intersections.get(i - 1), clip.minX());
                int x1 = Math.min(intersections.get(i), clip.maxX());
                int y1 = Math.min(y + stride, clip.maxY());
                if (drawZoneFillBand(zone.zone().pattern(), x0, y, x1 - x0, stride) && y1 > y) {
                    fillSpans.add(new FillSpan(x0, y, x1, y1));
                }
            }
        }
        for (int i = 0; i < points.size(); i++) {
            ScreenPoint a = points.get(i);
            ScreenPoint b = points.get((i + 1) % points.size());
            addClippedLine(outlineLines, a.x(), a.y(), b.x(), b.y(), clip);
        }
    }

    private static void buildCorridorZoneDrawPlan(ZonePrimitive zone, int stride, List<FillSpan> fillSpans,
            List<LinePrimitive> fillLines, List<LinePrimitive> outlineLines) {
        List<ScreenPoint> points = zone.points();
        if (points.size() < 2) {
            buildCircleZoneDrawPlan(zone, stride, fillSpans, new ArrayList<>(), outlineLines);
            return;
        }
        int thickness = Math.max(3, Math.min(18, zone.radius()));
        int step = Math.max(5, stride);
        Bounds clip = zone.clipBounds();
        for (int i = 1; i < points.size(); i++) {
            ScreenPoint previous = points.get(i - 1);
            ScreenPoint current = points.get(i);
            for (int offset = -thickness; offset <= thickness; offset += step) {
                addClippedLine(fillLines, previous.x() + offset, previous.y(),
                        current.x() + offset, current.y(), clip);
                addClippedLine(fillLines, previous.x(), previous.y() + offset,
                        current.x(), current.y() + offset, clip);
            }
            addClippedLine(outlineLines, previous.x(), previous.y(), current.x(), current.y(), clip);
        }
    }

    private static void buildZonePatternPlan(ZonePrimitive zone, int step, List<LinePrimitive> lines,
            List<FillSpan> dots) {
        Bounds bounds = zone.bounds();
        Bounds clip = zone.clipBounds();
        Bounds visible = intersect(bounds, clip);
        if (visible == null) {
            return;
        }
        int span = Math.max(bounds.maxX() - bounds.minX(), bounds.maxY() - bounds.minY());
        switch (zone.zone().pattern()) {
            case HAZARD_STRIPES -> {
                int start = alignedStart(visible.minX() - span, bounds.minX() - span, step);
                int end = visible.maxX() + visible.height() + step;
                for (int x = start; x < end; x += step) {
                    addClippedLine(lines, x, bounds.maxY(), x + span, bounds.minY(), clip);
                }
            }
            case SCAN_GRID -> {
                for (int x = alignedStart(visible.minX(), bounds.minX(), step);
                        x <= visible.maxX(); x += step) {
                    addClippedLine(lines, x, bounds.minY(), x, bounds.maxY(), clip);
                }
                for (int y = alignedStart(visible.minY(), bounds.minY(), step);
                        y <= visible.maxY(); y += step) {
                    addClippedLine(lines, bounds.minX(), y, bounds.maxX(), y, clip);
                }
            }
            case ANOMALY_NOISE -> {
                for (int i = 0; i < 12; i++) {
                    int x = visible.minX() + Math.floorMod(zone.zone().id().hashCode() + i * 37,
                            Math.max(1, visible.width()));
                    int y = visible.minY() + Math.floorMod(zone.zone().id().hashCode() + i * 53,
                            Math.max(1, visible.height()));
                    dots.add(new FillSpan(Math.max(clip.minX(), x - 1), Math.max(clip.minY(), y - 1),
                            Math.min(clip.maxX(), x + 2), Math.min(clip.maxY(), y + 2)));
                }
            }
            case ROUTE_BANDS -> {
                for (int y = alignedStart(visible.minY(), bounds.minY(), step);
                        y <= visible.maxY(); y += step) {
                    addClippedLine(lines, bounds.minX(), y, bounds.maxX(), y, clip);
                }
            }
            case SOLID -> {
            }
        }
    }

    private static Bounds intersect(Bounds a, Bounds b) {
        int minX = Math.max(a.minX(), b.minX());
        int minY = Math.max(a.minY(), b.minY());
        int maxX = Math.min(a.maxX(), b.maxX());
        int maxY = Math.min(a.maxY(), b.maxY());
        return maxX <= minX || maxY <= minY ? null : new Bounds(minX, minY, maxX, maxY);
    }

    private static int alignedStart(int minimum, int origin, int step) {
        int safeStep = Math.max(1, step);
        int offset = Math.floorMod(minimum - origin, safeStep);
        return offset == 0 ? minimum : minimum + safeStep - offset;
    }

    private static void addClippedLine(List<LinePrimitive> lines, int x0, int y0, int x1, int y1, Bounds clip) {
        LinePrimitive clipped = clippedLine(x0, y0, x1, y1, clip);
        if (clipped != null) {
            lines.add(clipped);
        }
    }

    private static LinePrimitive clippedLine(int x0, int y0, int x1, int y1, Bounds clip) {
        if (clip == null) {
            return new LinePrimitive(x0, y0, x1, y1);
        }
        double ax = x0;
        double ay = y0;
        double bx = x1;
        double by = y1;
        int codeA = clipCode(ax, ay, clip);
        int codeB = clipCode(bx, by, clip);
        while (true) {
            if ((codeA | codeB) == 0) {
                return new LinePrimitive((int) Math.round(ax), (int) Math.round(ay),
                        (int) Math.round(bx), (int) Math.round(by));
            }
            if ((codeA & codeB) != 0) {
                return null;
            }
            int code = codeA != 0 ? codeA : codeB;
            double x;
            double y;
            if ((code & 8) != 0) {
                if (Math.abs(by - ay) < 0.0001D) {
                    return null;
                }
                x = ax + (bx - ax) * (clip.maxY() - ay) / (by - ay);
                y = clip.maxY();
            } else if ((code & 4) != 0) {
                if (Math.abs(by - ay) < 0.0001D) {
                    return null;
                }
                x = ax + (bx - ax) * (clip.minY() - ay) / (by - ay);
                y = clip.minY();
            } else if ((code & 2) != 0) {
                if (Math.abs(bx - ax) < 0.0001D) {
                    return null;
                }
                y = ay + (by - ay) * (clip.maxX() - ax) / (bx - ax);
                x = clip.maxX();
            } else {
                if (Math.abs(bx - ax) < 0.0001D) {
                    return null;
                }
                y = ay + (by - ay) * (clip.minX() - ax) / (bx - ax);
                x = clip.minX();
            }
            if (code == codeA) {
                ax = x;
                ay = y;
                codeA = clipCode(ax, ay, clip);
            } else {
                bx = x;
                by = y;
                codeB = clipCode(bx, by, clip);
            }
        }
    }

    private static int clipCode(double x, double y, Bounds clip) {
        int code = 0;
        if (x < clip.minX()) {
            code |= 1;
        } else if (x > clip.maxX()) {
            code |= 2;
        }
        if (y < clip.minY()) {
            code |= 4;
        } else if (y > clip.maxY()) {
            code |= 8;
        }
        return code;
    }

    private static boolean drawPlanWithinBounds(ZoneDrawPlan plan, Bounds bounds) {
        for (FillSpan span : plan.fillSpans()) {
            if (!spanWithinBounds(span, bounds)) {
                return false;
            }
        }
        for (FillSpan dot : plan.patternDots()) {
            if (!spanWithinBounds(dot, bounds)) {
                return false;
            }
        }
        for (LinePrimitive line : plan.fillLines()) {
            if (!lineWithinBounds(line, bounds)) {
                return false;
            }
        }
        for (LinePrimitive line : plan.patternLines()) {
            if (!lineWithinBounds(line, bounds)) {
                return false;
            }
        }
        for (LinePrimitive line : plan.contourLines()) {
            if (!lineWithinBounds(line, bounds)) {
                return false;
            }
        }
        for (LinePrimitive line : plan.outlineLines()) {
            if (!lineWithinBounds(line, bounds)) {
                return false;
            }
        }
        return true;
    }

    private static boolean spanWithinBounds(FillSpan span, Bounds bounds) {
        return span.x0() >= bounds.minX() && span.y0() >= bounds.minY()
                && span.x1() <= bounds.maxX() && span.y1() <= bounds.maxY();
    }

    private static boolean lineWithinBounds(LinePrimitive line, Bounds bounds) {
        return pointWithinBounds(line.x0(), line.y0(), bounds)
                && pointWithinBounds(line.x1(), line.y1(), bounds);
    }

    private static boolean pointWithinBounds(int x, int y, Bounds bounds) {
        return x >= bounds.minX() && x <= bounds.maxX()
                && y >= bounds.minY() && y <= bounds.maxY();
    }

    private static boolean drawZoneFillBand(HoloMapZonePattern pattern, int x, int y, int width, int stride) {
        if (width <= 0 || stride <= 0) {
            return false;
        }
        return pattern != HoloMapZonePattern.HAZARD_STRIPES || Math.floorMod(x + y, 18) < 12;
    }

    private static int zoneFillStride(int radius) {
        if (radius > 900) {
            return 34;
        }
        if (radius > 720) {
            return 28;
        }
        if (radius > 480) {
            return 22;
        }
        if (radius > 360) {
            return 16;
        }
        if (radius > 180) {
            return 10;
        }
        if (radius > 80) {
            return 5;
        }
        return 1;
    }

    private static ZoneVisualStyle tacticalZoneStyle(ZonePrimitive zone, boolean hovered) {
        int span = zoneSpan(zone);
        int fillAlpha = tacticalZoneFillAlpha(zone.fillColor(), span, hovered);
        int outlineAlpha = tacticalZoneOutlineAlpha(zone.outlineColor(), span, hovered);
        int patternAlpha = hovered ? Math.min(0x54, outlineAlpha) : Math.min(0x2E, Math.max(0x10, outlineAlpha / 3));
        int step = zonePatternStep(zone);
        int contourAlpha = Math.max(0x0A, Math.min(0x24, outlineAlpha / 4));
        return new ZoneVisualStyle(withAlpha(zone.fillColor(), fillAlpha), withAlpha(zone.outlineColor(), outlineAlpha),
                withAlpha(zone.outlineColor(), patternAlpha), withAlpha(zone.outlineColor(), contourAlpha),
                zoneFillStride(Math.max(zone.radius(), span / 2)), step);
    }

    private static int zonePatternStep(ZonePrimitive zone) {
        int span = zoneSpan(zone);
        return switch (zone.zone().pattern()) {
            case HAZARD_STRIPES -> Math.max(36, Math.min(128, span / 5));
            case SCAN_GRID, ROUTE_BANDS -> Math.max(30, Math.min(112, span / 6));
            case ANOMALY_NOISE -> Math.max(18, Math.min(64, span / 9));
            case SOLID -> Math.max(24, Math.min(72, span / 8));
        };
    }

    private static int tacticalZoneFillAlpha(int color, int span, boolean hovered) {
        int base = alpha(color, 0x33);
        int cap;
        if (span > 720) {
            cap = 0x0A;
        } else if (span > 420) {
            cap = 0x0F;
        } else if (span > 220) {
            cap = 0x18;
        } else if (span > 120) {
            cap = 0x22;
        } else {
            cap = 0x32;
        }
        int alpha = Math.min(base, cap);
        return hovered ? Math.min(0x48, Math.max(alpha + 0x12, Math.min(base + 0x0C, 0x48))) : alpha;
    }

    private static int tacticalZoneOutlineAlpha(int color, int span, boolean hovered) {
        int base = alpha(color, 0xAA);
        int cap;
        if (span > 720) {
            cap = 0x54;
        } else if (span > 420) {
            cap = 0x66;
        } else if (span > 220) {
            cap = 0x7C;
        } else {
            cap = 0x98;
        }
        int alpha = Math.min(base, cap);
        return hovered ? Math.min(0xC0, Math.max(alpha + 0x20, 0x96)) : alpha;
    }

    private static int alpha(int color, int fallback) {
        int alpha = (color >>> 24) & 0xFF;
        return alpha == 0 ? fallback : alpha;
    }

    private static int zoneSpan(ZonePrimitive zone) {
        return Math.max(Math.max(zone.radius() * 2, zone.halfWidth() * 2),
                Math.max(zone.halfHeight() * 2,
                        Math.max(zone.bounds().maxX() - zone.bounds().minX(),
                                zone.bounds().maxY() - zone.bounds().minY())));
    }

    private static ZonePrimitive hoveredZone(HoloMapViewState state, List<ZonePrimitive> zones) {
        ZonePrimitive best = null;
        double bestScore = Double.MAX_VALUE;
        for (ZonePrimitive zone : zones) {
            if (!zoneContains(zone, state.mouseX(), state.mouseY())) {
                continue;
            }
            double score = zoneHoverScore(zone, state.mouseX(), state.mouseY());
            if (best == null || score < bestScore
                    || score == bestScore && zone.priority() > best.priority()) {
                best = zone;
                bestScore = score;
            }
        }
        return best;
    }

    private static boolean zoneContains(ZonePrimitive zone, double mouseX, double mouseY) {
        if (!inside(mouseX, mouseY, zone.bounds().minX(), zone.bounds().minY(),
                Math.max(1, zone.bounds().maxX() - zone.bounds().minX()),
                Math.max(1, zone.bounds().maxY() - zone.bounds().minY()))) {
            return false;
        }
        return switch (zone.zone().shape()) {
            case CIRCLE -> {
                double dx = mouseX - zone.x();
                double dy = mouseY - zone.y();
                yield dx * dx + dy * dy <= zone.radius() * (double) zone.radius();
            }
            case RECT -> Math.abs(mouseX - zone.x()) <= zone.halfWidth()
                    && Math.abs(mouseY - zone.y()) <= zone.halfHeight();
            case POLYGON -> pointInPolygon(zone.points(), mouseX, mouseY);
            case CORRIDOR -> distanceToPolyline(zone.points(), mouseX, mouseY)
                    <= Math.max(5.0D, Math.min(22.0D, zone.radius() + 4.0D));
        };
    }

    private static double zoneHoverScore(ZonePrimitive zone, double mouseX, double mouseY) {
        double distance = switch (zone.zone().shape()) {
            case CORRIDOR -> distanceToPolyline(zone.points(), mouseX, mouseY);
            case POLYGON, RECT, CIRCLE -> {
                double dx = mouseX - zone.x();
                double dy = mouseY - zone.y();
                yield Math.sqrt(dx * dx + dy * dy);
            }
        };
        return distance + zoneSpan(zone) * 0.15D - zone.priority() * 0.5D;
    }

    private static boolean pointInPolygon(List<ScreenPoint> points, double x, double y) {
        if (points.size() < 3) {
            return false;
        }
        boolean inside = false;
        for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            ScreenPoint pi = points.get(i);
            ScreenPoint pj = points.get(j);
            double denominator = pj.y() - pi.y();
            if ((pi.y() > y) != (pj.y() > y)
                    && Math.abs(denominator) > 0.0001D
                    && x < (pj.x() - pi.x()) * (y - pi.y()) / denominator + pi.x()) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static double distanceToPolyline(List<ScreenPoint> points, double x, double y) {
        if (points.size() < 2) {
            return Double.MAX_VALUE;
        }
        double best = Double.MAX_VALUE;
        for (int i = 1; i < points.size(); i++) {
            ScreenPoint a = points.get(i - 1);
            ScreenPoint b = points.get(i);
            best = Math.min(best, distanceToSegment(a.x(), a.y(), b.x(), b.y(), x, y));
        }
        return best;
    }

    private static double distanceToSegment(double x0, double y0, double x1, double y1, double x, double y) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= 0.0001D) {
            double px = x - x0;
            double py = y - y0;
            return Math.sqrt(px * px + py * py);
        }
        double t = Math.max(0.0D, Math.min(1.0D, ((x - x0) * dx + (y - y0) * dy) / lengthSquared));
        double projectionX = x0 + t * dx;
        double projectionY = y0 + t * dy;
        double px = x - projectionX;
        double py = y - projectionY;
        return Math.sqrt(px * px + py * py);
    }

    private static int edgeIndicatorLimit(RenderBudget budget) {
        int markerBudget = Math.max(0, budget.maxMarkers());
        if (markerBudget <= 96) {
            return 2;
        }
        if (markerBudget <= 192) {
            return 3;
        }
        return 4;
    }

    private static boolean claimEdgeIndicator(HoloMapViewState state, int x, int y, int[] sectors, int limit) {
        if (sectors == null || sectors.length == 0 || limit <= 0) {
            return false;
        }
        int centerX = state.x() + state.width() / 2;
        int centerY = state.y() + state.height() / 2;
        double angle = Math.atan2(y - centerY, x - centerX);
        int sector = Math.floorMod((int) Math.floor((angle + Math.PI) / (Math.PI * 2.0D) * sectors.length),
                sectors.length);
        if (sectors[sector] >= limit) {
            return false;
        }
        sectors[sector]++;
        return true;
    }

    private static void drawTerrain(GuiGraphicsExtractor graphics, HoloMapViewState state, TerrainPrimitive terrain) {
        int x = state.worldToScreenX(terrain.tile().chunkX() * 16.0D);
        int y = state.worldToScreenZ(terrain.tile().chunkZ() * 16.0D);
        if (!terrain.highDetail() || terrain.chunkSize() <= 18) {
            graphics.fill(x, y, x + terrain.chunkSize(), y + terrain.chunkSize(),
                    terrain.averageColor());
            return;
        }
        for (int localZ = 0; localZ < HoloMapTerrainTile.SIZE; localZ++) {
            for (int localX = 0; localX < HoloMapTerrainTile.SIZE; localX++) {
                int px = x + (int) Math.round(localX * terrain.pixelSize());
                int py = y + (int) Math.round(localZ * terrain.pixelSize());
                graphics.fill(px, py, px + terrain.pixelSize(), py + terrain.pixelSize(),
                        terrain.styled().pixel(localX, localZ));
            }
        }
    }

    private static List<LinePrimitive> buildWorldGrid(HoloMapViewState state) {
        int step = 16;
        while (step * state.zoom() < 12.0D && step < 256) {
            step *= 2;
        }
        if (step * state.zoom() < 10.0D) {
            return List.of();
        }
        ArrayList<LinePrimitive> lines = new ArrayList<>();
        double left = state.screenToWorldX(state.x() + 4);
        double right = state.screenToWorldX(state.x() + state.width() - 4);
        double top = state.screenToWorldZ(state.y() + 4);
        double bottom = state.screenToWorldZ(state.y() + state.height() - 4);
        int startX = (int) Math.floor(left / step) * step;
        for (int worldX = startX; worldX <= right; worldX += step) {
            int sx = state.worldToScreenX(worldX);
            lines.add(new LinePrimitive(sx, state.y() + 4, sx, state.y() + state.height() - 4,
                    worldX == 0 ? 0x6638DFF4 : 0x2438DFF4));
        }
        int startZ = (int) Math.floor(top / step) * step;
        for (int worldZ = startZ; worldZ <= bottom; worldZ += step) {
            int sy = state.worldToScreenZ(worldZ);
            lines.add(new LinePrimitive(state.x() + 4, sy, state.x() + state.width() - 4, sy,
                    worldZ == 0 ? 0x6638DFF4 : 0x2438DFF4));
        }
        return List.copyOf(lines);
    }

    private static List<LinePrimitive> buildFallbackGrid(HoloMapViewState state) {
        ArrayList<LinePrimitive> lines = new ArrayList<>();
        for (int gx = state.x() + 18; gx < state.x() + state.width() - 10; gx += 24) {
            lines.add(new LinePrimitive(gx, state.y() + 8, gx, state.y() + state.height() - 8, 0x2438DFF4));
        }
        for (int gy = state.y() + 18; gy < state.y() + state.height() - 10; gy += 24) {
            lines.add(new LinePrimitive(state.x() + 8, gy, state.x() + state.width() - 8, gy, 0x2438DFF4));
        }
        return List.copyOf(lines);
    }

    private static void drawLines(GuiGraphicsExtractor graphics, List<LinePrimitive> lines, int fallbackColor) {
        for (LinePrimitive line : lines) {
            HoloMapGlyphRenderer.drawLine(graphics, line.x0(), line.y0(), line.x1(), line.y1(),
                    line.color() == 0 ? fallbackColor : line.color());
        }
    }

    private static List<LinePrimitive> ringLines(int cx, int cy, int radius) {
        return ringLines(cx, cy, radius, null);
    }

    private static List<LinePrimitive> ringLines(int cx, int cy, int radius, Bounds clip) {
        ArrayList<LinePrimitive> lines = new ArrayList<>();
        int safeRadius = Math.max(1, radius);
        int segments = safeRadius > 88 ? 24 : 16;
        int previousX = cx + safeRadius;
        int previousY = cy;
        for (int i = 1; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;
            int px = cx + (int) Math.round(Math.cos(angle) * safeRadius);
            int py = cy + (int) Math.round(Math.sin(angle) * safeRadius);
            addClippedLine(lines, previousX, previousY, px, py, clip);
            previousX = px;
            previousY = py;
        }
        return List.copyOf(lines);
    }

    private static List<LinePrimitive> rectLines(int left, int top, int right, int bottom) {
        return rectLines(left, top, right, bottom, null);
    }

    private static List<LinePrimitive> rectLines(int left, int top, int right, int bottom, Bounds clip) {
        ArrayList<LinePrimitive> lines = new ArrayList<>(4);
        addClippedLine(lines, left, top, right, top, clip);
        addClippedLine(lines, right, top, right, bottom, clip);
        addClippedLine(lines, right, bottom, left, bottom, clip);
        addClippedLine(lines, left, bottom, left, top, clip);
        return List.copyOf(lines);
    }

    private static void drawRing(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        int segments = radius > 88 ? 24 : 16;
        int previousX = cx + radius;
        int previousY = cy;
        for (int i = 1; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;
            int px = cx + (int) Math.round(Math.cos(angle) * radius);
            int py = cy + (int) Math.round(Math.sin(angle) * radius);
            HoloMapGlyphRenderer.drawLine(graphics, previousX, previousY, px, py, color);
            previousX = px;
            previousY = py;
        }
    }

    private static void drawPlayer(GuiGraphicsExtractor graphics, HoloMapViewState state) {
        int px = state.worldToScreenX(state.playerX());
        int py = state.worldToScreenZ(state.playerZ());
        HoloMapGlyphRenderer.drawPlayer(graphics, px, py, state.playerYaw(), 5);
    }

    private static int visibleMarkerCount(HoloMapViewState state, List<HoloMapSnapshotPacket.MarkerData> markers) {
        int count = 0;
        for (HoloMapSnapshotPacket.MarkerData marker : markers) {
            if (state.dimension().equals(marker.dimension())
                    && HoloMapVisibility.visibleInNormalView(marker.state())) {
                count++;
            }
        }
        return count;
    }

    private static int visibleWaypointCount(HoloMapViewState state, List<HoloMapWaypoint> waypoints) {
        int count = 0;
        for (HoloMapWaypoint waypoint : waypoints) {
            if (waypoint.visible() && waypoint.inDimension(state.dimension())) {
                count++;
            }
        }
        return count;
    }

    private static int visibleOverlayCount(HoloMapViewState state, List<HoloMapSnapshotPacket.OverlayData> overlays) {
        int count = 0;
        for (HoloMapSnapshotPacket.OverlayData overlay : overlays) {
            if (state.dimension().equals(overlay.dimension())
                    && HoloMapVisibility.visibleInNormalView(overlay.state())
                    && state.fieldMode() != HoloMapVisibility.FieldMode.OFF
                    && shouldBuildOverlayField(state, overlay)) {
                count++;
            }
        }
        return count;
    }

    private static int visibleZoneCount(HoloMapViewState state, List<HoloMapSnapshotPacket.ZoneData> zones) {
        int count = 0;
        for (HoloMapSnapshotPacket.ZoneData zone : zones) {
            ZonePrimitive primitive = zonePrimitive(state, zone);
            if (primitive != null && state.fieldMode() != HoloMapVisibility.FieldMode.OFF
                    && shouldBuildField(state, primitive)) {
                count++;
            }
        }
        return count;
    }

    private static int visibleRouteSegmentCount(HoloMapViewState state, List<HoloMapSnapshotPacket.RouteData> routes) {
        int count = 0;
        for (HoloMapSnapshotPacket.RouteData route : routes) {
            if (!HoloMapVisibility.visibleInNormalView(route.state())) {
                continue;
            }
            int points = 0;
            for (HoloMapSnapshotPacket.RoutePointData point : route.points()) {
                if (state.dimension().equals(point.dimension())) {
                    points++;
                }
            }
            count += Math.max(0, points - 1);
        }
        return count;
    }

    private static int markerHash(List<HoloMapSnapshotPacket.MarkerData> markers) {
        int hash = 1;
        for (HoloMapSnapshotPacket.MarkerData marker : markers) {
            hash = 31 * hash + marker.id().hashCode();
            hash = 31 * hash + marker.state().hashCode();
            hash = 31 * hash + marker.routeOrder();
        }
        return hash;
    }

    private static int waypointHash(List<HoloMapWaypoint> waypoints) {
        int hash = 1;
        for (HoloMapWaypoint waypoint : waypoints) {
            hash = 31 * hash + waypoint.id().hashCode();
            hash = 31 * hash + Boolean.hashCode(waypoint.visible());
            hash = 31 * hash + waypoint.scope().hashCode();
        }
        return hash;
    }

    private static int pointHash(List<ScreenPoint> points) {
        int hash = 1;
        for (ScreenPoint point : points) {
            hash = 31 * hash + point.x();
            hash = 31 * hash + point.y();
        }
        return hash;
    }

    private static int clampToX(HoloMapViewState state, int x) {
        return Math.max(state.x() + 4, Math.min(state.x() + state.width() - 4, x));
    }

    private static int clampToY(HoloMapViewState state, int y) {
        return Math.max(state.y() + 4, Math.min(state.y() + state.height() - 4, y));
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static double distance(double x0, double z0, double x1, double z1) {
        return Math.sqrt(distanceSquared(x0, z0, x1, z1));
    }

    private static double distanceSquared(double x0, double z0, double x1, double z1) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        return dx * dx + dz * dz;
    }

    private static <T> List<T> boundedTopN(List<T> values, int limit, Comparator<T> bestFirst) {
        if (limit <= 0 || values.isEmpty()) {
            return List.of();
        }
        if (values.size() <= limit) {
            ArrayList<T> sorted = new ArrayList<>(values);
            sorted.sort(bestFirst);
            return List.copyOf(sorted);
        }
        PriorityQueue<T> retained = new PriorityQueue<>(limit, bestFirst.reversed());
        for (T value : values) {
            if (retained.size() < limit) {
                retained.add(value);
            } else if (bestFirst.compare(value, retained.peek()) < 0) {
                retained.poll();
                retained.add(value);
            }
        }
        ArrayList<T> sorted = new ArrayList<>(retained);
        sorted.sort(bestFirst);
        return List.copyOf(sorted);
    }

    private static int withAlpha(int color, int alpha) {
        return ((Math.max(0, Math.min(255, alpha)) & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private static String trim(Font font, String text, int maxWidth) {
        if (font == null || text == null || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String value = text;
        String ellipsis = "...";
        int allowed = Math.max(1, maxWidth - font.width(ellipsis));
        while (!value.isEmpty() && font.width(value) > allowed) {
            value = value.substring(0, value.length() - 1);
        }
        return value + ellipsis;
    }

    private static final class LabelPlacer {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final ArrayList<LabelRect> labels = new ArrayList<>();

        private LabelPlacer(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
        }

        private boolean draw(GuiGraphicsExtractor graphics, Font font, String text, int labelX, int labelY,
                int maxWidth, int color, boolean force) {
            String label = trim(font, text, maxWidth);
            if (label.isBlank()) {
                return false;
            }
            int labelW = Math.max(1, font.width(label));
            int drawX = Math.max(x, Math.min(labelX, x + width - labelW - 4));
            int drawY = Math.max(y, Math.min(labelY, y + height - 10));
            if (!claim(drawX - 3, drawY - 2, labelW + 6, 12, force)) {
                return false;
            }
            graphics.fill(drawX - 3, drawY - 2, drawX + labelW + 3, drawY + 10,
                    force ? 0xB0061014 : 0x88061014);
            int outline = withAlpha(color, force ? 0xAA : 0x66);
            graphics.outline(drawX - 3, drawY - 2, labelW + 6, 12, outline);
            graphics.fill(drawX - 3, drawY - 2, drawX - 2, drawY + 10, outline);
            graphics.text(font, label, drawX, drawY, color, false);
            return true;
        }

        private boolean claim(int rectX, int rectY, int rectW, int rectH, boolean force) {
            LabelRect rect = new LabelRect(rectX, rectY, Math.max(1, rectW), Math.max(1, rectH));
            if (!force) {
                for (LabelRect existing : labels) {
                    if (existing.intersects(rect)) {
                        return false;
                    }
                }
            }
            labels.add(rect);
            return true;
        }
    }

    public record RenderBudget(int maxTerrainTiles, int maxMarkers, int maxWaypoints, int maxRouteSegments,
            int maxOverlays, int maxZones, int labelLimit, boolean drawPlayer, boolean edgeIndicators) {
        public RenderBudget(int maxTerrainTiles, int maxMarkers, int maxWaypoints, int maxRouteSegments,
                int maxOverlays, int labelLimit, boolean drawPlayer, boolean edgeIndicators) {
            this(maxTerrainTiles, maxMarkers, maxWaypoints, maxRouteSegments, maxOverlays, maxOverlays,
                    labelLimit, drawPlayer, edgeIndicators);
        }

        private RenderBudget forDetail(Config.MapRenderDetail detail) {
            return switch (detail == null ? Config.MapRenderDetail.BALANCED : detail) {
                case PERFORMANCE -> new RenderBudget(
                        Math.max(48, maxTerrainTiles / 2),
                        Math.max(64, maxMarkers / 2),
                        Math.max(64, maxWaypoints / 2),
                        Math.max(128, maxRouteSegments / 2),
                        Math.max(16, maxOverlays / 2),
                        Math.max(16, maxZones / 2),
                        Math.max(4, labelLimit / 2),
                        drawPlayer,
                        edgeIndicators);
                case QUALITY -> new RenderBudget(
                        Math.min(1024, maxTerrainTiles + maxTerrainTiles / 2),
                        Math.min(512, maxMarkers + maxMarkers / 2),
                        Math.min(512, maxWaypoints + maxWaypoints / 2),
                        Math.min(1024, maxRouteSegments + maxRouteSegments / 2),
                        Math.min(128, maxOverlays + maxOverlays / 2),
                        Math.min(128, maxZones + maxZones / 2),
                        Math.min(48, labelLimit + labelLimit / 2),
                        drawPlayer,
                        edgeIndicators);
                case BALANCED -> this;
            };
        }
    }

    public record RenderResult(int terrainTiles, int culledTerrainTiles, int culledOverlays, int culledZones,
            int culledRouteSegments,
            int culledMarkers, int culledWaypoints, List<MarkerHit> markerHits, List<WaypointHit> waypointHits,
            boolean highDetailTerrain) {
        public RenderResult(int terrainTiles, int culledTerrainTiles, int culledOverlays, int culledRouteSegments,
                int culledMarkers, int culledWaypoints, List<MarkerHit> markerHits, List<WaypointHit> waypointHits,
                boolean highDetailTerrain) {
            this(terrainTiles, culledTerrainTiles, culledOverlays, 0, culledRouteSegments,
                    culledMarkers, culledWaypoints, markerHits, waypointHits, highDetailTerrain);
        }

        public int totalCulled() {
            return culledTerrainTiles + culledOverlays + culledZones + culledRouteSegments
                    + culledMarkers + culledWaypoints;
        }

        public int syncedTerrainTiles() {
            return Math.max(0, terrainTiles + culledTerrainTiles);
        }
    }

    public record MarkerHit(HoloMapSnapshotPacket.MarkerData marker, int x, int y) {
        private static MarkerHit from(MarkerPrimitive primitive) {
            return new MarkerHit(primitive.marker(), primitive.x(), primitive.y());
        }
    }

    public record WaypointHit(HoloMapWaypoint waypoint, int x, int y) {
        private static WaypointHit from(WaypointPrimitive primitive) {
            return new WaypointHit(primitive.waypoint(), primitive.x(), primitive.y());
        }
    }

    private record RenderCacheKey(String dimension, long terrainRevision, long snapshotGameTime, long waypointRevision,
            int viewportBucket, int zoomBucket, boolean showMarkers, HoloMapVisibility.FieldMode fieldMode,
            boolean showWaypoints, String selectedMarkerId, String selectedWaypointId, int markerHash,
            int waypointHash, RenderBudget budget) {
        private static RenderCacheKey from(HoloMapViewState state, HoloMapSnapshotPacket snapshot,
                List<HoloMapSnapshotPacket.MarkerData> markers, List<HoloMapWaypoint> waypoints, RenderBudget budget) {
            return new RenderCacheKey(state.dimension(), HoloMapTerrainClientState.revision(), snapshot.gameTime(),
                    HoloMapWaypointClientState.revision(), state.viewportBucket(), state.zoomBucket(),
                    state.showMarkers(), state.fieldMode(), state.showWaypoints(), state.selectedMarkerId(),
                    state.selectedWaypointId(), HoloMapRenderer.markerHash(markers),
                    HoloMapRenderer.waypointHash(waypoints), budget);
        }
    }

    private record TerrainCacheKey(String dimension, long terrainRevision, int minChunkX, int maxChunkX,
            int minChunkZ, int maxChunkZ, int zoomBucket, int maxTerrainTiles, int visibleTileCount,
            boolean highDetail) {
        private static TerrainCacheKey from(HoloMapViewState state, long terrainRevision,
                RenderBudget budget, int visibleTileCount) {
            return from(state, terrainRevision, budget, visibleTileCount, false);
        }

        private static TerrainCacheKey from(HoloMapViewState state, long terrainRevision,
                RenderBudget budget, int visibleTileCount, boolean highDetail) {
            RenderBudget safeBudget = budget == null ? FULLSCREEN_BUDGET : budget;
            return new TerrainCacheKey(state.dimension(), terrainRevision, state.minChunkX(), state.maxChunkX(),
                    state.minChunkZ(), state.maxChunkZ(), state.zoomBucket(),
                    safeBudget.maxTerrainTiles(), Math.max(0, visibleTileCount), highDetail);
        }
    }

    private record TerrainBuild(List<TerrainPrimitive> primitives, int culledTiles, int syncedTiles,
            boolean highDetail) {
        private static final TerrainBuild EMPTY = new TerrainBuild(List.of(), 0, 0, false);
    }

    private record HoloMapRenderModel(List<TerrainPrimitive> terrain, List<ZonePrimitive> zones,
            List<OverlayPrimitive> overlays, List<RouteSegmentPrimitive> routes,
            List<MarkerPrimitive> markers, List<WaypointPrimitive> waypoints,
            List<LinePrimitive> grid, List<LinePrimitive> fallbackGrid,
            RenderResult result) {
        private static final HoloMapRenderModel EMPTY = new HoloMapRenderModel(List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                new RenderResult(0, 0, 0, 0, 0, 0, List.of(), List.of(), false));
    }

    private record TerrainPrimitive(HoloMapTerrainTile tile, int chunkSize, int pixelSize,
            int averageColor, HoloMapTerrainRenderCache.StyledTile styled,
            boolean highDetail) {
    }

    private record ZonePrimitive(HoloMapSnapshotPacket.ZoneData zone, int x, int y, int radius,
            int halfWidth, int halfHeight, int fillColor, int outlineColor, List<ScreenPoint> points,
            Bounds bounds, Bounds clipBounds, double distance, int priority, ZoneDrawPlan drawPlan) {
    }

    private record ZoneDrawPlanKey(Identifier id, HoloMapZoneShape shape, HoloMapZonePattern pattern,
            boolean full, int x, int y, int radius, int halfWidth, int halfHeight, int fillColor,
            int outlineColor, int minX, int minY, int maxX, int maxY, int clipMinX, int clipMinY,
            int clipMaxX, int clipMaxY, int pointHash) {
        private static ZoneDrawPlanKey from(ZonePrimitive zone, boolean full) {
            Bounds bounds = zone.bounds();
            Bounds clip = zone.clipBounds();
            return new ZoneDrawPlanKey(zone.zone().id(), zone.zone().shape(), zone.zone().pattern(), full,
                    zone.x(), zone.y(), zone.radius(), zone.halfWidth(), zone.halfHeight(),
                    zone.fillColor(), zone.outlineColor(), bounds.minX(), bounds.minY(), bounds.maxX(),
                    bounds.maxY(), clip.minX(), clip.minY(), clip.maxX(), clip.maxY(),
                    HoloMapRenderer.pointHash(zone.points()));
        }
    }

    private record ZoneVisualStyle(int fillColor, int outlineColor, int patternColor, int contourColor,
            int fillStride, int patternStep) {
    }

    private record ZoneDrawPlan(List<FillSpan> fillSpans, List<LinePrimitive> fillLines,
            List<LinePrimitive> patternLines, List<FillSpan> patternDots, List<LinePrimitive> contourLines,
            List<LinePrimitive> outlineLines) {
        private static final ZoneDrawPlan EMPTY = new ZoneDrawPlan(List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of());
    }

    private record FillSpan(int x0, int y0, int x1, int y1) {
    }

    private record LinePrimitive(int x0, int y0, int x1, int y1, int color) {
        private LinePrimitive(int x0, int y0, int x1, int y1) {
            this(x0, y0, x1, y1, 0);
        }
    }

    private record OverlayPrimitive(int x, int y, int radius, int color, List<LinePrimitive> ring, boolean estimated,
            double distance, int priority) {
    }

    private record RouteSegmentPrimitive(int x0, int y0, int x1, int y1, int color, double distance) {
    }

    private record MarkerPrimitive(HoloMapSnapshotPacket.MarkerData marker, int x, int y, int edgeX, int edgeY,
            boolean onscreen, boolean selected, int color, int priority, double distance) {
    }

    private record WaypointPrimitive(HoloMapWaypoint waypoint, int x, int y, int edgeX, int edgeY,
            boolean onscreen, boolean selected, int color, double distance, boolean deathpoint) {
    }

    private record ScreenPoint(int x, int y) {
    }

    private record Bounds(int minX, int minY, int maxX, int maxY) {
        private int width() {
            return Math.max(0, maxX - minX);
        }

        private int height() {
            return Math.max(0, maxY - minY);
        }
    }

    private record WorldBounds(double minX, double minZ, double maxX, double maxZ) {
        private boolean contains(double x, double z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        private boolean intersects(WorldBounds other) {
            return other != null
                    && minX <= other.maxX()
                    && maxX >= other.minX()
                    && minZ <= other.maxZ()
                    && maxZ >= other.minZ();
        }

        private boolean intersectsCircle(double x, double z, double radius) {
            double safeRadius = Math.max(0.0D, radius);
            double nearestX = Math.max(minX, Math.min(maxX, x));
            double nearestZ = Math.max(minZ, Math.min(maxZ, z));
            return distanceSquared(x, z, nearestX, nearestZ) <= safeRadius * safeRadius;
        }

        private boolean intersectsSegment(double x0, double z0, double x1, double z1, double padding) {
            double safePadding = Math.max(0.0D, padding);
            return minX - safePadding <= Math.max(x0, x1)
                    && maxX + safePadding >= Math.min(x0, x1)
                    && minZ - safePadding <= Math.max(z0, z1)
                    && maxZ + safePadding >= Math.min(z0, z1);
        }
    }

    private record LabelRect(int x, int y, int w, int h) {
        private boolean intersects(LabelRect other) {
            return x < other.x() + other.w()
                    && x + w > other.x()
                    && y < other.y() + other.h()
                    && y + h > other.y();
        }
    }
}
