package com.knoxhack.echoholomap.client;

import com.echoplatform.echocore.api.IMapMarker;
import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoholomap.EchoHoloMapClient;
import com.knoxhack.echoholomap.network.HoloMapChunkActionPacket;
import com.knoxhack.echoholomap.network.HoloMapClientState;
import com.knoxhack.echoholomap.network.HoloMapSnapshotPacket;
import com.knoxhack.echoholomap.network.HoloMapSyncRequestPacket;
import com.knoxhack.echoholomap.network.HoloMapTerrainClientState;
import com.knoxhack.echoholomap.network.HoloMapTileRequestPacket;
import com.knoxhack.echoholomap.network.HoloMapWaypointActionPacket;
import com.knoxhack.echoholomap.network.HoloMapWaypointClientState;
import com.knoxhack.echoholomap.map.HoloMapVisibility;
import com.knoxhack.echoholomap.integration.HoloMapSoundHooks;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint.Scope;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class HoloMapFullScreenMapScreen extends Screen {
    private static final long SYNC_MIN_INTERVAL_TICKS = 40L;
    private static final long SYNC_MAX_INTERVAL_TICKS = 600L;
    private static final double SYNC_MOVE_DISTANCE_BLOCKS = 192.0D;
    private static final int BG = 0xF002070A;
    private static final int PANEL = 0xDD061014;
    private static final int PANEL_ALT = 0xBB0A1720;
    private static final int ROW = 0x77112430;
    private static final int ROW_HOVER = 0xAA183642;
    private static final int ACCENT = HoloMapVisualStyle.ACCENT;

    private final HoloMapRenderer renderer = new HoloMapRenderer();
    private final List<Hitbox> hitboxes = new ArrayList<>();
    private final List<MarkerHit> markerHits = new ArrayList<>();
    private final List<WaypointHit> waypointHits = new ArrayList<>();
    private double centerX;
    private double centerZ;
    private double zoom = 1.35D;
    private boolean cameraReady;
    private boolean draggingMap;
    private boolean showWaypoints = true;
    private boolean showMarkers = true;
    private HoloMapVisibility.FieldMode fieldMode = HoloMapVisibility.FieldMode.AUTO_NEAR;
    private boolean actionMenuOpen;
    private int actionMenuX;
    private int actionMenuY;
    private double actionWorldX;
    private double actionWorldZ;
    private String selectedMarkerId = "";
    private String selectedWaypointId = "";
    private long lastSyncTick = -200L;
    private String lastSyncDimension = "";
    private double lastSyncPlayerX = Double.NaN;
    private double lastSyncPlayerZ = Double.NaN;
    private long lastTerrainRequestTick = -200L;
    private boolean renderedOnce;
    private HoloMapRenderer.RenderResult lastRenderResult =
            new HoloMapRenderer.RenderResult(0, 0, 0, 0, 0, 0, List.of(), List.of(), false);
    private int lastRequestChunkX = Integer.MIN_VALUE;
    private int lastRequestChunkZ = Integer.MIN_VALUE;
    private int lastRequestRadius = -1;
    private EntryCacheKey entryCacheKey;
    private List<MapEntry> cachedEntries = List.of();
    private int mapX;
    private int mapY;
    private int mapW;
    private int mapH;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public HoloMapFullScreenMapScreen() {
        super(Component.translatable("screen.echoholomap.fullscreen"));
    }

    @Override
    protected void init() {
        HoloMapLocalWaypointStore.ensureLoaded();
        HoloMapSoundHooks.play(Minecraft.getInstance().player, HoloMapSoundHooks.OPEN);
        renderedOnce = false;
        requestSync(true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        hitboxes.clear();
        markerHits.clear();
        waypointHits.clear();
        HoloMapLocalWaypointStore.ensureLoaded();
        layout();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            drawOffline(graphics);
            return;
        }

        maybeRequestSync();
        ensureCamera();
        if (renderedOnce) {
            requestTerrain(false);
        }

        String dimension = minecraft.player.level().dimension().identifier().toString();
        HoloMapSnapshotPacket snapshot = HoloMapClientState.snapshotForDimension(dimension);
        List<HoloMapSnapshotPacket.MarkerData> markers = visibleMarkers(snapshot);
        List<HoloMapWaypoint> waypoints = visibleWaypoints();

        graphics.fill(0, 0, width, height, BG);
        drawHeader(graphics, minecraft.font, snapshot, mouseX, mouseY);
        drawMap(graphics, minecraft.font, snapshot, markers, waypoints, mouseX, mouseY);
        drawDetailPanel(graphics, minecraft.font, snapshot, markers, waypoints, mouseX, mouseY);
        renderedOnce = true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
            return EchoHoloMapClient.dispatchNativeFullscreenMouse(
                    this,
                    "click",
                    event.x(),
                    event.y(),
                    event.button(),
                    event.modifiers(),
                    doubleClick,
                    0.0D,
                    0.0D);
        }
        return handleNativeRouteMouse("click", event.x(), event.y(), event.button(), event.modifiers(), doubleClick, 0.0D, 0.0D)
                || super.mouseClicked(event, doubleClick);
    }

    public boolean handleNativeRouteMouse(
            String phase,
            double mouseX,
            double mouseY,
            int button,
            int modifiers,
            boolean doubleClick,
            double dragX,
            double dragY
    ) {
        if ("drag".equals(phase)) {
            return handleNativeRouteMouseDrag(button, dragX, dragY);
        }
        if ("release".equals(phase)) {
            return handleNativeRouteMouseRelease(button);
        }
        if (!"click".equals(phase)) {
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && inside(mouseX, mouseY, mapX, mapY, mapW, mapH)) {
            actionMenuOpen = true;
            actionMenuX = (int) mouseX;
            actionMenuY = (int) mouseY;
            actionWorldX = screenToWorldX(mouseX);
            actionWorldZ = screenToWorldZ(mouseY);
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            actionMenuOpen = false;
            return false;
        }
        for (Hitbox hitbox : List.copyOf(hitboxes)) {
            if (!hitbox.inside(mouseX, mouseY)) {
                continue;
            }
            handleHitbox(hitbox);
            actionMenuOpen = false;
            return true;
        }
        for (WaypointHit hit : waypointHits) {
            if (inside(mouseX, mouseY, hit.x() - 7, hit.y() - 7, 14, 14)) {
                selectedWaypointId = hit.waypoint().id().toString();
                selectedMarkerId = "";
                actionMenuOpen = false;
                return true;
            }
        }
        for (MarkerHit hit : markerHits) {
            if (inside(mouseX, mouseY, hit.x() - 6, hit.y() - 6, 12, 12)) {
                selectedMarkerId = hit.marker().id().toString();
                selectedWaypointId = "";
                actionMenuOpen = false;
                return true;
            }
        }
        if (inside(mouseX, mouseY, mapX, mapY, mapW, mapH)) {
            if (doubleClick) {
                centerOnPlayer();
            } else {
                draggingMap = true;
                selectedMarkerId = "";
                selectedWaypointId = "";
            }
            actionMenuOpen = false;
            return true;
        }
        actionMenuOpen = false;
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
            return EchoHoloMapClient.dispatchNativeFullscreenMouse(
                    this,
                    "drag",
                    event.x(),
                    event.y(),
                    event.button(),
                    event.modifiers(),
                    false,
                    dragX,
                    dragY);
        }
        return handleNativeRouteMouseDrag(event.button(), dragX, dragY) || super.mouseDragged(event, dragX, dragY);
    }

    private boolean handleNativeRouteMouseDrag(int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingMap) {
            centerX -= dragX / Math.max(0.25D, zoom);
            centerZ -= dragY / Math.max(0.25D, zoom);
            cameraReady = true;
            requestTerrain(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
            return EchoHoloMapClient.dispatchNativeFullscreenMouse(
                    this,
                    "release",
                    event.x(),
                    event.y(),
                    event.button(),
                    event.modifiers(),
                    false,
                    0.0D,
                    0.0D);
        }
        return handleNativeRouteMouseRelease(event.button()) || super.mouseReleased(event);
    }

    private boolean handleNativeRouteMouseRelease(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingMap) {
            draggingMap = false;
            requestTerrain(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
            return EchoHoloMapClient.dispatchNativeFullscreenScroll(this, mouseX, mouseY, scrollX, scrollY);
        }
        return handleNativeRouteScroll(mouseX, mouseY, scrollX, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean handleNativeRouteScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!inside(mouseX, mouseY, mapX, mapY, mapW, mapH)) {
            return false;
        }
        double worldX = screenToWorldX(mouseX);
        double worldZ = screenToWorldZ(mouseY);
        double before = zoom;
        zoom = clamp(zoom * (scrollY > 0.0D ? 1.2D : 0.82D), 0.25D, 8.0D);
        if (before != zoom) {
            centerX += worldX - screenToWorldX(mouseX);
            centerZ += worldZ - screenToWorldZ(mouseY);
            requestTerrain(true);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
            return EchoHoloMapClient.dispatchNativeFullscreenKey(this, key);
        }
        return handleNativeRouteKey(key) || super.keyPressed(event);
    }

    public boolean handleNativeRouteKey(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_J) {
            HoloMapSoundHooks.play(Minecraft.getInstance().player, HoloMapSoundHooks.CLOSE);
            EchoNativeLoadStatus lifecycleStatus = EchoHoloMapClient.publishNativeScreenLifecycle(
                    "close",
                    "holomap.fullscreen.close",
                    getClass().getName(),
                    Map.of("transitionSource", "fullscreen_key"));
            if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()
                    && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
                return false;
            }
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        if (key == GLFW.GLFW_KEY_R) {
            requestSync(true);
            return true;
        }
        if (key == GLFW.GLFW_KEY_W) {
            showWaypoints = !showWaypoints;
            return true;
        }
        if (key == GLFW.GLFW_KEY_V) {
            showMarkers = !showMarkers;
            return true;
        }
        if (key == GLFW.GLFW_KEY_F) {
            fieldMode = fieldMode.next();
            return true;
        }
        if (key == GLFW.GLFW_KEY_C || key == GLFW.GLFW_KEY_HOME) {
            centerOnPlayer();
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
            deleteSelectedWaypoint();
            return true;
        }
        double pan = Math.max(16.0D, 96.0D / Math.max(0.25D, zoom));
        if (key == GLFW.GLFW_KEY_LEFT) {
            centerX -= pan;
        } else if (key == GLFW.GLFW_KEY_RIGHT) {
            centerX += pan;
        } else if (key == GLFW.GLFW_KEY_UP) {
            centerZ -= pan;
        } else if (key == GLFW.GLFW_KEY_DOWN) {
            centerZ += pan;
        } else if (key == GLFW.GLFW_KEY_EQUAL || key == GLFW.GLFW_KEY_KP_ADD) {
            zoom = clamp(zoom * 1.2D, 0.25D, 8.0D);
        } else if (key == GLFW.GLFW_KEY_MINUS || key == GLFW.GLFW_KEY_KP_SUBTRACT) {
            zoom = clamp(zoom * 0.82D, 0.25D, 8.0D);
        } else {
            return false;
        }
        cameraReady = true;
        requestTerrain(true);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void layout() {
        int margin = width < 360 ? 4 : 8;
        int top = 28;
        panelW = width >= 460 ? Math.min(220, Math.max(176, width / 4)) : 0;
        panelX = panelW == 0 ? width : width - panelW - margin;
        panelY = top;
        panelH = Math.max(1, height - top - margin);
        mapX = margin;
        mapY = top;
        mapW = Math.max(1, width - margin * 2 - (panelW == 0 ? 0 : panelW + margin));
        mapH = Math.max(1, height - top - margin);
    }

    private void drawOffline(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, width, height, BG);
        graphics.text(Minecraft.getInstance().font, "ECHO HOLOMAP OFFLINE", 12, 12,
                HoloMapVisualStyle.warning(null), false);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, Font font, HoloMapSnapshotPacket snapshot,
            int mouseX, int mouseY) {
        graphics.fill(0, 0, width, 24, 0xF0061014);
        graphics.fill(0, 0, Math.max(56, width / 5), 2, ACCENT);
        graphics.text(font, "ECHO HOLOMAP", 10, 8, HoloMapVisualStyle.TEXT, false);
        String status = snapshot.statusLine() + " | " + String.format(Locale.ROOT, "%.2fx", zoom);
        graphics.text(font, trim(font, status, Math.max(20, width - 360)), 100, 8,
                HoloMapVisualStyle.MUTED, false);
        button(graphics, font, width - 284, 4, 48, "CENTER", HitKind.CENTER, null, mouseX, mouseY);
        button(graphics, font, width - 232, 4, 42, "SYNC", HitKind.SYNC, null, mouseX, mouseY);
        button(graphics, font, width - 186, 4, 50, showWaypoints ? "WP ON" : "WP OFF",
                HitKind.TOGGLE_WAYPOINTS, null, mouseX, mouseY);
        button(graphics, font, width - 132, 4, 44, showMarkers ? "PIN" : "PINX",
                HitKind.TOGGLE_MARKERS, null, mouseX, mouseY);
        button(graphics, font, width - 84, 4, 50, "F " + fieldMode.label(),
                HitKind.TOGGLE_FIELDS, null, mouseX, mouseY);
        button(graphics, font, width - 28, 4, 20, "X", HitKind.CLOSE, null, mouseX, mouseY);
    }

    private void drawMap(GuiGraphicsExtractor graphics, Font font, HoloMapSnapshotPacket snapshot,
            List<HoloMapSnapshotPacket.MarkerData> markers, List<HoloMapWaypoint> waypoints,
            int mouseX, int mouseY) {
        EchoCyberGlassUi.panel(graphics, mapX, mapY, mapW, mapH, PANEL, ACCENT);
        graphics.enableScissor(mapX + 3, mapY + 3, mapX + mapW - 3, mapY + mapH - 3);
        var player = Minecraft.getInstance().player;
        HoloMapViewState state = new HoloMapViewState(
                player.level().dimension().identifier().toString(),
                mapX, mapY, mapW, mapH, centerX, centerZ, zoom, showMarkers, fieldMode, showWaypoints,
                selectedMarkerId, selectedWaypointId, mouseX, mouseY, player.getX(), player.getZ(), player.getYRot());
        lastRenderResult = renderer.render(graphics, font, state, snapshot, markers, waypoints,
                HoloMapRenderer.FULLSCREEN_BUDGET);
        markerHits.clear();
        for (HoloMapRenderer.MarkerHit hit : lastRenderResult.markerHits()) {
            markerHits.add(new MarkerHit(hit.marker(), hit.x(), hit.y()));
        }
        waypointHits.clear();
        for (HoloMapRenderer.WaypointHit hit : lastRenderResult.waypointHits()) {
            waypointHits.add(new WaypointHit(hit.waypoint(), hit.x(), hit.y()));
        }
        String dim = Minecraft.getInstance().player.level().dimension().identifier().toString();
        HoloMapTerrainClientState.DetailStats stats = HoloMapTerrainClientState.detailStats(dim);
        graphics.fill(mapX + 10, mapY + 8, mapX + mapW - 10, mapY + 22, 0x99061014);
        String culled = lastRenderResult.culledTerrainTiles() > 0
                ? " | terrain culled " + lastRenderResult.culledTerrainTiles()
                : "";
        int realChunks = HoloMapTerrainClientState.discoveredCount();
        String terrainText = HoloMapRenderer.terrainStatusLabel(lastRenderResult, realChunks);
        graphics.text(font, "ATLAS " + terrainText + " | " + stats.label() + " | XYZ "
                        + (int) centerX + " / " + (int) centerZ + culled,
                mapX + 14, mapY + 12, HoloMapVisualStyle.ACCENT, false);
        drawLegend(graphics, font, markers.size(), waypoints.size());
        if (actionMenuOpen) {
            drawActionMenu(graphics, font, mouseX, mouseY);
        }
        graphics.disableScissor();
    }

    private void drawLegend(GuiGraphicsExtractor graphics, Font font, int markerCount, int waypointCount) {
        int y = mapY + mapH - 20;
        graphics.fill(mapX + 10, y, mapX + mapW - 10, y + 14, 0x99061014);
        String text = "Markers " + markerCount + " | Waypoints " + waypointCount
                + " | Fields " + fieldMode.label() + " | V pins | F fields | RMB waypoint | R sync";
        graphics.text(font, trim(font, text, mapW - 28), mapX + 14, y + 4,
                HoloMapVisualStyle.MUTED, false);
    }

    private void drawActionMenu(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        String dimension = currentDimension();
        int chunkX = actionChunkX();
        int chunkZ = actionChunkZ();
        boolean renderableChunk = HoloMapTerrainClientState.hasRenderableTile(dimension, chunkX, chunkZ);
        List<HoloMapChunkMenuAction> chunkActions = renderableChunk
                ? HoloMapClientChunkActions.actions(dimension, chunkX, chunkZ)
                : List.of();
        int chunkRows = Math.max(1, chunkActions.size());
        int w = 152;
        int h = 64 + (selectedWaypointId.isBlank() ? 0 : 20) + chunkRows * 20;
        int x = Math.max(mapX + 8, Math.min(actionMenuX, mapX + mapW - w - 8));
        int y = Math.max(mapY + 8, Math.min(actionMenuY, mapY + mapH - h - 8));
        graphics.fill(x, y, x + w, y + h, 0xEE061014);
        graphics.outline(x, y, w, h, ACCENT);
        graphics.text(font, "MAP ACTION", x + 8, y + 7, HoloMapVisualStyle.ACCENT, false);
        button(graphics, font, x + 8, y + 22, 62, "LOCAL", HitKind.MENU_LOCAL, null, mouseX, mouseY);
        button(graphics, font, x + 78, y + 22, 66, "PERSONAL", HitKind.MENU_PERSONAL, null, mouseX, mouseY);
        button(graphics, font, x + 8, y + 42, 62, "SHARE", HitKind.MENU_SHARED, null, mouseX, mouseY);
        button(graphics, font, x + 78, y + 42, 66, "COPY", HitKind.MENU_COPY, null, mouseX, mouseY);
        int rowY = y + 62;
        if (!selectedWaypointId.isBlank()) {
            button(graphics, font, x + 8, rowY, 62, "MOVE", HitKind.MENU_MOVE, null, mouseX, mouseY);
            button(graphics, font, x + 78, rowY, 66, "DELETE", HitKind.DELETE_WAYPOINT, null, mouseX, mouseY);
            rowY += 20;
        }
        if (!renderableChunk) {
            button(graphics, font, x + 8, rowY, w - 16, "PENDING SCAN", HitKind.CHUNK_ACTION,
                    null, mouseX, mouseY, false, HoloMapVisualStyle.MUTED);
        } else if (chunkActions.isEmpty()) {
            button(graphics, font, x + 8, rowY, w - 16, "NO CLAIM ACTION", HitKind.CHUNK_ACTION,
                    null, mouseX, mouseY, false, HoloMapVisualStyle.MUTED);
        } else {
            for (HoloMapChunkMenuAction action : chunkActions) {
                button(graphics, font, x + 8, rowY, w - 16, action.label(), HitKind.CHUNK_ACTION,
                        action.menuId(), mouseX, mouseY, action.enabled(), action.color());
                rowY += 20;
            }
        }
    }

    private void drawDetailPanel(GuiGraphicsExtractor graphics, Font font, HoloMapSnapshotPacket snapshot,
            List<HoloMapSnapshotPacket.MarkerData> markers, List<HoloMapWaypoint> waypoints,
            int mouseX, int mouseY) {
        if (panelW <= 0) {
            return;
        }
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_ALT);
        graphics.outline(panelX, panelY, panelW, panelH, 0x5538DFF4);
        graphics.text(font, "MAP INDEX", panelX + 10, panelY + 10, ACCENT, false);
        int y = panelY + 30;
        y = metric(graphics, font, y, "Visible", String.valueOf(markers.size()), ACCENT);
        y = metric(graphics, font, y, "Synced", String.valueOf(snapshot.markers().size()), HoloMapVisualStyle.MUTED);
        y = metric(graphics, font, y, "Waypoints", String.valueOf(waypoints.size()), HoloMapVisualStyle.WARNING);
        y = metric(graphics, font, y, "Real Chunks", String.valueOf(HoloMapTerrainClientState.discoveredCount()),
                HoloMapVisualStyle.ACCENT);
        y += 8;
        graphics.fill(panelX + 10, y, panelX + panelW - 10, y + 1, 0x6638DFF4);
        y += 10;

        List<MapEntry> entries = listEntries(snapshot, markers, waypoints);
        int rows = Math.max(3, Math.min(10, (panelH - (y - panelY) - 120) / 17));
        for (int i = 0; i < Math.min(rows, entries.size()); i++) {
            MapEntry entry = entries.get(i);
            if (entry.header()) {
                graphics.fill(panelX + 10, y + 6, panelX + panelW - 10, y + 7, 0x4438DFF4);
                graphics.text(font, trim(font, entry.title().toUpperCase(Locale.ROOT), panelW - 28),
                        panelX + 14, y + 1, HoloMapVisualStyle.MUTED, false);
                y += 15;
                continue;
            }
            boolean selected = entry.id().equals(selectedMarkerId) || entry.id().equals(selectedWaypointId);
            boolean hovered = inside(mouseX, mouseY, panelX + 10, y - 2, panelW - 20, 16);
            graphics.fill(panelX + 10, y - 2, panelX + panelW - 10, y + 14,
                    selected ? 0x5538DFF4 : hovered ? ROW_HOVER : ROW);
            String suffix = entry.count() > 1 ? " x" + entry.count() : "";
            graphics.text(font, trim(font, entry.prefix() + " " + entry.title() + suffix, panelW - 28),
                    panelX + 14, y + 2, selected ? HoloMapVisualStyle.TEXT : entry.color(), false);
            hitboxes.add(new Hitbox(HitKind.SELECT_ENTRY, entry.id(), panelX + 10, y - 2, panelW - 20, 16));
            y += 17;
        }
        y += 8;
        graphics.fill(panelX + 10, y, panelX + panelW - 10, y + 1, 0x6638DFF4);
        y += 10;
        drawSelectionDetails(graphics, font, markers, waypoints, y, mouseX, mouseY);
    }

    private void drawSelectionDetails(GuiGraphicsExtractor graphics, Font font,
            List<HoloMapSnapshotPacket.MarkerData> markers, List<HoloMapWaypoint> waypoints,
            int y, int mouseX, int mouseY) {
        HoloMapWaypoint waypoint = selectedWaypoint(waypoints);
        if (waypoint != null) {
            graphics.text(font, trim(font, waypoint.title(), panelW - 22), panelX + 10, y,
                    waypoint.color(), false);
            y += 16;
            y = metric(graphics, font, y, "Scope", waypoint.scope().name(), waypoint.color());
            y = metric(graphics, font, y, "Dim", waypoint.dimension(), HoloMapVisualStyle.MUTED);
            y = metric(graphics, font, y, "XYZ",
                    (int) waypoint.x() + " / " + (int) waypoint.y() + " / " + (int) waypoint.z(),
                    HoloMapVisualStyle.TEXT);
            button(graphics, font, panelX + 10, y + 8, 58, "DELETE", HitKind.DELETE_WAYPOINT,
                    null, mouseX, mouseY);
            return;
        }
        HoloMapSnapshotPacket.MarkerData marker = selectedMarker(markers);
        if (marker == null) {
            graphics.text(font, "Select a marker or waypoint.", panelX + 10, y,
                    HoloMapVisualStyle.MUTED, false);
            return;
        }
        int color = HoloMapVisualStyle.markerColor(Minecraft.getInstance().player, marker);
        graphics.text(font, trim(font, marker.title(), panelW - 22), panelX + 10, y, color, false);
        y += 16;
        y = metric(graphics, font, y, "State", marker.state().name(), color);
        y = metric(graphics, font, y, "Layer", marker.layerId().getPath().replace("layer/", ""),
                HoloMapVisualStyle.MUTED);
        y = metric(graphics, font, y, "XYZ",
                (int) marker.x() + " / " + (int) marker.y() + " / " + (int) marker.z(),
                marker.precise() ? HoloMapVisualStyle.TEXT : HoloMapVisualStyle.WARNING);
        graphics.text(font, trim(font, marker.summary(), panelW - 22), panelX + 10, y + 8,
                HoloMapVisualStyle.MUTED, false);
    }

    private int metric(GuiGraphicsExtractor graphics, Font font, int y, String label, String value, int color) {
        graphics.text(font, label, panelX + 10, y, HoloMapVisualStyle.MUTED, false);
        graphics.text(font, trim(font, value, panelW - 76), panelX + 70, y, color, false);
        return y + 13;
    }

    private void handleHitbox(Hitbox hitbox) {
        switch (hitbox.kind()) {
            case CENTER -> centerOnPlayer();
            case SYNC -> requestSync(true);
            case TOGGLE_WAYPOINTS -> showWaypoints = !showWaypoints;
            case TOGGLE_MARKERS -> showMarkers = !showMarkers;
            case TOGGLE_FIELDS -> fieldMode = fieldMode.next();
            case CLOSE -> {
                HoloMapSoundHooks.play(Minecraft.getInstance().player, HoloMapSoundHooks.CLOSE);
                EchoNativeLoadStatus lifecycleStatus = EchoHoloMapClient.publishNativeScreenLifecycle(
                        "close",
                        "holomap.fullscreen.close",
                        getClass().getName(),
                        Map.of("transitionSource", "fullscreen_header_button"));
                if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()
                        && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
                    return;
                }
                Minecraft.getInstance().setScreen(null);
            }
            case MENU_LOCAL -> createWaypoint(Scope.LOCAL);
            case MENU_PERSONAL -> createWaypoint(Scope.PERSONAL);
            case MENU_SHARED -> createWaypoint(Scope.SHARED);
            case MENU_COPY -> copyActionCoordinates();
            case MENU_MOVE -> moveSelectedWaypointToAction();
            case DELETE_WAYPOINT -> deleteSelectedWaypoint();
            case CHUNK_ACTION -> sendChunkAction(hitbox.id());
            case SELECT_ENTRY -> focusEntry(hitbox.id());
        }
    }

    private void sendChunkAction(Identifier menuId) {
        if (Minecraft.getInstance().player == null || menuId == null) {
            return;
        }
        String dimension = currentDimension();
        int chunkX = actionChunkX();
        int chunkZ = actionChunkZ();
        HoloMapChunkMenuAction action = HoloMapClientChunkActions.action(menuId, dimension, chunkX, chunkZ);
        if (action == null || !action.enabled()
                || !HoloMapTerrainClientState.hasRenderableTile(dimension, chunkX, chunkZ)) {
            return;
        }
        EchoNetClientActions.sendServerboundAction(new HoloMapChunkActionPacket(
                action.providerId(), action.actionId(), dimension, chunkX, chunkZ));
        requestSync(true);
    }

    private void createWaypoint(Scope scope) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        var player = Minecraft.getInstance().player;
        long time = player.level().getGameTime();
        String dimension = player.level().dimension().identifier().toString();
        String title = switch (scope) {
            case LOCAL -> "Local " + (int) actionWorldX + ", " + (int) actionWorldZ;
            case PERSONAL -> "Personal " + (int) actionWorldX + ", " + (int) actionWorldZ;
            case SHARED -> "Shared " + (int) actionWorldX + ", " + (int) actionWorldZ;
        };
        int color = scope == Scope.SHARED ? HoloMapVisualStyle.WARNING
                : scope == Scope.PERSONAL ? HoloMapVisualStyle.SUCCESS : HoloMapVisualStyle.ACCENT;
        HoloMapWaypoint waypoint = HoloMapWaypoint.create(scope, player.getUUID(), dimension,
                actionWorldX, Math.floor(player.getY()), actionWorldZ, title, color, time);
        if (scope == Scope.LOCAL) {
            HoloMapLocalWaypointStore.upsert(waypoint);
            selectedWaypointId = waypoint.id().toString();
            selectedMarkerId = "";
        } else {
            EchoNetClientActions.sendServerboundAction(HoloMapWaypointActionPacket.upsert(waypoint));
            requestSync(true);
        }
    }

    private void moveSelectedWaypointToAction() {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        Identifier id = Identifier.tryParse(selectedWaypointId);
        if (id == null) {
            return;
        }
        HoloMapWaypoint selected = HoloMapWaypointClientState.waypoints().stream()
                .filter(waypoint -> waypoint.id().equals(id))
                .findFirst()
                .orElse(null);
        if (selected == null) {
            return;
        }
        var player = Minecraft.getInstance().player;
        long time = player.level().getGameTime();
        HoloMapWaypoint moved = new HoloMapWaypoint(
                selected.id(), selected.owner(), selected.scope(),
                player.level().dimension().identifier().toString(),
                actionWorldX, Math.floor(player.getY()), actionWorldZ,
                selected.title(), selected.color(), selected.icon(), selected.visible(),
                selected.createdTime(), time);
        if (moved.scope() == Scope.LOCAL) {
            HoloMapLocalWaypointStore.upsert(moved);
        } else {
            EchoNetClientActions.sendServerboundAction(HoloMapWaypointActionPacket.upsert(moved));
            requestSync(true);
        }
        selectedWaypointId = moved.id().toString();
        selectedMarkerId = "";
    }

    private void deleteSelectedWaypoint() {
        Identifier id = Identifier.tryParse(selectedWaypointId);
        if (id == null) {
            return;
        }
        HoloMapWaypoint selected = HoloMapWaypointClientState.waypoints().stream()
                .filter(waypoint -> waypoint.id().equals(id))
                .findFirst()
                .orElse(null);
        if (selected == null) {
            return;
        }
        if (selected.scope() == Scope.LOCAL) {
            HoloMapLocalWaypointStore.remove(id);
        } else {
            EchoNetClientActions.sendServerboundAction(HoloMapWaypointActionPacket.delete(id));
            requestSync(true);
        }
        selectedWaypointId = "";
    }

    private void copyActionCoordinates() {
        int y = Minecraft.getInstance().player == null ? 64 : (int) Minecraft.getInstance().player.getY();
        Minecraft.getInstance().keyboardHandler.setClipboard((int) actionWorldX + " " + y + " " + (int) actionWorldZ);
    }

    private void focusEntry(Identifier id) {
        if (id == null) {
            return;
        }
        for (HoloMapWaypoint waypoint : HoloMapWaypointClientState.waypoints()) {
            if (waypoint.id().equals(id)) {
                selectedWaypointId = id.toString();
                selectedMarkerId = "";
                centerX = waypoint.x();
                centerZ = waypoint.z();
                requestTerrain(true);
                return;
            }
        }
        String dimension = Minecraft.getInstance().player == null
                ? "minecraft:overworld"
                : Minecraft.getInstance().player.level().dimension().identifier().toString();
        for (HoloMapSnapshotPacket.MarkerData marker : HoloMapClientState.markersForDimension(dimension)) {
            if (marker.id().equals(id)) {
                selectedMarkerId = id.toString();
                selectedWaypointId = "";
                centerX = marker.x();
                centerZ = marker.z();
                requestTerrain(true);
                return;
            }
        }
    }

    private void maybeRequestSync() {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        var player = Minecraft.getInstance().player;
        long now = player.level().getGameTime();
        String dimension = player.level().dimension().identifier().toString();
        double dx = player.getX() - lastSyncPlayerX;
        double dz = player.getZ() - lastSyncPlayerZ;
        boolean moved = Double.isNaN(lastSyncPlayerX)
                || dx * dx + dz * dz >= SYNC_MOVE_DISTANCE_BLOCKS * SYNC_MOVE_DISTANCE_BLOCKS;
        boolean dimensionChanged = !dimension.equals(lastSyncDimension);
        boolean stale = now - lastSyncTick > SYNC_MAX_INTERVAL_TICKS;
        if (HoloMapClientState.snapshot().gameTime() == 0L || dimensionChanged || moved || stale) {
            requestSync(false);
        }
    }

    private void requestSync(boolean force) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        var player = Minecraft.getInstance().player;
        long now = player.level().getGameTime();
        if (!force && now - lastSyncTick < SYNC_MIN_INTERVAL_TICKS) {
            return;
        }
        lastSyncTick = now;
        lastSyncDimension = player.level().dimension().identifier().toString();
        lastSyncPlayerX = player.getX();
        lastSyncPlayerZ = player.getZ();
        EchoNetClientActions.sendServerboundAction(new HoloMapSyncRequestPacket());
    }

    private void requestTerrain(boolean force) {
        if (Minecraft.getInstance().player == null || mapW <= 0 || mapH <= 0) {
            return;
        }
        long now = Minecraft.getInstance().player.level().getGameTime();
        int centerChunkX = Math.floorDiv((int) Math.floor(centerX), 16);
        int centerChunkZ = Math.floorDiv((int) Math.floor(centerZ), 16);
        int radius = visibleChunkRadius();
        if (!force && now - lastTerrainRequestTick < 20L) {
            return;
        }
        if (!force && centerChunkX == lastRequestChunkX
                && centerChunkZ == lastRequestChunkZ
                && radius == lastRequestRadius) {
            return;
        }
        lastTerrainRequestTick = now;
        lastRequestChunkX = centerChunkX;
        lastRequestChunkZ = centerChunkZ;
        lastRequestRadius = radius;
        EchoNetClientActions.sendServerboundAction(new HoloMapTileRequestPacket(
                Minecraft.getInstance().player.level().dimension().identifier().toString(),
                centerChunkX, centerChunkZ, radius));
    }

    private void ensureCamera() {
        if (cameraReady || Minecraft.getInstance().player == null) {
            return;
        }
        centerOnPlayer();
    }

    private void centerOnPlayer() {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        centerX = Minecraft.getInstance().player.getX();
        centerZ = Minecraft.getInstance().player.getZ();
        cameraReady = true;
        if (renderedOnce) {
            requestTerrain(true);
        }
    }

    private int visibleChunkRadius() {
        double blocksAcross = Math.max(mapW, mapH) / Math.max(0.25D, zoom);
        return Math.max(1, Math.min(32, (int) Math.ceil(blocksAcross / 32.0D) + 1));
    }

    private int worldToScreenX(double worldX) {
        return mapX + mapW / 2 + (int) Math.round((worldX - centerX) * zoom);
    }

    private int worldToScreenZ(double worldZ) {
        return mapY + mapH / 2 + (int) Math.round((worldZ - centerZ) * zoom);
    }

    private double screenToWorldX(double screenX) {
        return centerX + (screenX - (mapX + mapW / 2.0D)) / Math.max(0.25D, zoom);
    }

    private double screenToWorldZ(double screenY) {
        return centerZ + (screenY - (mapY + mapH / 2.0D)) / Math.max(0.25D, zoom);
    }

    private String currentDimension() {
        return Minecraft.getInstance().player == null
                ? "minecraft:overworld"
                : Minecraft.getInstance().player.level().dimension().identifier().toString();
    }

    private int actionChunkX() {
        return Math.floorDiv((int) Math.floor(actionWorldX), 16);
    }

    private int actionChunkZ() {
        return Math.floorDiv((int) Math.floor(actionWorldZ), 16);
    }

    private List<HoloMapSnapshotPacket.MarkerData> visibleMarkers(HoloMapSnapshotPacket snapshot) {
        if (!showMarkers || Minecraft.getInstance().player == null) {
            return List.of();
        }
        String dimension = Minecraft.getInstance().player.level().dimension().identifier().toString();
        List<HoloMapSnapshotPacket.MarkerData> dimensionMarkers = HoloMapClientState.markersForDimension(dimension);
        return dimensionMarkers.stream()
                .filter(marker -> dimension.equals(marker.dimension()))
                .filter(marker -> HoloMapVisibility.visibleInNormalView(marker.state()))
                .toList();
    }

    private List<HoloMapWaypoint> visibleWaypoints() {
        if (!showWaypoints || Minecraft.getInstance().player == null) {
            return List.of();
        }
        String dimension = Minecraft.getInstance().player.level().dimension().identifier().toString();
        return HoloMapWaypointClientState.waypoints().stream()
                .filter(HoloMapWaypoint::visible)
                .filter(waypoint -> waypoint.inDimension(dimension))
                .toList();
    }

    private List<MapEntry> listEntries(HoloMapSnapshotPacket snapshot, List<HoloMapSnapshotPacket.MarkerData> markers,
            List<HoloMapWaypoint> waypoints) {
        String dimension = Minecraft.getInstance().player == null
                ? "minecraft:overworld"
                : Minecraft.getInstance().player.level().dimension().identifier().toString();
        EntryCacheKey nextKey = new EntryCacheKey(dimension, snapshot.gameTime(), HoloMapWaypointClientState.revision(),
                selectedMarkerId, selectedWaypointId, showMarkers, fieldMode, showWaypoints, distanceBucket());
        if (nextKey.equals(entryCacheKey)) {
            return cachedEntries;
        }
        entryCacheKey = nextKey;
        List<MapEntry> entries = new ArrayList<>();
        for (HoloMapWaypoint waypoint : waypoints) {
            entries.add(MapEntry.waypoint(waypoint, distanceToCenter(waypoint.x(), waypoint.z()),
                    waypoint.id().toString().equals(selectedWaypointId)));
        }
        Map<String, MapEntryGroup> groups = new LinkedHashMap<>();
        for (HoloMapSnapshotPacket.MarkerData marker : markers) {
            String section = section(marker);
            String key = section + "|" + marker.kind() + "|" + marker.title().toLowerCase(Locale.ROOT);
            groups.computeIfAbsent(key, ignored -> new MapEntryGroup(section, marker,
                            distanceToCenter(marker.x(), marker.z())))
                    .add(marker, distanceToCenter(marker.x(), marker.z()),
                            marker.id().toString().equals(selectedMarkerId));
        }
        for (MapEntryGroup group : groups.values()) {
            entries.add(group.entry());
        }
        entries.sort(Comparator.comparingInt((MapEntry entry) -> sectionOrder(entry.section()))
                .thenComparing(MapEntry::selected, Comparator.reverseOrder())
                .thenComparingDouble(MapEntry::distance)
                .thenComparing(MapEntry::title, String.CASE_INSENSITIVE_ORDER));
        ArrayList<MapEntry> withHeaders = new ArrayList<>();
        String lastSection = "";
        for (MapEntry entry : entries) {
            if (!entry.section().equals(lastSection)) {
                lastSection = entry.section();
                withHeaders.add(MapEntry.header(lastSection));
            }
            withHeaders.add(entry);
        }
        cachedEntries = List.copyOf(withHeaders);
        return cachedEntries;
    }

    private static String section(HoloMapSnapshotPacket.MarkerData marker) {
        return switch (marker.kind()) {
            case MISSION, ROUTE -> "Missions";
            case HAZARD, REGION, CRASH_SITE -> "Hazards/Regions";
            case BASE_OUTPOST, ORBITAL_SCAN, NEXUS_ANOMALY, DRONE_SCAN, GENERIC, STRUCTURE, FACTION -> "Other";
        };
    }

    private static int sectionOrder(String section) {
        return switch (section) {
            case "Waypoints" -> 0;
            case "Missions" -> 1;
            case "Hazards/Regions" -> 2;
            default -> 3;
        };
    }

    private double distanceToCenter(double x, double z) {
        if (Minecraft.getInstance().player != null) {
            double dx = x - Minecraft.getInstance().player.getX();
            double dz = z - Minecraft.getInstance().player.getZ();
            return Math.sqrt(dx * dx + dz * dz);
        }
        double dx = x - centerX;
        double dz = z - centerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private int distanceBucket() {
        if (Minecraft.getInstance().player != null) {
            int x = (int) Math.floor(Minecraft.getInstance().player.getX() / 64.0D);
            int z = (int) Math.floor(Minecraft.getInstance().player.getZ() / 64.0D);
            return x * 31 + z;
        }
        int x = (int) Math.floor(centerX / 64.0D);
        int z = (int) Math.floor(centerZ / 64.0D);
        return x * 31 + z;
    }

    private HoloMapWaypoint selectedWaypoint(List<HoloMapWaypoint> waypoints) {
        if (selectedWaypointId.isBlank()) {
            return null;
        }
        for (HoloMapWaypoint waypoint : waypoints) {
            if (selectedWaypointId.equals(waypoint.id().toString())) {
                return waypoint;
            }
        }
        return null;
    }

    private HoloMapSnapshotPacket.MarkerData selectedMarker(List<HoloMapSnapshotPacket.MarkerData> markers) {
        if (selectedMarkerId.isBlank()) {
            return null;
        }
        for (HoloMapSnapshotPacket.MarkerData marker : markers) {
            if (selectedMarkerId.equals(marker.id().toString())) {
                return marker;
            }
        }
        return null;
    }

    private void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, String label,
            HitKind kind, Identifier id, int mouseX, int mouseY) {
        button(graphics, font, x, y, w, label, kind, id, mouseX, mouseY, true, ACCENT);
    }

    private void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, String label,
            HitKind kind, Identifier id, int mouseX, int mouseY, boolean enabled, int color) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, 16);
        int safeColor = enabled ? color : HoloMapVisualStyle.MUTED;
        EchoCyberGlassUi.button(graphics, font, x, y, w, 16, trim(font, label, w - 6), hovered && enabled, enabled,
                hovered && enabled ? HoloMapVisualStyle.TEXT : safeColor);
        if (enabled) {
            hitboxes.add(new Hitbox(kind, id, x, y, w, 16));
        }
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String trim(Font font, String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
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

    private enum HitKind {
        CENTER,
        SYNC,
        TOGGLE_WAYPOINTS,
        TOGGLE_MARKERS,
        TOGGLE_FIELDS,
        CLOSE,
        MENU_LOCAL,
        MENU_PERSONAL,
        MENU_SHARED,
        MENU_COPY,
        MENU_MOVE,
        DELETE_WAYPOINT,
        CHUNK_ACTION,
        SELECT_ENTRY
    }

    private record Hitbox(HitKind kind, Identifier id, int x, int y, int w, int h) {
        boolean inside(double mouseX, double mouseY) {
            return HoloMapFullScreenMapScreen.inside(mouseX, mouseY, x, y, w, h);
        }
    }

    private record MarkerHit(HoloMapSnapshotPacket.MarkerData marker, int x, int y) {
    }

    private record WaypointHit(HoloMapWaypoint waypoint, int x, int y) {
    }

    private record MapEntry(
            Identifier id,
            String title,
            String section,
            double distance,
            int color,
            boolean waypoint,
            boolean field,
            int count,
            boolean selected,
            boolean header) {
        static MapEntry marker(HoloMapSnapshotPacket.MarkerData marker, String section, double distance,
                int count, boolean selected) {
            return new MapEntry(marker.id(), marker.title(), section, distance,
                    HoloMapVisualStyle.markerColor(Minecraft.getInstance().player, marker), false,
                    marker.radius() > 0.0F && HoloMapVisibility.markerCanGenerateField(marker.kind()),
                    count, selected, false);
        }

        static MapEntry waypoint(HoloMapWaypoint waypoint, double distance, boolean selected) {
            return new MapEntry(waypoint.id(), waypoint.title(), "Waypoints", distance, waypoint.color(), true,
                    false, 1, selected, false);
        }

        static MapEntry header(String section) {
            return new MapEntry(null, section, section, 0.0D, HoloMapVisualStyle.MUTED, false, false, 0, false, true);
        }

        String prefix() {
            return waypoint ? "W" : field ? "F" : "P";
        }
    }

    private record EntryCacheKey(String dimension, long snapshotGameTime, long waypointRevision,
            String selectedMarkerId, String selectedWaypointId, boolean showMarkers,
            HoloMapVisibility.FieldMode fieldMode, boolean showWaypoints, int distanceBucket) {
    }

    private static final class MapEntryGroup {
        private final String section;
        private HoloMapSnapshotPacket.MarkerData marker;
        private double distance;
        private int count;
        private boolean selected;

        private MapEntryGroup(String section, HoloMapSnapshotPacket.MarkerData marker, double distance) {
            this.section = section;
            this.marker = marker;
            this.distance = distance;
        }

        private void add(HoloMapSnapshotPacket.MarkerData candidate, double candidateDistance, boolean candidateSelected) {
            count++;
            if (candidateSelected || !selected && candidateDistance < distance) {
                marker = candidate;
                distance = candidateDistance;
            }
            selected |= candidateSelected;
        }

        private MapEntry entry() {
            return MapEntry.marker(marker, section, distance, count, selected);
        }
    }
}
