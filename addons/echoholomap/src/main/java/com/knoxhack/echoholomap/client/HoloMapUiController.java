package com.knoxhack.echoholomap.client;

import com.echoplatform.echocore.api.IMapMarker;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoholomap.network.HoloMapChunkActionPacket;
import com.knoxhack.echoholomap.network.HoloMapClientState;
import com.knoxhack.echoholomap.network.HoloMapSnapshotPacket;
import com.knoxhack.echoholomap.network.HoloMapSyncRequestPacket;
import com.knoxhack.echoholomap.network.HoloMapTerrainClientState;
import com.knoxhack.echoholomap.network.HoloMapTileRequestPacket;
import com.knoxhack.echoholomap.network.HoloMapWaypointClientState;
import com.knoxhack.echoholomap.map.HoloMapVisibility;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class HoloMapUiController {
    private static final long SYNC_MIN_INTERVAL_TICKS = 40L;
    private static final long SYNC_MAX_INTERVAL_TICKS = 600L;
    private static final double SYNC_MOVE_DISTANCE_BLOCKS = 192.0D;
    private static final long INTERACTIVE_RENDER_GRACE_MS = 240L;
    private static final int OVERLAY_BG = 0xB4061014;
    private static final int OVERLAY_BG_SOFT = 0x92061014;
    private static final int OVERLAY_HOVER = 0xB0183642;
    private static final int OVERLAY_SELECTED = 0x6626DFF4;
    private static final int OVERLAY_BORDER = 0x8838DFF4;
    private static final int OVERLAY_PAD = 8;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CHIP_HEIGHT = 18;
    private static final int INDEX_ROW_HEIGHT = 22;
    private static final int INDEX_HEADER_HEIGHT = 16;
    private static final HoloMapUiController FULLSCREEN = new HoloMapUiController();

    private final HoloMapRenderer renderer = new HoloMapRenderer();
    private final List<HoloMapRenderer.MarkerHit> markerHits = new ArrayList<>();
    private final List<HoloMapRenderer.WaypointHit> waypointHits = new ArrayList<>();
    private final List<OverlayHitbox> overlayHitboxes = new ArrayList<>();

    private double centerX;
    private double centerZ;
    private double zoom = 1.35D;
    private boolean cameraReady;
    private boolean renderedOnce;
    private boolean dragging;
    private long lastInteractiveMillis = Long.MIN_VALUE;
    private boolean showMarkers = true;
    private HoloMapVisibility.FieldMode fieldMode = HoloMapVisibility.FieldMode.AUTO_NEAR;
    private boolean showWaypoints = true;
    private String selectedMarkerId = "";
    private String selectedWaypointId = "";
    private long lastSyncTick = -200L;
    private String lastSyncDimension = "";
    private double lastSyncPlayerX = Double.NaN;
    private double lastSyncPlayerZ = Double.NaN;
    private long lastTerrainRequestTick = -200L;
    private boolean actionMenuOpen;
    private int actionMenuX;
    private int actionMenuY;
    private double actionWorldX;
    private double actionWorldZ;
    private int lastMapX;
    private int lastMapY;
    private int lastRequestChunkX = Integer.MIN_VALUE;
    private int lastRequestChunkZ = Integer.MIN_VALUE;
    private int lastRequestRadius = -1;
    private int lastMapWidth;
    private int lastMapHeight;
    private EntryCacheKey entryCacheKey;
    private List<Entry> cachedEntries = List.of();
    private boolean indexDrawerOpen;
    private int indexScrollOffset;
    private int indexMaxScroll;
    private int indexX;
    private int indexY;
    private int indexW;
    private int indexH;
    private HoloMapRenderer.RenderResult lastRenderResult =
            new HoloMapRenderer.RenderResult(0, 0, 0, 0, 0, 0, List.of(), List.of(), false);

    private HoloMapUiController() {
    }

    public static HoloMapUiController fullscreen() {
        return FULLSCREEN;
    }

    public void open() {
        HoloMapLocalWaypointStore.ensureLoaded();
        renderedOnce = false;
        requestSync(true);
        centerOnPlayer();
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            int mouseX, int mouseY) {
        if (graphics == null || width <= 0 || height <= 0) {
            return;
        }
        overlayHitboxes.clear();
        lastMapX = x;
        lastMapY = y;
        lastMapWidth = width;
        lastMapHeight = height;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            graphics.fill(x, y, x + width, y + height, 0xF002070A);
            if (font != null) {
                graphics.text(font, "ECHO HOLOMAP OFFLINE", x + 10, y + 10,
                        HoloMapVisualStyle.warning(null), false);
            }
            return;
        }
        HoloMapLocalWaypointStore.ensureLoaded();
        maybeRequestSync();
        ensureCamera();
        if (renderedOnce) {
            requestTerrain(false, width, height);
        }

        String dimension = minecraft.player.level().dimension().identifier().toString();
        HoloMapSnapshotPacket snapshot = HoloMapClientState.snapshotForDimension(dimension);
        List<HoloMapSnapshotPacket.MarkerData> markers = visibleMarkers(dimension);
        List<HoloMapWaypoint> waypoints = visibleWaypoints(dimension);
        HoloMapViewState state = new HoloMapViewState(dimension, x, y, width, height, centerX, centerZ, zoom,
                showMarkers, fieldMode, showWaypoints, selectedMarkerId, selectedWaypointId, mouseX, mouseY,
                minecraft.player.getX(), minecraft.player.getZ(), minecraft.player.getYRot());

        graphics.fill(x, y, x + width, y + height, 0xCC061014);
        graphics.enableScissor(x, y, x + width, y + height);
        HoloMapRenderer.RenderBudget renderBudget = fullscreenBudget(interactiveRendering());
        lastRenderResult = renderer.render(graphics, font, state, snapshot, markers, waypoints,
                renderBudget);
        graphics.disableScissor();

        markerHits.clear();
        markerHits.addAll(lastRenderResult.markerHits());
        waypointHits.clear();
        waypointHits.addAll(lastRenderResult.waypointHits());
        drawOverlays(graphics, font, x, y, width, height, dimension, snapshot, markers, waypoints, mouseX, mouseY);
        renderedOnce = true;
    }

    public ClickResult mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                && inside(mouseX, mouseY, lastMapX, lastMapY, lastMapWidth, lastMapHeight)) {
            actionMenuOpen = true;
            actionMenuX = (int) mouseX;
            actionMenuY = (int) mouseY;
            actionWorldX = screenToWorldX(mouseX, lastMapX, lastMapWidth);
            actionWorldZ = screenToWorldZ(mouseY, lastMapY, lastMapHeight);
            dragging = false;
            markInteractive();
            return ClickResult.HANDLED;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            actionMenuOpen = false;
            return ClickResult.NONE;
        }
        for (int i = overlayHitboxes.size() - 1; i >= 0; i--) {
            OverlayHitbox hitbox = overlayHitboxes.get(i);
            if (hitbox.contains(mouseX, mouseY)) {
                dragging = false;
                ClickResult result = handleOverlayHitbox(hitbox);
                actionMenuOpen = false;
                return result;
            }
        }
        for (HoloMapRenderer.WaypointHit hit : waypointHits) {
            if (inside(mouseX, mouseY, hit.x() - 7, hit.y() - 7, 14, 14)) {
                selectedWaypointId = hit.waypoint().id().toString();
                selectedMarkerId = "";
                actionMenuOpen = false;
                return ClickResult.HANDLED;
            }
        }
        for (HoloMapRenderer.MarkerHit hit : markerHits) {
            if (inside(mouseX, mouseY, hit.x() - 7, hit.y() - 7, 14, 14)) {
                selectedMarkerId = hit.marker().id().toString();
                selectedWaypointId = "";
                actionMenuOpen = false;
                return ClickResult.HANDLED;
            }
        }
        dragging = true;
        markInteractive();
        selectedMarkerId = "";
        selectedWaypointId = "";
        actionMenuOpen = false;
        return ClickResult.HANDLED;
    }

    public boolean mouseDragged(double dragX, double dragY, int width, int height) {
        if (!dragging) {
            return false;
        }
        centerX -= dragX / Math.max(0.25D, zoom);
        centerZ -= dragY / Math.max(0.25D, zoom);
        cameraReady = true;
        markInteractive();
        requestTerrain(false, width, height);
        return true;
    }

    public boolean mouseReleased(int width, int height) {
        if (!dragging) {
            return false;
        }
        dragging = false;
        markInteractive();
        requestTerrain(true, width, height);
        return true;
    }

    public boolean mouseScrolled(double scrollY, double mouseX, double mouseY, int x, int y, int width, int height) {
        if (indexDrawerOpen && inside(mouseX, mouseY, indexX, indexY, indexW, indexH)) {
            indexScrollOffset = clampInt(indexScrollOffset - (int) Math.round(scrollY * 24.0D), 0, indexMaxScroll);
            return true;
        }
        double worldX = screenToWorldX(mouseX, x, width);
        double worldZ = screenToWorldZ(mouseY, y, height);
        double before = zoom;
        zoom = clamp(zoom * (scrollY > 0.0D ? 1.2D : 0.82D), 0.25D, 8.0D);
        if (before == zoom) {
            return false;
        }
        centerX += worldX - screenToWorldX(mouseX, x, width);
        centerZ += worldZ - screenToWorldZ(mouseY, y, height);
        markInteractive();
        requestTerrain(true, width, height);
        return true;
    }

    public boolean keyPressed(int key, int width, int height) {
        double pan = Math.max(16.0D, 96.0D / Math.max(0.25D, zoom));
        if (key == GLFW.GLFW_KEY_C || key == GLFW.GLFW_KEY_HOME) {
            centerOnPlayer();
            markInteractive();
            requestTerrain(true, width, height);
            return true;
        }
        if (key == GLFW.GLFW_KEY_R) {
            requestSync(true);
            markInteractive();
            return true;
        }
        if (key == GLFW.GLFW_KEY_V) {
            toggleMarkers();
            markInteractive();
            return true;
        }
        if (key == GLFW.GLFW_KEY_F) {
            cycleFieldMode();
            markInteractive();
            return true;
        }
        if (key == GLFW.GLFW_KEY_W) {
            toggleWaypoints();
            markInteractive();
            return true;
        }
        if (key == GLFW.GLFW_KEY_I) {
            indexDrawerOpen = !indexDrawerOpen;
            markInteractive();
            return true;
        }
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
        markInteractive();
        requestTerrain(true, width, height);
        return true;
    }

    public void requestSync(boolean force) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        long now = minecraft.player.level().getGameTime();
        if (!force && now - lastSyncTick < SYNC_MIN_INTERVAL_TICKS) {
            return;
        }
        lastSyncTick = now;
        lastSyncDimension = minecraft.player.level().dimension().identifier().toString();
        lastSyncPlayerX = minecraft.player.getX();
        lastSyncPlayerZ = minecraft.player.getZ();
        EchoNetClientActions.sendServerboundAction(new HoloMapSyncRequestPacket());
    }

    public void centerOnPlayer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        centerX = minecraft.player.getX();
        centerZ = minecraft.player.getZ();
        cameraReady = true;
    }

    public void toggleMarkers() {
        showMarkers = !showMarkers;
    }

    public void cycleFieldMode() {
        fieldMode = fieldMode.next();
    }

    public void toggleWaypoints() {
        showWaypoints = !showWaypoints;
    }

    public boolean selectEntry(Identifier id) {
        EntryFocusTarget target = focusTarget(entries(), id);
        if (target == null) {
            return false;
        }
        selectedMarkerId = target.waypoint() ? "" : target.id().toString();
        selectedWaypointId = target.waypoint() ? target.id().toString() : "";
        centerX = target.x();
        centerZ = target.z();
        cameraReady = true;
        markInteractive();
        if (lastMapWidth > 0 && lastMapHeight > 0) {
            requestTerrain(true, lastMapWidth, lastMapHeight);
        }
        return true;
    }

    public List<Entry> entries() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return List.of();
        }
        String dimension = minecraft.player.level().dimension().identifier().toString();
        HoloMapSnapshotPacket snapshot = HoloMapClientState.snapshotForDimension(dimension);
        EntryCacheKey nextKey = new EntryCacheKey(dimension, snapshot.gameTime(), HoloMapWaypointClientState.revision(),
                selectedMarkerId, selectedWaypointId, showMarkers, fieldMode, showWaypoints,
                distanceBucket(minecraft.player.getX(), minecraft.player.getZ()));
        if (!nextKey.equals(entryCacheKey)) {
            entryCacheKey = nextKey;
            cachedEntries = groupedEntries(visibleMarkers(dimension), visibleWaypoints(dimension),
                    selectedMarkerId, selectedWaypointId, minecraft.player.getX(), minecraft.player.getZ());
        }
        return cachedEntries;
    }

    public static List<Entry> groupedEntriesForTests(List<HoloMapSnapshotPacket.MarkerData> markers,
            List<HoloMapWaypoint> waypoints, String selectedMarkerId, String selectedWaypointId) {
        return groupedEntriesForTests(markers, waypoints, selectedMarkerId, selectedWaypointId, 0.0D, 0.0D);
    }

    public static List<Entry> groupedEntriesForTests(List<HoloMapSnapshotPacket.MarkerData> markers,
            List<HoloMapWaypoint> waypoints, String selectedMarkerId, String selectedWaypointId,
            double playerX, double playerZ) {
        return groupedEntries(markers == null ? List.of() : markers, waypoints == null ? List.of() : waypoints,
                selectedMarkerId == null ? "" : selectedMarkerId, selectedWaypointId == null ? "" : selectedWaypointId,
                playerX, playerZ);
    }

    public static int entryCacheFingerprintForTests(String dimension, long snapshotGameTime, long waypointRevision,
            String selectedMarkerId, String selectedWaypointId, boolean showMarkers, boolean showWaypoints) {
        return entryCacheFingerprintForTests(dimension, snapshotGameTime, waypointRevision,
                selectedMarkerId, selectedWaypointId, showMarkers, HoloMapVisibility.FieldMode.AUTO_NEAR,
                showWaypoints);
    }

    public static int entryCacheFingerprintForTests(String dimension, long snapshotGameTime, long waypointRevision,
            String selectedMarkerId, String selectedWaypointId, boolean showMarkers,
            HoloMapVisibility.FieldMode fieldMode, boolean showWaypoints) {
        return new EntryCacheKey(dimension == null ? "" : dimension, snapshotGameTime, waypointRevision,
                selectedMarkerId == null ? "" : selectedMarkerId, selectedWaypointId == null ? "" : selectedWaypointId,
                showMarkers, fieldMode == null ? HoloMapVisibility.FieldMode.AUTO_NEAR : fieldMode,
                showWaypoints, 0).hashCode();
    }

    public static HoloMapRenderer.RenderBudget fullscreenBudgetForTests(boolean interactive) {
        return fullscreenBudget(interactive);
    }

    public static String controlLabelForTests(String control, boolean showMarkers,
            HoloMapVisibility.FieldMode fieldMode, boolean showWaypoints) {
        return controlLabel(control, showMarkers, fieldMode, showWaypoints);
    }

    public static EntryFocusTarget focusTargetForTests(List<Entry> entries, Identifier id) {
        return focusTarget(entries == null ? List.of() : entries, id);
    }

    public String controlLabel(String control) {
        return controlLabel(control, showMarkers, fieldMode, showWaypoints);
    }

    public boolean controlActive(String control) {
        return switch (control == null ? "" : control.toLowerCase(Locale.ROOT)) {
            case "markers" -> showMarkers;
            case "fields" -> fieldMode != HoloMapVisibility.FieldMode.OFF;
            case "waypoints" -> showWaypoints;
            default -> true;
        };
    }

    public boolean showMarkers() {
        return showMarkers;
    }

    public HoloMapVisibility.FieldMode fieldMode() {
        return fieldMode;
    }

    public synchronized Map<String, Object> nativeRouteState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("surface", "holomap");
        state.put("view", "fullscreen");
        state.put("cameraReady", cameraReady);
        state.put("renderedOnce", renderedOnce);
        state.put("dragging", dragging);
        state.put("showMarkers", showMarkers);
        state.put("fieldMode", fieldMode.name());
        state.put("showWaypoints", showWaypoints);
        state.put("indexDrawerOpen", indexDrawerOpen);
        state.put("selectedMarkerId", selectedMarkerId);
        state.put("selectedWaypointId", selectedWaypointId);
        state.put("zoom", zoom);
        state.put("centerX", centerX);
        state.put("centerZ", centerZ);
        return Map.copyOf(state);
    }

    public boolean showWaypoints() {
        return showWaypoints;
    }

    private void maybeRequestSync() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        long now = minecraft.player.level().getGameTime();
        String dimension = minecraft.player.level().dimension().identifier().toString();
        double dx = minecraft.player.getX() - lastSyncPlayerX;
        double dz = minecraft.player.getZ() - lastSyncPlayerZ;
        boolean moved = Double.isNaN(lastSyncPlayerX)
                || dx * dx + dz * dz >= SYNC_MOVE_DISTANCE_BLOCKS * SYNC_MOVE_DISTANCE_BLOCKS;
        boolean dimensionChanged = !dimension.equals(lastSyncDimension);
        boolean stale = now - lastSyncTick > SYNC_MAX_INTERVAL_TICKS;
        if (HoloMapClientState.snapshot().gameTime() == 0L || dimensionChanged || moved || stale) {
            requestSync(false);
        }
    }

    private void ensureCamera() {
        if (!cameraReady) {
            centerOnPlayer();
        }
    }

    private static HoloMapRenderer.RenderBudget fullscreenBudget(boolean interactive) {
        return interactive ? HoloMapRenderer.FULLSCREEN_INTERACTIVE_BUDGET : HoloMapRenderer.FULLSCREEN_BUDGET;
    }

    private void markInteractive() {
        lastInteractiveMillis = System.currentTimeMillis();
    }

    private boolean interactiveRendering() {
        return dragging
                || lastInteractiveMillis != Long.MIN_VALUE
                && System.currentTimeMillis() - lastInteractiveMillis <= INTERACTIVE_RENDER_GRACE_MS;
    }

    private void requestTerrain(boolean force, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || width <= 0 || height <= 0) {
            return;
        }
        long now = minecraft.player.level().getGameTime();
        int centerChunkX = Math.floorDiv((int) Math.floor(centerX), 16);
        int centerChunkZ = Math.floorDiv((int) Math.floor(centerZ), 16);
        int radius = visibleChunkRadius(width, height);
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
                minecraft.player.level().dimension().identifier().toString(), centerChunkX, centerChunkZ, radius));
    }

    private int visibleChunkRadius(int width, int height) {
        double blocksAcross = Math.max(width, height) / Math.max(0.25D, zoom);
        return Math.max(1, Math.min(32, (int) Math.ceil(blocksAcross / 32.0D) + 1));
    }

    private List<HoloMapSnapshotPacket.MarkerData> visibleMarkers(String dimension) {
        if (!showMarkers) {
            return List.of();
        }
        return HoloMapClientState.markersForDimension(dimension).stream()
                .filter(marker -> HoloMapVisibility.visibleInNormalView(marker.state()))
                .toList();
    }

    private List<HoloMapWaypoint> visibleWaypoints(String dimension) {
        if (!showWaypoints) {
            return List.of();
        }
        return HoloMapWaypointClientState.waypoints().stream()
                .filter(HoloMapWaypoint::visible)
                .filter(waypoint -> waypoint.inDimension(dimension))
                .toList();
    }

    private void drawOverlays(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            String dimension, HoloMapSnapshotPacket snapshot,
            List<HoloMapSnapshotPacket.MarkerData> markers, List<HoloMapWaypoint> waypoints,
            int mouseX, int mouseY) {
        if (font == null) {
            return;
        }
        List<Entry> entries = entries();
        drawStatusStrip(graphics, font, x, y, width, dimension, markers.size(), waypoints.size());
        drawPrimaryControls(graphics, font, x, y, width, height, mouseX, mouseY);
        drawModeChips(graphics, font, x, y, width, height, mouseX, mouseY);
        drawSelectedDetail(graphics, font, x, y, width, height, selectedEntry(entries));
        if (indexDrawerOpen) {
            drawIndexDrawer(graphics, font, x, y, width, height, entries, snapshot, mouseX, mouseY);
        } else {
            indexX = 0;
            indexY = 0;
            indexW = 0;
            indexH = 0;
            indexMaxScroll = 0;
            indexScrollOffset = 0;
        }
        if (actionMenuOpen) {
            drawActionMenu(graphics, font, x, y, width, height, dimension, mouseX, mouseY);
        }
    }

    private void drawStatusStrip(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, String dimension,
            int markers, int waypoints) {
        HoloMapTerrainClientState.DetailStats stats = HoloMapTerrainClientState.detailStats(dimension);
        int realChunks = HoloMapTerrainClientState.discoveredCount();
        String terrainText = HoloMapRenderer.terrainStatusLabel(lastRenderResult, realChunks);
        String text = "ATLAS " + terrainText + " | " + stats.compactLabel()
                + " | " + String.format(Locale.ROOT, "%.2fx", zoom)
                + " | " + markers + " P / " + waypoints + " WP";
        if (lastRenderResult.culledTerrainTiles() > 0) {
            text += " | terrain culled " + lastRenderResult.culledTerrainTiles();
        }
        int controlsWidth = primaryControlsWidth(width);
        int statusW = Math.min(Math.max(76, width - controlsWidth - 30), compact(width) ? 270 : 520);
        int statusX = x + OVERLAY_PAD;
        int statusY = y + OVERLAY_PAD;
        drawOverlayPanel(graphics, statusX, statusY, statusW, 22, OVERLAY_BORDER);
        graphics.text(font, trim(font, text, Math.max(24, statusW - 10)), statusX + 5, statusY + 7,
                HoloMapVisualStyle.ACCENT, false);
    }

    private void drawPrimaryControls(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            int mouseX, int mouseY) {
        for (OverlayHitbox hitbox : primaryControlHitboxes(x, y, width, height)) {
            drawOverlayButton(graphics, font, hitbox, controlOverlayLabel(hitbox.action(), width), mouseX, mouseY);
        }
    }

    private void drawModeChips(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            int mouseX, int mouseY) {
        for (OverlayHitbox hitbox : modeControlHitboxes(x, y, width, height)) {
            drawOverlayButton(graphics, font, hitbox, controlOverlayLabel(hitbox.action(), width), mouseX, mouseY);
        }
    }

    private void drawSelectedDetail(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            Entry selected) {
        if (selected == null) {
            return;
        }
        int bottomReserved = CHIP_HEIGHT + OVERLAY_PAD * 2 + 6;
        int detailW = Math.min(width - OVERLAY_PAD * 2, compact(width) ? width - OVERLAY_PAD * 2 : 320);
        if (indexDrawerOpen && !compact(width)) {
            int indexWidth = indexDrawerWidth(width);
            detailW = Math.min(detailW, Math.max(120, width - indexWidth - OVERLAY_PAD * 3));
        }
        if (detailW < 120) {
            return;
        }
        int detailH = 48;
        int detailX = x + OVERLAY_PAD;
        int detailY = y + height - bottomReserved - detailH;
        drawOverlayPanel(graphics, detailX, detailY, detailW, detailH,
                HoloMapVisualStyle.withAlpha(selected.color(), 0xAA));
        graphics.fill(detailX, detailY, detailX + 2, detailY + detailH, selected.color());
        graphics.text(font, trim(font, selected.title(), Math.max(8, detailW - 16)),
                detailX + 8, detailY + 7, HoloMapVisualStyle.TEXT, false);
        String meta = selected.kindLabel() + selected.countLabel() + " | "
                + selected.distanceLabel() + " | " + selected.coordinateLabel();
        graphics.text(font, trim(font, meta, Math.max(8, detailW - 16)),
                detailX + 8, detailY + 19, HoloMapVisualStyle.MUTED, false);
        String hint = selected.waypoint() ? "Waypoint selected" : selected.field() ? "Field source selected" : "Marker selected";
        graphics.text(font, trim(font, hint, Math.max(8, detailW - 16)),
                detailX + 8, detailY + 31, HoloMapVisualStyle.withAlpha(selected.color(), 0xDD), false);
    }

    private void drawActionMenu(GuiGraphicsExtractor graphics, Font font, int mapX, int mapY, int mapWidth,
            int mapHeight, String dimension, int mouseX, int mouseY) {
        int chunkX = actionChunkX();
        int chunkZ = actionChunkZ();
        boolean renderableChunk = HoloMapTerrainClientState.hasRenderableTile(dimension, chunkX, chunkZ);
        List<HoloMapChunkMenuAction> actions = renderableChunk
                ? HoloMapClientChunkActions.actions(dimension, chunkX, chunkZ)
                : List.of();
        int rows = Math.max(1, actions.size());
        int w = 150;
        int h = 28 + rows * 20;
        int x = Math.max(mapX + OVERLAY_PAD, Math.min(actionMenuX, mapX + mapWidth - w - OVERLAY_PAD));
        int y = Math.max(mapY + OVERLAY_PAD, Math.min(actionMenuY, mapY + mapHeight - h - OVERLAY_PAD));
        drawOverlayPanel(graphics, x, y, w, h, HoloMapVisualStyle.ACCENT);
        graphics.text(font, "CHUNK " + chunkX + ", " + chunkZ, x + 8, y + 8,
                HoloMapVisualStyle.ACCENT, false);
        int rowY = y + 24;
        if (!renderableChunk) {
            drawActionMenuButton(graphics, font, x + 8, rowY, w - 16, "PENDING SCAN",
                    null, false, HoloMapVisualStyle.MUTED, mouseX, mouseY);
            return;
        }
        if (actions.isEmpty()) {
            drawActionMenuButton(graphics, font, x + 8, rowY, w - 16, "NO CLAIM ACTION",
                    null, false, HoloMapVisualStyle.MUTED, mouseX, mouseY);
            return;
        }
        for (HoloMapChunkMenuAction action : actions) {
            drawActionMenuButton(graphics, font, x + 8, rowY, w - 16, action.label(),
                    action.menuId(), action.enabled(), action.color(), mouseX, mouseY);
            rowY += 20;
        }
    }

    private void drawActionMenuButton(GuiGraphicsExtractor graphics, Font font, int x, int y, int width,
            String label, Identifier menuId, boolean enabled, int color, int mouseX, int mouseY) {
        boolean hovered = enabled && inside(mouseX, mouseY, x, y, width, 16);
        int border = hovered ? HoloMapVisualStyle.TEXT : enabled ? color : HoloMapVisualStyle.MUTED;
        int bg = hovered ? OVERLAY_HOVER : OVERLAY_BG;
        graphics.fill(x, y, x + width, y + 16, bg);
        graphics.outline(x, y, width, 16, HoloMapVisualStyle.withAlpha(border, enabled ? 0xCC : 0x66));
        graphics.text(font, trim(font, label, Math.max(8, width - 8)), x + 4, y + 5,
                enabled ? border : HoloMapVisualStyle.MUTED, false);
        if (enabled && menuId != null) {
            overlayHitboxes.add(new OverlayHitbox(OverlayAction.CHUNK_ACTION, menuId, x, y, width, 16));
        }
    }

    private void drawIndexDrawer(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            List<Entry> entries, HoloMapSnapshotPacket snapshot, int mouseX, int mouseY) {
        indexW = Math.min(width - OVERLAY_PAD * 2, compact(width) ? width - OVERLAY_PAD * 2 : 286);
        indexH = Math.max(96, height - 76);
        indexX = x + width - indexW - OVERLAY_PAD;
        indexY = y + 38;
        if (indexY + indexH > y + height - 32) {
            indexH = Math.max(80, y + height - 32 - indexY);
        }
        drawOverlayPanel(graphics, indexX, indexY, indexW, indexH, OVERLAY_BORDER);
        graphics.text(font, "SIGNAL INDEX", indexX + 8, indexY + 8, HoloMapVisualStyle.ACCENT, false);
        String meta = snapshot.statusLine();
        graphics.text(font, trim(font, meta, Math.max(8, indexW - 16)), indexX + 8, indexY + 20,
                HoloMapVisualStyle.MUTED, false);

        int listX = indexX + 6;
        int listY = indexY + 36;
        int listW = Math.max(1, indexW - 12);
        int listH = Math.max(1, indexH - 42);
        int contentH = indexContentHeight(entries);
        indexMaxScroll = Math.max(0, contentH - listH);
        indexScrollOffset = clampInt(indexScrollOffset, 0, indexMaxScroll);
        if (entries.isEmpty()) {
            graphics.text(font, "No visible signals.", listX + 4, listY + 4, HoloMapVisualStyle.MUTED, false);
            return;
        }
        graphics.enableScissor(listX, listY, listX + listW, listY + listH);
        try {
            int rowY = listY - indexScrollOffset;
            for (Entry entry : entries) {
                int rowH = indexRowHeight(entry);
                if (rowY + rowH >= listY && rowY <= listY + listH) {
                    drawIndexEntry(graphics, font, entry, listX, rowY, listW, rowH, mouseX, mouseY);
                }
                rowY += rowH;
            }
        } finally {
            graphics.disableScissor();
        }
        drawIndexScrollbar(graphics, listX, listY, listW, listH);
    }

    private void drawIndexEntry(GuiGraphicsExtractor graphics, Font font, Entry entry,
            int x, int y, int width, int height, int mouseX, int mouseY) {
        if (entry.header()) {
            graphics.fill(x + 3, y + 9, x + width - 4, y + 10,
                    HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.ACCENT, 0x38));
            graphics.text(font, trim(font, entry.title().toUpperCase(Locale.ROOT), Math.max(8, width - 12)),
                    x + 5, y + 2, HoloMapVisualStyle.MUTED, false);
            return;
        }
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        int background = entry.selected() ? OVERLAY_SELECTED : hovered ? OVERLAY_HOVER : 0x55102430;
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, background);
        graphics.fill(x + 6, y + 5, x + 10, y + height - 5, entry.color());
        int textX = x + 15;
        String title = entry.prefix() + " " + entry.title() + entry.countLabel();
        graphics.text(font, trim(font, title, Math.max(8, width - 34)), textX, y + 3,
                entry.selected() ? HoloMapVisualStyle.TEXT : entry.color(), false);
        String meta = entry.kindLabel() + " | " + entry.distanceLabel() + " | " + entry.coordinateLabel();
        graphics.text(font, trim(font, meta, Math.max(8, width - 34)), textX, y + 13,
                HoloMapVisualStyle.MUTED, false);
        overlayHitboxes.add(new OverlayHitbox(OverlayAction.SELECT_ENTRY, entry.id(), x, y, width, height));
    }

    private void drawIndexScrollbar(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        if (indexMaxScroll <= 0 || height <= 8) {
            return;
        }
        int railX = x + width - 3;
        int railH = Math.max(12, height - 8);
        int thumbH = Math.max(12, railH * height / Math.max(height + indexMaxScroll, 1));
        int thumbY = y + 4 + (railH - thumbH) * indexScrollOffset / Math.max(1, indexMaxScroll);
        graphics.fill(railX, y + 4, railX + 1, y + 4 + railH,
                HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.ACCENT, 0x44));
        graphics.fill(railX - 1, thumbY, railX + 2, thumbY + thumbH,
                HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.ACCENT, 0xAA));
    }

    private void drawOverlayButton(GuiGraphicsExtractor graphics, Font font, OverlayHitbox hitbox, String label,
            int mouseX, int mouseY) {
        overlayHitboxes.add(hitbox);
        boolean hovered = hitbox.contains(mouseX, mouseY);
        boolean active = switch (hitbox.action()) {
            case TOGGLE_MARKERS -> showMarkers;
            case CYCLE_FIELDS -> fieldMode != HoloMapVisibility.FieldMode.OFF;
            case TOGGLE_WAYPOINTS -> showWaypoints;
            case TOGGLE_INDEX -> indexDrawerOpen;
            default -> false;
        };
        int border = active || hovered ? HoloMapVisualStyle.ACCENT : OVERLAY_BORDER;
        int bg = hovered ? OVERLAY_HOVER : active ? OVERLAY_SELECTED : OVERLAY_BG;
        graphics.fill(hitbox.x(), hitbox.y(), hitbox.right(), hitbox.bottom(), bg);
        graphics.outline(hitbox.x(), hitbox.y(), hitbox.width(), hitbox.height(), border);
        String trimmed = trim(font, label, Math.max(8, hitbox.width() - 8));
        int labelX = hitbox.x() + Math.max(4, (hitbox.width() - font.width(trimmed)) / 2);
        int labelY = hitbox.y() + Math.max(4, (hitbox.height() - 8) / 2);
        graphics.text(font, trimmed, labelX, labelY, active ? HoloMapVisualStyle.TEXT : HoloMapVisualStyle.MUTED, false);
        if (active) {
            graphics.fill(hitbox.x() + 4, hitbox.bottom() - 2, hitbox.right() - 4, hitbox.bottom() - 1,
                    HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.ACCENT, 0xAA));
        }
    }

    private static void drawOverlayPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            int borderColor) {
        graphics.fill(x, y, x + width, y + height, OVERLAY_BG_SOFT);
        graphics.outline(x, y, width, height, borderColor);
    }

    private ClickResult handleOverlayHitbox(OverlayHitbox hitbox) {
        switch (hitbox.action()) {
            case CENTER -> {
                centerOnPlayer();
                requestTerrain(true, lastMapWidth, lastMapHeight);
            }
            case SYNC -> requestSync(true);
            case CLOSE -> {
                return ClickResult.CLOSE;
            }
            case TOGGLE_MARKERS -> toggleMarkers();
            case CYCLE_FIELDS -> cycleFieldMode();
            case TOGGLE_WAYPOINTS -> toggleWaypoints();
            case TOGGLE_INDEX -> indexDrawerOpen = !indexDrawerOpen;
            case SELECT_ENTRY -> selectEntry(hitbox.id());
            case CHUNK_ACTION -> sendChunkAction(hitbox.id());
        }
        markInteractive();
        return ClickResult.HANDLED;
    }

    private void sendChunkAction(Identifier menuId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || menuId == null) {
            return;
        }
        String dimension = minecraft.player.level().dimension().identifier().toString();
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

    public static String overlayActionForTests(int x, int y, int width, int height, int mouseX, int mouseY) {
        ArrayList<OverlayHitbox> hitboxes = new ArrayList<>();
        hitboxes.addAll(primaryControlHitboxes(x, y, width, height));
        hitboxes.addAll(modeControlHitboxes(x, y, width, height));
        for (int i = hitboxes.size() - 1; i >= 0; i--) {
            OverlayHitbox hitbox = hitboxes.get(i);
            if (hitbox.contains(mouseX, mouseY)) {
                return hitbox.action().name();
            }
        }
        return "";
    }

    public static Entry selectedEntryForTests(List<Entry> entries) {
        return selectedEntry(entries == null ? List.of() : entries);
    }

    private static Entry selectedEntry(List<Entry> entries) {
        for (Entry entry : entries == null ? List.<Entry>of() : entries) {
            if (!entry.header() && entry.selected()) {
                return entry;
            }
        }
        return null;
    }

    private static int indexContentHeight(List<Entry> entries) {
        int height = 0;
        for (Entry entry : entries == null ? List.<Entry>of() : entries) {
            height += indexRowHeight(entry);
        }
        return height;
    }

    private static int indexRowHeight(Entry entry) {
        return entry != null && entry.header() ? INDEX_HEADER_HEIGHT : INDEX_ROW_HEIGHT;
    }

    private static int primaryControlsWidth(int width) {
        List<OverlayHitbox> hitboxes = primaryControlHitboxes(0, 0, width, 1);
        if (hitboxes.isEmpty()) {
            return 0;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (OverlayHitbox hitbox : hitboxes) {
            minX = Math.min(minX, hitbox.x());
            maxX = Math.max(maxX, hitbox.right());
        }
        return Math.max(0, maxX - minX);
    }

    private static List<OverlayHitbox> primaryControlHitboxes(int x, int y, int width, int height) {
        if (width <= OVERLAY_PAD * 2 + 24) {
            return List.of();
        }
        boolean compact = compact(width);
        int gap = 4;
        int closeW = compact ? 22 : 28;
        int syncW = compact ? 32 : 44;
        int centerW = compact ? 34 : 58;
        int buttonY = y + OVERLAY_PAD;
        int right = x + width - OVERLAY_PAD;
        ArrayList<OverlayHitbox> hitboxes = new ArrayList<>(3);
        int closeX = right - closeW;
        hitboxes.add(new OverlayHitbox(OverlayAction.CLOSE, null, closeX, buttonY, closeW, CONTROL_HEIGHT));
        int syncX = closeX - gap - syncW;
        if (syncX >= x + OVERLAY_PAD) {
            hitboxes.add(new OverlayHitbox(OverlayAction.SYNC, null, syncX, buttonY, syncW, CONTROL_HEIGHT));
        }
        int centerX = syncX - gap - centerW;
        if (centerX >= x + OVERLAY_PAD) {
            hitboxes.add(new OverlayHitbox(OverlayAction.CENTER, null, centerX, buttonY, centerW, CONTROL_HEIGHT));
        }
        return List.copyOf(hitboxes);
    }

    private static List<OverlayHitbox> modeControlHitboxes(int x, int y, int width, int height) {
        if (width <= OVERLAY_PAD * 2 + 48 || height <= OVERLAY_PAD * 2 + CHIP_HEIGHT) {
            return List.of();
        }
        boolean compact = compact(width);
        int[] widths = compact ? new int[] {54, 54, 56, 46} : new int[] {88, 88, 104, 56};
        OverlayAction[] actions = new OverlayAction[] {
                OverlayAction.TOGGLE_MARKERS,
                OverlayAction.CYCLE_FIELDS,
                OverlayAction.TOGGLE_WAYPOINTS,
                OverlayAction.TOGGLE_INDEX
        };
        int chipX = x + OVERLAY_PAD;
        int chipY = y + height - OVERLAY_PAD - CHIP_HEIGHT;
        int gap = 4;
        ArrayList<OverlayHitbox> hitboxes = new ArrayList<>(actions.length);
        for (int i = 0; i < actions.length; i++) {
            int chipW = Math.min(widths[i], Math.max(24, x + width - OVERLAY_PAD - chipX));
            if (chipW < 24) {
                break;
            }
            hitboxes.add(new OverlayHitbox(actions[i], null, chipX, chipY, chipW, CHIP_HEIGHT));
            chipX += chipW + gap;
        }
        return List.copyOf(hitboxes);
    }

    private String controlOverlayLabel(OverlayAction action, int width) {
        boolean compact = compact(width);
        return switch (action) {
            case CENTER -> compact ? "C" : "CENTER";
            case SYNC -> compact ? "R" : "SYNC";
            case CLOSE -> compact ? "X" : "CLOSE";
            case TOGGLE_MARKERS -> compact ? showMarkers ? "PIN ON" : "PIN OFF" : controlLabel("markers");
            case CYCLE_FIELDS -> compact ? "F " + fieldMode.label() : controlLabel("fields");
            case TOGGLE_WAYPOINTS -> compact ? showWaypoints ? "WP ON" : "WP OFF" : controlLabel("waypoints");
            case TOGGLE_INDEX -> compact ? "IDX" : indexDrawerOpen ? "INDEX ON" : "INDEX";
            case CHUNK_ACTION -> "";
            case SELECT_ENTRY -> "";
        };
    }

    private static int indexDrawerWidth(int width) {
        return Math.min(width - OVERLAY_PAD * 2, compact(width) ? width - OVERLAY_PAD * 2 : 286);
    }

    private static boolean compact(int width) {
        return width < 420;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double screenToWorldX(double screenX, int x, int width) {
        return centerX + (screenX - (x + width / 2.0D)) / Math.max(0.25D, zoom);
    }

    private double screenToWorldZ(double screenY, int y, int height) {
        return centerZ + (screenY - (y + height / 2.0D)) / Math.max(0.25D, zoom);
    }

    private int actionChunkX() {
        return Math.floorDiv((int) Math.floor(actionWorldX), 16);
    }

    private int actionChunkZ() {
        return Math.floorDiv((int) Math.floor(actionWorldZ), 16);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String trim(Font font, String text, int maxWidth) {
        if (font == null || text == null || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String value = text;
        while (!value.isEmpty() && font.width(value + "...") > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "...";
    }

    private static List<Entry> groupedEntries(List<HoloMapSnapshotPacket.MarkerData> markers,
            List<HoloMapWaypoint> waypoints, String selectedMarkerId, String selectedWaypointId,
            double playerX, double playerZ) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (HoloMapWaypoint waypoint : waypoints) {
            entries.add(Entry.waypoint(waypoint, distance(playerX, playerZ, waypoint.x(), waypoint.z()),
                    waypoint.id().toString().equals(selectedWaypointId)));
        }
        Map<String, EntryGroup> groups = new LinkedHashMap<>();
        for (HoloMapSnapshotPacket.MarkerData marker : markers) {
            String section = section(marker);
            String key = section + "|" + marker.kind() + "|" + marker.title().toLowerCase(Locale.ROOT);
            EntryGroup group = groups.computeIfAbsent(key, ignored -> new EntryGroup(section, marker,
                    distance(playerX, playerZ, marker.x(), marker.z())));
            group.add(marker, distance(playerX, playerZ, marker.x(), marker.z()),
                    marker.id().toString().equals(selectedMarkerId));
        }
        for (EntryGroup group : groups.values()) {
            entries.add(group.entry());
        }
        entries.sort(Comparator.comparingInt((Entry entry) -> sectionOrder(entry.section()))
                .thenComparing(Entry::selected, Comparator.reverseOrder())
                .thenComparingDouble(Entry::distance)
                .thenComparing(Entry::title, String.CASE_INSENSITIVE_ORDER));
        ArrayList<Entry> withHeaders = new ArrayList<>();
        String lastSection = "";
        for (Entry entry : entries) {
            if (!entry.section().equals(lastSection)) {
                lastSection = entry.section();
                withHeaders.add(Entry.header(lastSection));
            }
            withHeaders.add(entry);
        }
        return List.copyOf(withHeaders);
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

    private static String controlLabel(String control, boolean showMarkers,
            HoloMapVisibility.FieldMode fieldMode, boolean showWaypoints) {
        return switch (control == null ? "" : control.toLowerCase(Locale.ROOT)) {
            case "markers" -> showMarkers ? "MARKERS ON" : "MARKERS OFF";
            case "fields" -> "FIELDS " + (fieldMode == null ? HoloMapVisibility.FieldMode.AUTO_NEAR : fieldMode).label();
            case "waypoints" -> showWaypoints ? "WAYPOINTS ON" : "WAYPOINTS OFF";
            default -> "";
        };
    }

    private static EntryFocusTarget focusTarget(List<Entry> entries, Identifier id) {
        if (id == null) {
            return null;
        }
        for (Entry entry : entries) {
            if (!entry.header() && id.equals(entry.id())) {
                return new EntryFocusTarget(entry.id(), entry.waypoint(), entry.x(), entry.z());
            }
        }
        return null;
    }

    private static double distance(double x0, double z0, double x1, double z1) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static int distanceBucket(double x, double z) {
        return (int) Math.floor(x / 64.0D) * 31 + (int) Math.floor(z / 64.0D);
    }

    private static String markerKindLabel(IMapMarker.MarkerKind kind, boolean field) {
        if (field) {
            return "Field";
        }
        return switch (kind == null ? IMapMarker.MarkerKind.GENERIC : kind) {
            case MISSION -> "Mission";
            case ROUTE -> "Route";
            case HAZARD -> "Hazard";
            case REGION -> "Region";
            case CRASH_SITE -> "Crash";
            case BASE_OUTPOST -> "Base";
            case ORBITAL_SCAN -> "Scan";
            case NEXUS_ANOMALY -> "Nexus";
            case DRONE_SCAN -> "Drone";
            case GENERIC, STRUCTURE, FACTION -> "Marker";
        };
    }

    private static final class EntryGroup {
        private final String section;
        private Identifier id;
        private String title;
        private String dimension;
        private double x;
        private double z;
        private double distance;
        private int color;
        private int count;
        private boolean selected;
        private boolean field;
        private String kindLabel;

        private EntryGroup(String section, HoloMapSnapshotPacket.MarkerData marker, double distance) {
            this.section = section;
            this.id = marker.id();
            this.title = marker.title();
            this.dimension = marker.dimension();
            this.x = marker.x();
            this.z = marker.z();
            this.distance = distance;
            this.color = HoloMapVisualStyle.markerColor(null, marker);
            this.field = fieldMarker(marker);
            this.kindLabel = markerKindLabel(marker.kind(), field);
        }

        private void add(HoloMapSnapshotPacket.MarkerData marker, double markerDistance, boolean markerSelected) {
            count++;
            boolean markerField = fieldMarker(marker);
            if (markerSelected || !selected && markerDistance < distance) {
                id = marker.id();
                title = marker.title();
                dimension = marker.dimension();
                x = marker.x();
                z = marker.z();
                distance = markerDistance;
                color = HoloMapVisualStyle.markerColor(null, marker);
                kindLabel = markerKindLabel(marker.kind(), markerField);
            }
            selected |= markerSelected;
            field |= markerField;
            if (field) {
                kindLabel = "Field";
            }
        }

        private Entry entry() {
            return new Entry(id, title, section, color, false, field, count, selected, false,
                    dimension, x, z, distance, kindLabel);
        }

        private static boolean fieldMarker(HoloMapSnapshotPacket.MarkerData marker) {
            return marker.radius() > 0.0F && HoloMapVisibility.markerCanGenerateField(marker.kind());
        }
    }

    public record Entry(Identifier id, String title, String section, int color, boolean waypoint,
            boolean field, int count, boolean selected, boolean header,
            String dimension, double x, double z, double distance, String kindLabel) {
        private static Entry waypoint(HoloMapWaypoint waypoint, double distance, boolean selected) {
            return new Entry(waypoint.id(), waypoint.title(), "Waypoints", waypoint.color(),
                    true, false, 1, selected, false, waypoint.dimension(), waypoint.x(), waypoint.z(),
                    distance, waypoint.isDeathpoint() ? "Deathpoint" : "Waypoint");
        }

        private static Entry header(String section) {
            return new Entry(null, section, section, HoloMapVisualStyle.MUTED, false, false, 0, false, true,
                    "", 0.0D, 0.0D, Double.NaN, section);
        }

        public String prefix() {
            return waypoint ? "W" : field ? "F" : "P";
        }

        public String countLabel() {
            return count > 1 ? " x" + count : "";
        }

        public String distanceLabel() {
            if (Double.isNaN(distance) || Double.isInfinite(distance)) {
                return "";
            }
            if (distance >= 1000.0D) {
                return String.format(Locale.ROOT, "%.1fkm", distance / 1000.0D);
            }
            return Math.max(0, (int) Math.round(distance)) + "m";
        }

        public String coordinateLabel() {
            return (int) Math.round(x) + ", " + (int) Math.round(z);
        }
    }

    public record EntryFocusTarget(Identifier id, boolean waypoint, double x, double z) {
    }

    public enum ClickResult {
        NONE,
        HANDLED,
        CLOSE;

        public boolean handled() {
            return this != NONE;
        }
    }

    private enum OverlayAction {
        CENTER,
        SYNC,
        CLOSE,
        TOGGLE_MARKERS,
        CYCLE_FIELDS,
        TOGGLE_WAYPOINTS,
        TOGGLE_INDEX,
        CHUNK_ACTION,
        SELECT_ENTRY
    }

    private record OverlayHitbox(OverlayAction action, Identifier id, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return inside(mouseX, mouseY, x, y, width, height);
        }

        private int right() {
            return x + width;
        }

        private int bottom() {
            return y + height;
        }
    }

    private record EntryCacheKey(String dimension, long snapshotGameTime, long waypointRevision,
            String selectedMarkerId, String selectedWaypointId, boolean showMarkers,
            HoloMapVisibility.FieldMode fieldMode, boolean showWaypoints, int distanceBucket) {
    }
}
