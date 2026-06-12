package com.knoxhack.echoholomap.client;

import com.echoplatform.echocore.api.IMapMarker;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.api.HoloMapPrecision;
import com.knoxhack.echoholomap.api.HoloMapZoneShape;
import com.knoxhack.echoholomap.map.HoloMapTerrainTile;
import com.knoxhack.echoholomap.map.HoloMapVisualPriority;
import com.knoxhack.echoholomap.map.HoloMapVisibility;
import com.knoxhack.echoholomap.network.HoloMapClientState;
import com.knoxhack.echoholomap.network.HoloMapSnapshotPacket;
import com.knoxhack.echoholomap.network.HoloMapTerrainClientState;
import com.knoxhack.echoholomap.network.HoloMapTileRequestPacket;
import com.knoxhack.echoholomap.network.HoloMapWaypointClientState;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class HoloMapMiniMapOverlay {
    private static boolean toggledVisible = true;
    private static long lastRequestTick = -200L;
    private static Config.MiniMapCorner cornerOverride;
    private static double zoomOffset;

    private HoloMapMiniMapOverlay() {
    }

    public static void toggle() {
        toggledVisible = !toggledVisible;
    }

    public static void zoomIn() {
        zoomOffset = Math.min(1.5D, zoomOffset + 0.25D);
    }

    public static void zoomOut() {
        zoomOffset = Math.max(-0.75D, zoomOffset - 0.25D);
    }

    public static void cycleCorner() {
        Config.MiniMapCorner current = corner();
        Config.MiniMapCorner[] values = Config.MiniMapCorner.values();
        cornerOverride = values[(current.ordinal() + 1) % values.length];
    }

    public static Map<String, Object> nativeOverlayState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("surface", "holomap");
        state.put("overlay", "minimap");
        state.put("visible", enabled());
        state.put("toggledVisible", toggledVisible);
        state.put("corner", corner().name());
        state.put("zoom", minimapZoom());
        state.put("zoomOffset", zoomOffset);
        state.put("size", minimapSize());
        state.put("markerLimit", markerLimit());
        state.put("discoveredTerrainChunks", HoloMapTerrainClientState.discoveredCount());
        state.put("waypoints", HoloMapWaypointClientState.waypoints().size());
        return Map.copyOf(state);
    }

    public static String nativeOverlayStatusLine() {
        Map<String, Object> state = nativeOverlayState();
        return "HoloMap minimap "
                + (Boolean.TRUE.equals(state.get("visible")) ? "visible" : "hidden")
                + " / corner " + state.get("corner")
                + " / zoom " + String.format(java.util.Locale.ROOT, "%.2f", state.get("zoom"))
                + " / chunks " + state.get("discoveredTerrainChunks")
                + " / waypoints " + state.get("waypoints");
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !enabled()) {
            return;
        }
        int size = minimapSize();
        int margin = 12;
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();
        int x = switch (corner()) {
            case TOP_LEFT, BOTTOM_LEFT -> margin;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - size - margin;
        };
        int y = switch (corner()) {
            case TOP_LEFT, TOP_RIGHT -> margin;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - size - margin;
        };
        requestNearbyTiles(player);
        HoloMapLocalWaypointStore.ensureLoaded();
        int accent = HoloMapVisualStyle.accent(player);
        int panel = HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.panel(player),
                Math.round(HoloMapVisualStyle.hologramOpacity(player) * 255.0F));
        graphics.fill(x - 4, y - 4, x + size + 4, y + size + 18, panel);
        graphics.outline(x - 4, y - 4, size + 8, size + 22, HoloMapVisualStyle.withAlpha(accent, 0xAA));
        graphics.fill(x - 3, y - 3, x + size + 3, y - 1, accent);
        graphics.enableScissor(x, y, x + size, y + size);
        drawTerrain(graphics, player, x, y, size, size, minimapZoom());
        drawMarkers(graphics, player, x, y, size, size, minimapZoom());
        drawWaypoints(graphics, player, x, y, size, size, minimapZoom());
        drawPlayer(graphics, player, x, y, size);
        graphics.disableScissor();
        drawReadout(graphics, minecraft.font, player, x, y, size);
        renderCoreFrame(graphics, x - 4, y - 4, size + 8, size + 22);
    }

    private static void requestNearbyTiles(Player player) {
        long now = player.level().getGameTime();
        if (now - lastRequestTick < 40L) {
            return;
        }
        lastRequestTick = now;
        EchoNetClientActions.sendServerboundAction(HoloMapTileRequestPacket.forPlayer(
                player, player.getX(), player.getZ(), Math.max(2, (int) Math.ceil(minimapSize() / 32.0D))));
    }

    private static void drawTerrain(GuiGraphicsExtractor graphics, Player player,
            int x, int y, int w, int h, double zoom) {
        String dimension = player.level().dimension().identifier().toString();
        double centerX = player.getX();
        double centerZ = player.getZ();
        int minChunkX = Math.floorDiv((int) Math.floor(centerX - w / (2.0D * zoom)), 16) - 1;
        int maxChunkX = Math.floorDiv((int) Math.floor(centerX + w / (2.0D * zoom)), 16) + 1;
        int minChunkZ = Math.floorDiv((int) Math.floor(centerZ - h / (2.0D * zoom)), 16) - 1;
        int maxChunkZ = Math.floorDiv((int) Math.floor(centerZ + h / (2.0D * zoom)), 16) + 1;
        List<HoloMapTerrainTile> tiles = HoloMapTerrainClientState.tiles(dimension,
                minChunkX, maxChunkX, minChunkZ, maxChunkZ);
        graphics.fill(x, y, x + w, y + h, 0xCC061014);
        if (tiles.isEmpty()) {
            drawGrid(graphics, x, y, w, h);
            return;
        }
        for (HoloMapTerrainTile tile : tiles) {
            drawTile(graphics, tile, centerX, centerZ, zoom, x, y, w, h, tiles.size());
        }
        drawGrid(graphics, x, y, w, h);
    }

    private static void drawTile(GuiGraphicsExtractor graphics, HoloMapTerrainTile tile,
            double centerX, double centerZ, double zoom, int x, int y, int w, int h, int visibleTileCount) {
        double baseX = tile.chunkX() * 16.0D;
        double baseZ = tile.chunkZ() * 16.0D;
        int screenX = x + w / 2 + (int) Math.floor((baseX - centerX) * zoom);
        int screenY = y + h / 2 + (int) Math.floor((baseZ - centerZ) * zoom);
        int chunkSize = Math.max(1, (int) Math.ceil(16.0D * zoom));
        if (chunkSize <= 18 || !HoloMapRenderer.highDetailTerrainAllowed(zoom, visibleTileCount, highDetailTerrain())) {
            graphics.fill(screenX, screenY, screenX + chunkSize, screenY + chunkSize,
                    HoloMapVisualStyle.terrainColor(tile.averageColor()));
            return;
        }
        int pixelSize = Math.max(1, (int) Math.ceil(zoom));
        for (int localZ = 0; localZ < HoloMapTerrainTile.SIZE; localZ++) {
            for (int localX = 0; localX < HoloMapTerrainTile.SIZE; localX++) {
                int px = x + w / 2 + (int) Math.floor((baseX + localX - centerX) * zoom);
                int py = y + h / 2 + (int) Math.floor((baseZ + localZ - centerZ) * zoom);
                graphics.fill(px, py, px + pixelSize, py + pixelSize,
                        HoloMapVisualStyle.terrainColor(tile.pixel(localX, localZ)));
            }
        }
    }

    private static void drawMarkers(GuiGraphicsExtractor graphics, Player player,
            int x, int y, int w, int h, double zoom) {
        String dimension = player.level().dimension().identifier().toString();
        double centerX = player.getX();
        double centerZ = player.getZ();
        HoloMapSnapshotPacket snapshot = HoloMapClientState.snapshotForDimension(dimension);
        drawZones(graphics, snapshot.zones(), x, y, w, h, zoom, centerX, centerZ, dimension);
        drawOverlays(graphics, player, snapshot.overlays(), x, y, w, h, zoom, centerX, centerZ);
        drawRoutes(graphics, player, snapshot.routes(), x, y, w, h, zoom, centerX, centerZ);
        int limit = markerLimit();
        HoloMapClientState.markersForDimension(dimension).stream()
                .filter(marker -> HoloMapVisibility.visibleInNormalView(marker.state()))
                .sorted(Comparator.comparingDouble(marker -> HoloMapVisualPriority.drawPriority(
                        distance(centerX, centerZ, marker.x(), marker.z()), marker.state(), marker.kind(), false)))
                .limit(limit)
                .forEach(marker -> {
                    int mx = x + w / 2 + (int) Math.round((marker.x() - centerX) * zoom);
                    int my = y + h / 2 + (int) Math.round((marker.z() - centerZ) * zoom);
                    int color = HoloMapVisualStyle.markerColor(player, marker);
                    if (marker.precision() == HoloMapPrecision.VIRTUAL) {
                        color = HoloMapVisualStyle.withAlpha(color, 0x72);
                    }
                    int size = HoloMapVisualStyle.markerScalePixels(4);
                    if (mx < x - 8 || mx > x + w + 8 || my < y - 8 || my > y + h + 8) {
                        HoloMapGlyphRenderer.drawEdgeIndicator(graphics,
                                Math.max(x + 4, Math.min(x + w - 4, mx)),
                                Math.max(y + 4, Math.min(y + h - 4, my)), color);
                        return;
                    }
                    HoloMapGlyphRenderer.drawMarker(graphics, marker, mx, my, color, size, false);
                });
    }

    private static void drawRoutes(GuiGraphicsExtractor graphics, Player player,
            List<HoloMapSnapshotPacket.RouteData> routes, int x, int y, int w, int h, double zoom,
            double centerX, double centerZ) {
        String dimension = player.level().dimension().identifier().toString();
        for (HoloMapSnapshotPacket.RouteData route : routes) {
            if (!HoloMapVisibility.visibleInNormalView(route.state())) {
                continue;
            }
            List<HoloMapSnapshotPacket.RoutePointData> points = route.points().stream()
                    .filter(point -> dimension.equals(point.dimension()))
                    .sorted(Comparator.comparingInt(HoloMapSnapshotPacket.RoutePointData::order))
                    .toList();
            if (points.size() < 2) {
                continue;
            }
            int color = HoloMapVisualStyle.withAlpha(route.color(), 0x99);
            for (int i = 1; i < points.size(); i++) {
                HoloMapSnapshotPacket.RoutePointData previous = points.get(i - 1);
                HoloMapSnapshotPacket.RoutePointData current = points.get(i);
                int x0 = x + w / 2 + (int) Math.round((previous.x() - centerX) * zoom);
                int y0 = y + h / 2 + (int) Math.round((previous.z() - centerZ) * zoom);
                int x1 = x + w / 2 + (int) Math.round((current.x() - centerX) * zoom);
                int y1 = y + h / 2 + (int) Math.round((current.z() - centerZ) * zoom);
                if ((x0 < x - 16 && x1 < x - 16) || (x0 > x + w + 16 && x1 > x + w + 16)
                        || (y0 < y - 16 && y1 < y - 16) || (y0 > y + h + 16 && y1 > y + h + 16)) {
                    continue;
                }
                HoloMapGlyphRenderer.drawLine(graphics, x0, y0, x1, y1, color);
            }
        }
    }

    private static void drawOverlays(GuiGraphicsExtractor graphics, Player player,
            List<HoloMapSnapshotPacket.OverlayData> overlays, int x, int y, int w, int h, double zoom,
            double centerX, double centerZ) {
        String dimension = player.level().dimension().identifier().toString();
        for (HoloMapSnapshotPacket.OverlayData overlay : overlays) {
            if (!dimension.equals(overlay.dimension()) || !HoloMapVisibility.visibleInNormalView(overlay.state())) {
                continue;
            }
            int cx = x + w / 2 + (int) Math.round((overlay.x() - centerX) * zoom);
            int cy = y + h / 2 + (int) Math.round((overlay.z() - centerZ) * zoom);
            int radius = Math.max(4, Math.min(140, (int) Math.round(overlay.radius() * zoom)));
            if (cx + radius < x || cx - radius > x + w || cy + radius < y || cy - radius > y + h) {
                continue;
            }
            int color = HoloMapVisualStyle.withAlpha(overlay.color(),
                    overlay.precision() == HoloMapPrecision.VIRTUAL ? 0x20 : 0x32);
            drawRing(graphics, cx, cy, radius, color);
        }
    }

    private static void drawZones(GuiGraphicsExtractor graphics, List<HoloMapSnapshotPacket.ZoneData> zones,
            int x, int y, int w, int h, double zoom, double centerX, double centerZ, String dimension) {
        for (HoloMapSnapshotPacket.ZoneData zone : zones) {
            if (!dimension.equals(zone.dimension()) || !HoloMapVisibility.visibleInNormalView(zone.state())) {
                continue;
            }
            int cx = x + w / 2 + (int) Math.round((zone.x() - centerX) * zoom);
            int cy = y + h / 2 + (int) Math.round((zone.z() - centerZ) * zoom);
            int color = HoloMapVisualStyle.withAlpha(zone.outlineColor(), 0x28);
            switch (zone.shape()) {
                case CIRCLE -> {
                    int radius = Math.max(4, Math.min(120, (int) Math.round(zone.radius() * zoom)));
                    if (cx + radius >= x && cx - radius <= x + w && cy + radius >= y && cy - radius <= y + h) {
                        drawRing(graphics, cx, cy, radius, color);
                    }
                }
                case RECT -> {
                    int halfW = Math.max(4, Math.min(120, (int) Math.round(zone.width() * zoom / 2.0D)));
                    int halfH = Math.max(4, Math.min(120, (int) Math.round(zone.depth() * zoom / 2.0D)));
                    if (cx + halfW >= x && cx - halfW <= x + w && cy + halfH >= y && cy - halfH <= y + h) {
                        graphics.outline(cx - halfW, cy - halfH, halfW * 2, halfH * 2, color);
                    }
                }
                case POLYGON, CORRIDOR -> {
                    List<HoloMapSnapshotPacket.ZonePointData> points = zone.points().stream()
                            .filter(point -> dimension.equals(point.dimension()))
                            .toList();
                    for (int i = 1; i < points.size(); i++) {
                        HoloMapSnapshotPacket.ZonePointData previous = points.get(i - 1);
                        HoloMapSnapshotPacket.ZonePointData current = points.get(i);
                        int x0 = x + w / 2 + (int) Math.round((previous.x() - centerX) * zoom);
                        int y0 = y + h / 2 + (int) Math.round((previous.z() - centerZ) * zoom);
                        int x1 = x + w / 2 + (int) Math.round((current.x() - centerX) * zoom);
                        int y1 = y + h / 2 + (int) Math.round((current.z() - centerZ) * zoom);
                        if ((x0 < x - 8 && x1 < x - 8) || (x0 > x + w + 8 && x1 > x + w + 8)
                                || (y0 < y - 8 && y1 < y - 8) || (y0 > y + h + 8 && y1 > y + h + 8)) {
                            continue;
                        }
                        HoloMapGlyphRenderer.drawLine(graphics, x0, y0, x1, y1, color);
                    }
                    if (zone.shape() == HoloMapZoneShape.POLYGON && points.size() > 2) {
                        HoloMapSnapshotPacket.ZonePointData first = points.getFirst();
                        HoloMapSnapshotPacket.ZonePointData last = points.getLast();
                        HoloMapGlyphRenderer.drawLine(graphics,
                                x + w / 2 + (int) Math.round((last.x() - centerX) * zoom),
                                y + h / 2 + (int) Math.round((last.z() - centerZ) * zoom),
                                x + w / 2 + (int) Math.round((first.x() - centerX) * zoom),
                                y + h / 2 + (int) Math.round((first.z() - centerZ) * zoom),
                                color);
                    }
                }
            }
        }
    }

    private static void drawRing(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        int segments = radius > 48 ? 24 : 16;
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

    private static void drawWaypoints(GuiGraphicsExtractor graphics, Player player,
            int x, int y, int w, int h, double zoom) {
        String dimension = player.level().dimension().identifier().toString();
        double centerX = player.getX();
        double centerZ = player.getZ();
        int limit = markerLimit();
        HoloMapWaypointClientState.waypoints().stream()
                .filter(HoloMapWaypoint::visible)
                .filter(waypoint -> waypoint.inDimension(dimension))
                .sorted(Comparator.comparing(HoloMapWaypoint::isDeathpoint).reversed()
                        .thenComparingDouble(waypoint -> distance(centerX, centerZ, waypoint.x(), waypoint.z()))
                        .thenComparing(HoloMapWaypoint::title, String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .forEach(waypoint -> {
                    int wx = x + w / 2 + (int) Math.round((waypoint.x() - centerX) * zoom);
                    int wy = y + h / 2 + (int) Math.round((waypoint.z() - centerZ) * zoom);
                    int color = waypoint.isDeathpoint() ? HoloMapVisualStyle.danger(player) : waypoint.color();
                    if (wx < x - 8 || wx > x + w + 8 || wy < y - 8 || wy > y + h + 8) {
                        HoloMapGlyphRenderer.drawEdgeIndicator(graphics,
                                Math.max(x + 4, Math.min(x + w - 4, wx)),
                                Math.max(y + 4, Math.min(y + h - 4, wy)), color);
                        return;
                    }
                    HoloMapGlyphRenderer.drawWaypoint(graphics, waypoint, wx, wy, color,
                            HoloMapVisualStyle.markerScalePixels(4), false);
                });
    }

    private static void drawPlayer(GuiGraphicsExtractor graphics, Player player, int x, int y, int size) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        HoloMapGlyphRenderer.drawPlayer(graphics, cx, cy, player.getYRot(),
                HoloMapVisualStyle.markerScalePixels(5));
    }

    private static void drawReadout(GuiGraphicsExtractor graphics, Font font, Player player, int x, int y, int size) {
        String text = HoloMapTerrainClientState.discoveredCount() <= 0
                ? "HOLOMAP pending real chunks"
                : "HOLOMAP " + HoloMapTerrainClientState.discoveredCount() + " real chunks";
        if (booleanConfig(Config.MINIMAP_SHOW_COORDINATES, true)) {
            text += " // " + player.blockPosition().getX() + "," + player.blockPosition().getZ();
        }
        graphics.text(font, Component.literal(text), x, y + size + 5, HoloMapVisualStyle.text(player), true);
    }

    private static void drawGrid(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        for (int gx = x; gx <= x + w; gx += 16) {
            graphics.fill(gx, y, gx + 1, y + h, HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.accent(Minecraft.getInstance().player), 0x24));
        }
        for (int gy = y; gy <= y + h; gy += 16) {
            graphics.fill(x, gy, x + w, gy + 1, HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.accent(Minecraft.getInstance().player), 0x24));
        }
    }

    private static boolean enabled() {
        try {
            return toggledVisible && Config.MINIMAP_ENABLED.get();
        } catch (RuntimeException exception) {
            return toggledVisible;
        }
    }

    private static Config.MiniMapCorner corner() {
        if (cornerOverride != null) {
            return cornerOverride;
        }
        try {
            return Config.MINIMAP_CORNER.get();
        } catch (RuntimeException exception) {
            return Config.MiniMapCorner.TOP_RIGHT;
        }
    }

    private static int minimapSize() {
        try {
            return Math.max(64, Math.min(196, Config.MINIMAP_SIZE.get()));
        } catch (RuntimeException exception) {
            return 104;
        }
    }

    private static double minimapZoom() {
        try {
            return Math.max(0.5D, Math.min(4.0D, Config.MINIMAP_ZOOM.get() + zoomOffset));
        } catch (RuntimeException exception) {
            return 1.35D;
        }
    }

    private static int markerLimit() {
        try {
            return Math.max(0, Math.min(192, Config.MINIMAP_MARKER_DENSITY.get()));
        } catch (RuntimeException exception) {
            return 24;
        }
    }

    private static boolean highDetailTerrain() {
        return booleanConfig(Config.MINIMAP_HIGH_DETAIL_TERRAIN, false);
    }

    private static void renderCoreFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        try {
            Class.forName("com.knoxhack.echoholomap.integration.HoloMapRenderCoreClientIntegration")
                    .getMethod("drawMinimapFrame", GuiGraphicsExtractor.class, int.class, int.class, int.class, int.class)
                    .invoke(null, graphics, x, y, width, height);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    private static boolean booleanConfig(com.echoplatform.echocore.api.config.EchoNativeConfigSpec.BooleanValue value,
            boolean fallback) {
        try {
            return value.get();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static double distance(double x0, double z0, double x1, double z1) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
