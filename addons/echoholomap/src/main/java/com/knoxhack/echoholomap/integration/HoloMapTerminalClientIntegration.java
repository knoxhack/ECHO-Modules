package com.knoxhack.echoholomap.integration;

import com.echoplatform.echocore.api.IMapMarker;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.HoloMapIds;
import com.knoxhack.echoholomap.api.HoloMapPrecision;
import com.knoxhack.echoholomap.client.HoloMapChunkMenuAction;
import com.knoxhack.echoholomap.client.HoloMapClientChunkActions;
import com.knoxhack.echoholomap.client.HoloMapGlyphRenderer;
import com.knoxhack.echoholomap.client.HoloMapLocalWaypointStore;
import com.knoxhack.echoholomap.client.HoloMapRenderer;
import com.knoxhack.echoholomap.client.HoloMapViewState;
import com.knoxhack.echoholomap.client.HoloMapVisualStyle;
import com.knoxhack.echoholomap.map.HoloMapVisualPriority;
import com.knoxhack.echoholomap.map.HoloMapTerrainTile;
import com.knoxhack.echoholomap.map.HoloMapVisibility;
import com.knoxhack.echoholomap.network.HoloMapClientState;
import com.knoxhack.echoholomap.network.HoloMapChunkActionPacket;
import com.knoxhack.echoholomap.network.HoloMapSnapshotPacket;
import com.knoxhack.echoholomap.network.HoloMapTerrainClientState;
import com.knoxhack.echoholomap.network.HoloMapTileRequestPacket;
import com.knoxhack.echoholomap.network.HoloMapWaypointActionPacket;
import com.knoxhack.echoholomap.network.HoloMapWaypointClientState;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint.Scope;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class HoloMapTerminalClientIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int ACCENT = 0xFF38DFF4;

    private HoloMapTerminalClientIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalTab tab = new HoloMapTab();
        TerminalTabRegistry.register(tab);
        TerminalNavigationProfiles.register(tab.descriptor().id(), TerminalNavigationProfile.intel(185));
    }

    private enum StateFilter {
        ALL("ALL"),
        OPEN("OPEN"),
        LOCKED("LOCKED"),
        CHECKED("DONE");

        private final String label;

        StateFilter(String label) {
            this.label = label;
        }

        private StateFilter next() {
            StateFilter[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private enum SortMode {
        NEAREST("NEAR"),
        NAME("NAME"),
        LAYER("LAYER"),
        STATE("STATE");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }

        private SortMode next() {
            SortMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private static final class HoloMapTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor = new TerminalTabDescriptor(HoloMapIds.TAB, "HOLOMAP", 185, ACCENT);
        private final TerminalTabChrome chrome = TerminalTabChrome.of("HoloMap", TerminalTabChrome.GROUP_FIELD,
                "HM", "World telemetry map", 185);
        private final HoloMapRenderer renderer = new HoloMapRenderer();
        private final Map<Identifier, Boolean> layerEnabled = new LinkedHashMap<>();
        private final List<Button> buttons = new ArrayList<>();
        private final List<MarkerHit> markerHits = new ArrayList<>();
        private final List<WaypointHit> waypointHits = new ArrayList<>();
        private final List<ListEntryHit> listHits = new ArrayList<>();
        private StateFilter stateFilter = StateFilter.ALL;
        private SortMode sortMode = SortMode.NEAREST;
        private String selectedMarkerId = "";
        private String selectedWaypointId = "";
        private String searchText = "";
        private boolean searchFocused;
        private boolean showWaypoints = true;
        private int detailMode = 1;
        private long lastRefreshTick = -200L;
        private double centerX;
        private double centerZ;
        private double zoom = 1.35D;
        private boolean cameraReady;
        private boolean draggingMap;
        private long lastClickMs;
        private double lastClickX;
        private double lastClickY;
        private boolean actionMenuOpen;
        private int actionMenuX;
        private int actionMenuY;
        private double actionWorldX;
        private double actionWorldZ;
        private long lastTerrainRequestTick = -200L;
        private boolean renderedOnce;
        private HoloMapRenderer.RenderResult lastRenderResult =
                new HoloMapRenderer.RenderResult(0, 0, 0, 0, 0, 0, List.of(), List.of(), false);
        private int lastRequestChunkX = Integer.MIN_VALUE;
        private int lastRequestChunkZ = Integer.MIN_VALUE;
        private int lastRequestRadius = -1;
        private int lastMapX;
        private int lastMapY;
        private int lastMapW;
        private int lastMapH;
        private int lastSearchX;
        private int lastSearchY;
        private int lastSearchW;
        private int lastSearchH;

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void onSelected(TerminalRenderContext context) {
            HoloMapLocalWaypointStore.ensureLoaded();
            renderedOnce = false;
            requestRefresh(context);
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                float partialTick) {
            maybeRefresh(context);
            HoloMapLocalWaypointStore.ensureLoaded();
            buttons.clear();
            markerHits.clear();
            waypointHits.clear();
            listHits.clear();

            String dimension = context.player() == null
                    ? "minecraft:overworld"
                    : context.player().level().dimension().identifier().toString();
            HoloMapSnapshotPacket snapshot = HoloMapClientState.snapshotForDimension(dimension);
            syncLayers(snapshot.layers());
            int x = context.contentX() + 12;
            int y = context.contentY() + 10 - context.scrollY();
            int w = context.contentWidth() - 24;
            y = TerminalUi.sectionHeader(context, graphics, "HOLOMAP", "COMMAND MAP", x, y, w, ACCENT);

            int controlH = 92;
            TerminalUi.flatHudPanel(context, graphics, x, y, w, controlH, ACCENT);
            TerminalUi.line(context, graphics, snapshot.statusLine(), x + 12, y + 10, w - 192,
                    TerminalUi.accent(context));
            button(graphics, context, "CENTER", x + w - 230, y + 8, 58, true, mouseX, mouseY, ButtonKind.CENTER, "");
            button(graphics, context, "SYNC", x + w - 166, y + 8, 70, true, mouseX, mouseY, ButtonKind.REFRESH, "");
            button(graphics, context, "TEST", x + w - 88, y + 8, 68, true, mouseX, mouseY, ButtonKind.TEST, "");
            button(graphics, context, "STATE " + stateFilter.label, x + w - 166, y + 34, 104, true,
                    mouseX, mouseY, ButtonKind.STATE, "");
            button(graphics, context, detailMode == 0 ? "LOW" : detailMode == 1 ? "MED" : "HIGH",
                    x + w - 54, y + 34, 34, true, mouseX, mouseY, ButtonKind.DETAIL, "");
            button(graphics, context, showWaypoints ? "WP ON" : "WP OFF", x + w - 166, y + 58, 70, true,
                    mouseX, mouseY, ButtonKind.WAYPOINTS, "");
            button(graphics, context, "SORT " + sortMode.label, x + w - 88, y + 58, 68, true,
                    mouseX, mouseY, ButtonKind.SORT, "");
            drawLayerButtons(context, graphics, snapshot.layers(), x + 12, y + 34, Math.max(120, w - 194),
                    mouseX, mouseY);
            drawSearchBox(context, graphics, x + 12, y + 58, Math.max(140, Math.min(310, w - 202)), mouseX, mouseY);

            int bodyY = y + controlH + 12;
            int bodyH = Math.max(330, context.contentHeight() - 116);
            int detailW = Math.min(220, Math.max(174, w / 3));
            int mapW = Math.max(260, w - detailW - 12);
            List<HoloMapSnapshotPacket.MarkerData> visible = visibleMarkers(dimension);
            List<HoloMapWaypoint> waypoints = visibleWaypoints(context);
            drawMap(context, graphics, snapshot, visible, waypoints, x, bodyY, mapW, bodyH, mouseX, mouseY,
                    snapshot.gameTime());
            drawDetailPanel(context, graphics, snapshot, visible, waypoints, x + mapW + 12, bodyY, detailW, bodyH,
                    mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            if (button == 1 && TerminalUi.inside(mouseX, mouseY, lastMapX, lastMapY, lastMapW, lastMapH)) {
                actionMenuOpen = true;
                actionMenuX = (int) mouseX;
                actionMenuY = (int) mouseY;
                actionWorldX = screenToWorldX(mouseX);
                actionWorldZ = screenToWorldZ(mouseY);
                searchFocused = false;
                return true;
            }
            if (button != 0) {
                actionMenuOpen = false;
                return false;
            }
            if (TerminalUi.inside(mouseX, mouseY, lastSearchX, lastSearchY, lastSearchW, lastSearchH)) {
                searchFocused = true;
                actionMenuOpen = false;
                return true;
            }
            for (Button hit : buttons) {
                if (!TerminalUi.inside(mouseX, mouseY, hit.x(), hit.y(), hit.w(), hit.h())) {
                    continue;
                }
                switch (hit.kind()) {
                    case CENTER -> centerOnPlayer(context);
                    case REFRESH -> requestRefresh(context);
                    case TEST -> context.sendAction(HoloMapIds.TAB, HoloMapIds.TEST_MARKER_ACTION, "drones_scans");
                    case STATE -> stateFilter = stateFilter.next();
                    case DETAIL -> detailMode = (detailMode + 1) % 3;
                    case LAYER -> layerEnabled.put(hit.layerId(), !layerEnabled.getOrDefault(hit.layerId(), true));
                    case WAYPOINTS -> showWaypoints = !showWaypoints;
                    case SORT -> sortMode = sortMode.next();
                    case SEARCH_CLEAR -> searchText = "";
                    case MENU_LOCAL -> createWaypoint(context, Scope.LOCAL);
                    case MENU_PERSONAL -> createWaypoint(context, Scope.PERSONAL);
                    case MENU_SHARED -> createWaypoint(context, Scope.SHARED);
                    case MENU_COPY -> copyActionCoordinates(context);
                    case MENU_MOVE_SELECTED -> moveSelectedWaypointToAction(context);
                    case WAYPOINT_DELETE -> deleteSelectedWaypoint();
                    case CHUNK_ACTION -> sendChunkAction(context, hit.layerId());
                }
                if (hit.kind() != ButtonKind.SEARCH_CLEAR) {
                    searchFocused = false;
                }
                actionMenuOpen = false;
                return true;
            }
            for (ListEntryHit hit : listHits) {
                if (TerminalUi.inside(mouseX, mouseY, hit.x(), hit.y(), hit.w(), hit.h())) {
                    focusListEntry(context, hit.entry());
                    searchFocused = false;
                    actionMenuOpen = false;
                    return true;
                }
            }
            for (WaypointHit hit : waypointHits) {
                if (TerminalUi.inside(mouseX, mouseY, hit.x() - 6, hit.y() - 6, 12, 12)) {
                    selectedWaypointId = hit.waypoint().id().toString();
                    selectedMarkerId = "";
                    context.playCommandSound();
                    return true;
                }
            }
            for (MarkerHit hit : markerHits) {
                if (TerminalUi.inside(mouseX, mouseY, hit.x() - 5, hit.y() - 5, 10, 10)) {
                    selectedMarkerId = hit.marker().id().toString();
                    selectedWaypointId = "";
                    context.playCommandSound();
                    return true;
                }
            }
            if (TerminalUi.inside(mouseX, mouseY, lastMapX, lastMapY, lastMapW, lastMapH)) {
                long now = System.currentTimeMillis();
                double dx = mouseX - lastClickX;
                double dy = mouseY - lastClickY;
                if (now - lastClickMs < 320L && dx * dx + dy * dy < 64.0D) {
                    centerOnPlayer(context);
                    draggingMap = false;
                } else {
                    draggingMap = true;
                    selectedMarkerId = "";
                    selectedWaypointId = "";
                }
                lastClickMs = now;
                lastClickX = mouseX;
                lastClickY = mouseY;
                searchFocused = false;
                actionMenuOpen = false;
                return true;
            }
            searchFocused = false;
            actionMenuOpen = false;
            return false;
        }

        @Override
        public boolean mouseDragged(TerminalRenderContext context, double mouseX, double mouseY, int button,
                double dragX, double dragY) {
            if (button != 0 || !draggingMap || lastMapW <= 0 || lastMapH <= 0) {
                return false;
            }
            centerX -= dragX / Math.max(0.35D, zoom);
            centerZ -= dragY / Math.max(0.35D, zoom);
            cameraReady = true;
            requestTerrain(context, false);
            return true;
        }

        @Override
        public boolean mouseReleased(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            if (button == 0 && draggingMap) {
                draggingMap = false;
                requestTerrain(context, true);
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseScrolled(TerminalRenderContext context, double mouseX, double mouseY, double delta) {
            if (!TerminalUi.inside(mouseX, mouseY, lastMapX, lastMapY, lastMapW, lastMapH)) {
                return false;
            }
            double worldX = screenToWorldX(mouseX);
            double worldZ = screenToWorldZ(mouseY);
            double before = zoom;
            zoom = clamp(zoom * (delta > 0.0D ? 1.2D : 0.82D), 0.35D, 6.0D);
            if (before != zoom && lastMapW > 0 && lastMapH > 0) {
                double afterX = screenToWorldX(mouseX);
                double afterZ = screenToWorldZ(mouseY);
                centerX += worldX - afterX;
                centerZ += worldZ - afterZ;
            }
            requestTerrain(context, true);
            return true;
        }

        @Override
        public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
            int key = event.key();
            if (searchFocused) {
                if (key == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.offsetByCodePoints(searchText.length(), -1));
                    return true;
                }
                if (key == GLFW.GLFW_KEY_ESCAPE) {
                    searchFocused = false;
                    if (!searchText.isEmpty()) {
                        searchText = "";
                    }
                    return true;
                }
                if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                    searchFocused = false;
                    return true;
                }
                if (key != GLFW.GLFW_KEY_LEFT && key != GLFW.GLFW_KEY_RIGHT
                        && key != GLFW.GLFW_KEY_UP && key != GLFW.GLFW_KEY_DOWN) {
                    return false;
                }
            }
            double pan = Math.max(16.0D, 72.0D / Math.max(0.35D, zoom));
            if (key == GLFW.GLFW_KEY_LEFT) {
                centerX -= pan;
            } else if (key == GLFW.GLFW_KEY_RIGHT) {
                centerX += pan;
            } else if (key == GLFW.GLFW_KEY_UP) {
                centerZ -= pan;
            } else if (key == GLFW.GLFW_KEY_DOWN) {
                centerZ += pan;
            } else if (key == GLFW.GLFW_KEY_C || key == GLFW.GLFW_KEY_HOME) {
                centerOnPlayer(context);
            } else if (key == GLFW.GLFW_KEY_EQUAL || key == GLFW.GLFW_KEY_KP_ADD) {
                zoom = clamp(zoom * 1.2D, 0.35D, 6.0D);
            } else if (key == GLFW.GLFW_KEY_MINUS || key == GLFW.GLFW_KEY_KP_SUBTRACT) {
                zoom = clamp(zoom * 0.82D, 0.35D, 6.0D);
            } else {
                return false;
            }
            cameraReady = true;
            requestTerrain(context, true);
            return true;
        }

        @Override
        public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
            if (!searchFocused || event == null || !event.isAllowedChatCharacter() || searchText.length() >= 72) {
                return false;
            }
            String typed = event.codepointAsString();
            if (typed == null || typed.isBlank()) {
                return false;
            }
            searchText += typed.toLowerCase(Locale.ROOT);
            return true;
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return 620;
        }

        private void maybeRefresh(TerminalRenderContext context) {
            if (context.player() == null) {
                return;
            }
            long now = context.player().level().getGameTime();
            if (HoloMapClientState.snapshot().gameTime() == 0L || now - lastRefreshTick > 120L) {
                requestRefresh(context);
            }
        }

        private void requestRefresh(TerminalRenderContext context) {
            if (context.player() == null) {
                return;
            }
            lastRefreshTick = context.player().level().getGameTime();
            context.sendAction(HoloMapIds.TAB, HoloMapIds.REFRESH_ACTION, "");
            EchoNetClientActions.sendServerboundAction(HoloMapWaypointActionPacket.requestSync());
        }

        private void ensureCamera(TerminalRenderContext context) {
            if (cameraReady || context.player() == null) {
                return;
            }
            centerOnPlayer(context);
        }

        private void centerOnPlayer(TerminalRenderContext context) {
            if (context.player() == null) {
                return;
            }
            centerX = context.player().getX();
            centerZ = context.player().getZ();
            cameraReady = true;
            if (renderedOnce) {
                requestTerrain(context, true);
            }
        }

        private void requestTerrain(TerminalRenderContext context, boolean force) {
            if (context.player() == null || lastMapW <= 0 || lastMapH <= 0) {
                return;
            }
            long now = context.player().level().getGameTime();
            int centerChunkX = Math.floorDiv((int) Math.floor(centerX), 16);
            int centerChunkZ = Math.floorDiv((int) Math.floor(centerZ), 16);
            int radius = visibleChunkRadius();
            if (!force && now - lastTerrainRequestTick < 20L) {
                return;
            }
            if (!force
                    && centerChunkX == lastRequestChunkX
                    && centerChunkZ == lastRequestChunkZ
                    && radius == lastRequestRadius) {
                return;
            }
            lastTerrainRequestTick = now;
            lastRequestChunkX = centerChunkX;
            lastRequestChunkZ = centerChunkZ;
            lastRequestRadius = radius;
            EchoNetClientActions.sendServerboundAction(new HoloMapTileRequestPacket(
                    context.player().level().dimension().identifier().toString(),
                    centerChunkX,
                    centerChunkZ,
                    radius));
        }

        private int visibleChunkRadius() {
            double blocksAcross = Math.max(lastMapW, lastMapH) / Math.max(0.35D, zoom);
            return Math.max(1, Math.min(32, (int) Math.ceil(blocksAcross / 32.0D) + 1));
        }

        private int worldToScreenX(double worldX) {
            return lastMapX + lastMapW / 2 + (int) Math.round((worldX - centerX) * zoom);
        }

        private int worldToScreenZ(double worldZ) {
            return lastMapY + lastMapH / 2 + (int) Math.round((worldZ - centerZ) * zoom);
        }

        private double screenToWorldX(double screenX) {
            return centerX + (screenX - (lastMapX + lastMapW / 2.0D)) / Math.max(0.35D, zoom);
        }

        private double screenToWorldZ(double screenY) {
            return centerZ + (screenY - (lastMapY + lastMapH / 2.0D)) / Math.max(0.35D, zoom);
        }

        private void syncLayers(List<HoloMapSnapshotPacket.LayerData> layers) {
            for (HoloMapSnapshotPacket.LayerData layer : layers) {
                layerEnabled.putIfAbsent(layer.id(), layer.visibleByDefault());
            }
        }

        private void drawLayerButtons(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<HoloMapSnapshotPacket.LayerData> layers, int x, int y, int width, int mouseX, int mouseY) {
            int cx = x;
            int cy = y;
            for (HoloMapSnapshotPacket.LayerData layer : layers) {
                String label = layerLabel(layer.title());
                int bw = Math.max(48, Math.min(84, label.length() * 6 + 16));
                if (cx + bw > x + width) {
                    cx = x;
                    cy += 20;
                }
                boolean enabled = layerEnabled.getOrDefault(layer.id(), layer.visibleByDefault());
                int color = enabled ? layer.color() : TerminalUi.accentDim(context);
                TerminalUi.compactButton(context, graphics, cx, cy, bw, label, color, true,
                        TerminalUi.inside(mouseX, mouseY, cx, cy, bw, 16));
                buttons.add(new Button(cx, cy, bw, 16, ButtonKind.LAYER, layer.id()));
                cx += bw + 6;
                if (cy > y + 20) {
                    break;
                }
            }
        }

        private void drawSearchBox(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int mouseX, int mouseY) {
            lastSearchX = x;
            lastSearchY = y;
            lastSearchW = w;
            lastSearchH = 16;
            int border = searchFocused ? TerminalUi.text(context) : ACCENT;
            graphics.fill(x, y, x + w, y + 16, 0xAA061014);
            graphics.outline(x, y, w, 16, border);
            String label = searchText.isBlank() ? "SEARCH MARKERS / WAYPOINTS" : searchText;
            TerminalUi.line(context, graphics, label, x + 6, y + 4, Math.max(24, w - 30),
                    searchText.isBlank() ? TerminalUi.muted(context) : TerminalUi.text(context));
            if (!searchText.isBlank()) {
                TerminalUi.compactButton(context, graphics, x + w - 22, y, 20, "X", TerminalUi.danger(context), true,
                        TerminalUi.inside(mouseX, mouseY, x + w - 22, y, 20, 16));
                buttons.add(new Button(x + w - 22, y, 20, 16, ButtonKind.SEARCH_CLEAR,
                        HoloMapIds.id("button/search_clear")));
            }
        }

        private void drawMap(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                HoloMapSnapshotPacket snapshot, List<HoloMapSnapshotPacket.MarkerData> markers,
                List<HoloMapWaypoint> waypoints,
                int x, int y, int w, int h,
                int mouseX, int mouseY, long gameTime) {
            ensureCamera(context);
            lastMapX = x;
            lastMapY = y;
            lastMapW = w;
            lastMapH = h;
            if (renderedOnce) {
                requestTerrain(context, false);
            }
            TerminalUi.flatHudPanel(context, graphics, x, y, w, h, ACCENT);
            graphics.enableScissor(x + 4, y + 4, x + w - 4, y + h - 4);
            String dimension = context.player() == null
                    ? "minecraft:overworld"
                    : context.player().level().dimension().identifier().toString();
            double playerX = context.player() == null ? centerX : context.player().getX();
            double playerZ = context.player() == null ? centerZ : context.player().getZ();
            float playerYaw = context.player() == null ? 0.0F : context.player().getYRot();
            HoloMapViewState state = new HoloMapViewState(dimension, x, y, w, h, centerX, centerZ, zoom,
                    true, HoloMapVisibility.FieldMode.AUTO_NEAR, showWaypoints,
                    selectedMarkerId, selectedWaypointId, mouseX, mouseY,
                    playerX, playerZ, playerYaw);
            HoloMapRenderer.RenderBudget renderBudget = new HoloMapRenderer.RenderBudget(
                    HoloMapRenderer.TERMINAL_BUDGET.maxTerrainTiles(),
                    HoloMapRenderer.TERMINAL_BUDGET.maxMarkers(),
                    HoloMapRenderer.TERMINAL_BUDGET.maxWaypoints(),
                    HoloMapRenderer.TERMINAL_BUDGET.maxRouteSegments(),
                    HoloMapRenderer.TERMINAL_BUDGET.maxOverlays(),
                    detailLabelLimit(),
                    true,
                    true);
            lastRenderResult = renderer.render(graphics, Minecraft.getInstance().font, state,
                    snapshot, markers, waypoints, renderBudget);
            markerHits.clear();
            for (HoloMapRenderer.MarkerHit hit : lastRenderResult.markerHits()) {
                markerHits.add(new MarkerHit(hit.marker(), hit.x(), hit.y()));
            }
            waypointHits.clear();
            for (HoloMapRenderer.WaypointHit hit : lastRenderResult.waypointHits()) {
                waypointHits.add(new WaypointHit(hit.waypoint(), hit.x(), hit.y()));
            }
            if (markers.isEmpty() && waypoints.isEmpty()) {
                TerminalUi.line(context, graphics, lastRenderResult.syncedTerrainTiles() == 0
                                ? "Real chunk scan pending. Move through loaded chunks or run debug scan_terrain."
                                : "No synced markers or waypoints for the current filters.",
                        x + 16, y + 22, w - 32, TerminalUi.muted(context));
            }
            HoloMapTerrainClientState.DetailStats terrainStats = HoloMapTerrainClientState.detailStats(dimension);
            String culled = lastRenderResult.culledTerrainTiles() > 0
                    ? " | terrain culled " + lastRenderResult.culledTerrainTiles()
                    : "";
            int realChunks = HoloMapTerrainClientState.discoveredCount();
            String terrainText = HoloMapRenderer.terrainStatusLabel(lastRenderResult, realChunks);
            TerminalUi.line(context, graphics, "ATLAS " + terrainText + " | "
                            + terrainStats.compactLabel() + " | XYZ " + (int) centerX + " / " + (int) centerZ
                            + " | " + String.format(Locale.ROOT, "%.2fx", zoom) + " | M " + markers.size()
                            + " | WP " + waypoints.size() + culled,
                    x + 12, y + 10, w - 24, TerminalUi.accent(context));
            drawLegendRow(context, graphics, x + 12, y + h - 18, w - 24, markers.size(), waypoints.size());
            if (actionMenuOpen) {
                drawActionMenu(context, graphics, mouseX, mouseY);
            }
            graphics.disableScissor();
            renderedOnce = true;
        }

        private void drawOverlays(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<HoloMapSnapshotPacket.OverlayData> overlays, int x, int y, int w, int h) {
            String dimension = context.player() == null
                    ? "minecraft:overworld"
                    : context.player().level().dimension().identifier().toString();
            for (HoloMapSnapshotPacket.OverlayData overlay : overlays) {
                if (!dimension.equals(overlay.dimension())
                        || !HoloMapVisibility.visibleInNormalView(overlay.state())) {
                    continue;
                }
                int cx = worldToScreenX(overlay.x());
                int cy = worldToScreenZ(overlay.z());
                int radius = Math.max(5, Math.min(480, (int) Math.round(overlay.radius() * zoom)));
                if (cx + radius < x || cx - radius > x + w || cy + radius < y || cy - radius > y + h) {
                    continue;
                }
                int color = withAlpha(overlay.color(), overlay.precision() == HoloMapPrecision.VIRTUAL ? 0x50 : 0x72);
                drawRing(graphics, cx, cy, radius, color);
                if (overlay.precision() != HoloMapPrecision.PRECISE && radius > 12) {
                    drawRing(graphics, cx, cy, Math.max(4, radius - 4), withAlpha(color, 0x34));
                }
            }
        }

        private boolean drawRoutes(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<HoloMapSnapshotPacket.RouteData> routes) {
            String dimension = context.player() == null
                    ? "minecraft:overworld"
                    : context.player().level().dimension().identifier().toString();
            boolean drew = false;
            for (HoloMapSnapshotPacket.RouteData route : routes) {
                List<HoloMapSnapshotPacket.RoutePointData> points = route.points().stream()
                        .filter(point -> dimension.equals(point.dimension()))
                        .sorted(Comparator.comparingInt(HoloMapSnapshotPacket.RoutePointData::order))
                        .toList();
                if (points.size() < 2) {
                    continue;
                }
                drew = true;
                if (!HoloMapVisibility.visibleInNormalView(route.state())) {
                    continue;
                }
                int color = withAlpha(route.color(), 0xAA);
                for (int i = 1; i < points.size(); i++) {
                    HoloMapSnapshotPacket.RoutePointData previous = points.get(i - 1);
                    HoloMapSnapshotPacket.RoutePointData current = points.get(i);
                    HoloMapGlyphRenderer.drawLine(graphics, worldToScreenX(previous.x()), worldToScreenZ(previous.z()),
                            worldToScreenX(current.x()), worldToScreenZ(current.z()), color);
                }
            }
            return drew;
        }

        private void drawRing(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
            int segments = radius > 88 ? 36 : 24;
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

        private void drawLegendRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int markerCount, int waypointCount) {
            Config.VisualDensity density = HoloMapVisualStyle.visualDensity();
            int bg = density == Config.VisualDensity.LOW ? 0x66061014 : 0xAA061014;
            graphics.fill(x - 4, y - 2, x + w + 4, y + 14, bg);
            graphics.outline(x - 4, y - 2, w + 8, 16, 0x5538DFF4);
            int sx = x + 6;
            sx = legendGlyph(graphics, context, sx, y + 6, IMapMarker.MarkerKind.MISSION,
                    IMapMarker.MarkerState.DISCOVERED, "MISSION", HoloMapVisualStyle.ACCENT);
            sx = legendGlyph(graphics, context, sx, y + 6, IMapMarker.MarkerKind.HAZARD,
                    IMapMarker.MarkerState.DISCOVERED, "HAZARD", HoloMapVisualStyle.DANGER);
            sx = legendGlyph(graphics, context, sx, y + 6, IMapMarker.MarkerKind.ROUTE,
                    IMapMarker.MarkerState.DISCOVERED, "ROUTE", HoloMapVisualStyle.SUCCESS);
            if (density != Config.VisualDensity.LOW) {
                sx = legendWaypoint(graphics, context, sx, y + 6, Scope.PERSONAL, "WP", 0xFF92F7A6);
                sx = legendGlyph(graphics, context, sx, y + 6, IMapMarker.MarkerKind.ORBITAL_SCAN,
                        IMapMarker.MarkerState.CHECKED, "CHECKED", HoloMapVisualStyle.MUTED);
            }
            String counts = "visible " + markerCount + " / wp " + waypointCount + " / "
                    + String.format(Locale.ROOT, "%.2fx", zoom);
            TerminalUi.line(context, graphics, counts, Math.min(sx + 4, x + w - 120), y + 2, 116,
                    TerminalUi.muted(context));
        }

        private int legendGlyph(GuiGraphicsExtractor graphics, TerminalRenderContext context, int x, int y,
                IMapMarker.MarkerKind kind, IMapMarker.MarkerState state, String label, int color) {
            HoloMapGlyphRenderer.drawMarkerKind(graphics, kind, state, x, y, color, 4);
            TerminalUi.line(context, graphics, label, x + 8, y - 4, 54, color);
            return x + 62;
        }

        private int legendWaypoint(GuiGraphicsExtractor graphics, TerminalRenderContext context, int x, int y,
                Scope scope, String label, int color) {
            HoloMapGlyphRenderer.drawWaypointScope(graphics, scope, x, y, color, 4);
            TerminalUi.line(context, graphics, label, x + 8, y - 4, 32, color);
            return x + 40;
        }

        private int drawTerrain(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int h, long gameTime) {
            String dimension = context.player() == null
                    ? "minecraft:overworld"
                    : context.player().level().dimension().identifier().toString();
            int minChunkX = Math.floorDiv((int) Math.floor(screenToWorldX(x + 4)), 16) - 1;
            int maxChunkX = Math.floorDiv((int) Math.floor(screenToWorldX(x + w - 4)), 16) + 1;
            int minChunkZ = Math.floorDiv((int) Math.floor(screenToWorldZ(y + 4)), 16) - 1;
            int maxChunkZ = Math.floorDiv((int) Math.floor(screenToWorldZ(y + h - 4)), 16) + 1;
            List<HoloMapTerrainTile> tiles = HoloMapTerrainClientState.tiles(dimension,
                    minChunkX, maxChunkX, minChunkZ, maxChunkZ);
            if (tiles.isEmpty()) {
                drawGrid(context, graphics, x, y, w, h, gameTime);
                return 0;
            }
            graphics.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xD8061014);
            for (HoloMapTerrainTile tile : tiles) {
                drawTerrainTile(graphics, tile);
            }
            return tiles.size();
        }

        private void drawTerrainTile(GuiGraphicsExtractor graphics, HoloMapTerrainTile tile) {
            double baseX = tile.chunkX() * 16.0D;
            double baseZ = tile.chunkZ() * 16.0D;
            int screenX = worldToScreenX(baseX);
            int screenY = worldToScreenZ(baseZ);
            int chunkSize = Math.max(1, (int) Math.ceil(16.0D * zoom));
            if (chunkSize <= 18) {
                graphics.fill(screenX, screenY, screenX + chunkSize, screenY + chunkSize,
                        HoloMapVisualStyle.terrainColor(tile.averageColor()));
                return;
            }
            int pixelSize = Math.max(1, (int) Math.ceil(zoom));
            for (int localZ = 0; localZ < HoloMapTerrainTile.SIZE; localZ++) {
                for (int localX = 0; localX < HoloMapTerrainTile.SIZE; localX++) {
                    int px = worldToScreenX(baseX + localX);
                    int py = worldToScreenZ(baseZ + localZ);
                    graphics.fill(px, py, px + pixelSize, py + pixelSize,
                            HoloMapVisualStyle.terrainColor(tile.pixel(localX, localZ)));
                }
            }
        }

        private void drawWorldGrid(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int h) {
            int worldStep = 16;
            while (worldStep * zoom < 12.0D && worldStep < 256) {
                worldStep *= 2;
            }
            if (worldStep * zoom < 10.0D) {
                return;
            }
            double left = screenToWorldX(x + 4);
            double right = screenToWorldX(x + w - 4);
            double top = screenToWorldZ(y + 4);
            double bottom = screenToWorldZ(y + h - 4);
            int startX = (int) Math.floor(left / worldStep) * worldStep;
            for (int worldX = startX; worldX <= right; worldX += worldStep) {
                int sx = worldToScreenX(worldX);
                int color = worldX == 0 ? 0x6638DFF4 : 0x2438DFF4;
                graphics.fill(sx, y + 8, sx + 1, y + h - 8, color);
            }
            int startZ = (int) Math.floor(top / worldStep) * worldStep;
            for (int worldZ = startZ; worldZ <= bottom; worldZ += worldStep) {
                int sy = worldToScreenZ(worldZ);
                int color = worldZ == 0 ? 0x6638DFF4 : 0x2438DFF4;
                graphics.fill(x + 8, sy, x + w - 8, sy + 1, color);
            }
            if (context.player() != null) {
                int px = worldToScreenX(context.player().getX());
                int pz = worldToScreenZ(context.player().getZ());
                graphics.fill(px - 3, pz - 3, px + 4, pz + 4, 0xFFFFFFFF);
                graphics.outline(px - 6, pz - 6, 12, 12, TerminalUi.accent(context));
            }
        }

        private void drawGrid(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, int h, long gameTime) {
            graphics.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xD8050D14);
            for (int gx = x + 18; gx < x + w - 10; gx += 24) {
                graphics.fill(gx, y + 8, gx + 1, y + h - 8, 0x2438DFF4);
            }
            for (int gy = y + 18; gy < y + h - 10; gy += 24) {
                graphics.fill(x + 8, gy, x + w - 8, gy + 1, 0x2438DFF4);
            }
            int centerX = x + w / 2;
            int centerY = y + h / 2;
            graphics.fill(centerX - 22, centerY, centerX + 23, centerY + 1, 0x7738DFF4);
            graphics.fill(centerX, centerY - 22, centerX + 1, centerY + 23, 0x7738DFF4);
            int sweep = x + 12 + (int) Math.floorMod(gameTime, Math.max(1, w - 24));
            graphics.fill(sweep, y + 8, sweep + 2, y + h - 8, 0x5538DFF4);
            TerminalUi.line(context, graphics, "ECHO HOLOGRAPHIC ATLAS", x + 12, y + 10, w - 24,
                    TerminalUi.accent(context));
        }

        private void drawWaypoints(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<HoloMapWaypoint> waypoints, int mouseX, int mouseY) {
            if (!showWaypoints || waypoints.isEmpty()) {
                return;
            }
            int labels = Math.max(2, detailLabelLimit() / 2);
            int drawn = 0;
            for (HoloMapWaypoint waypoint : waypoints) {
                int px = worldToScreenX(waypoint.x());
                int py = worldToScreenZ(waypoint.z());
                if (px < lastMapX - 32 || px > lastMapX + lastMapW + 32
                        || py < lastMapY - 32 || py > lastMapY + lastMapH + 32) {
                    continue;
                }
                boolean selected = waypoint.id().toString().equals(selectedWaypointId);
                boolean hovered = TerminalUi.inside(mouseX, mouseY, px - 6, py - 6, 12, 12);
                int color = waypoint.visible() ? waypoint.color() : TerminalUi.muted(context);
                HoloMapGlyphRenderer.drawWaypoint(graphics, waypoint, px, py, color, 4, selected || hovered);
                waypointHits.add(new WaypointHit(waypoint, px, py));
                if (selected || hovered || drawn++ < labels) {
                    TerminalUi.line(context, graphics, waypoint.title(), px + 8, py - 4, 116,
                            selected ? TerminalUi.text(context) : waypoint.color());
                }
            }
        }

        private void drawActionMenu(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            String dimension = context.player() == null ? "minecraft:overworld"
                    : context.player().level().dimension().identifier().toString();
            int chunkX = actionChunkX();
            int chunkZ = actionChunkZ();
            boolean renderableChunk = HoloMapTerrainClientState.hasRenderableTile(dimension, chunkX, chunkZ);
            List<HoloMapChunkMenuAction> chunkActions = renderableChunk
                    ? HoloMapClientChunkActions.actions(dimension, chunkX, chunkZ)
                    : List.of();
            int chunkRows = Math.max(1, chunkActions.size());
            int w = 128;
            int h = 64 + (selectedWaypointId.isBlank() ? 0 : 20) + chunkRows * 20;
            int x = Math.max(lastMapX + 8, Math.min(actionMenuX, lastMapX + lastMapW - w - 8));
            int y = Math.max(lastMapY + 8, Math.min(actionMenuY, lastMapY + lastMapH - h - 8));
            graphics.fill(x, y, x + w, y + h, 0xEE061014);
            graphics.outline(x, y, w, h, ACCENT);
            TerminalUi.line(context, graphics, "MAP ACTION", x + 8, y + 6, w - 16, TerminalUi.accent(context));
            menuButton(graphics, context, "LOCAL", x + 8, y + 22, 54, mouseX, mouseY, ButtonKind.MENU_LOCAL);
            menuButton(graphics, context, "PERSONAL", x + 66, y + 22, 54, mouseX, mouseY, ButtonKind.MENU_PERSONAL);
            menuButton(graphics, context, "SHARED", x + 8, y + 42, 54, mouseX, mouseY, ButtonKind.MENU_SHARED);
            menuButton(graphics, context, "COPY", x + 66, y + 42, 54, mouseX, mouseY, ButtonKind.MENU_COPY);
            int rowY = y + 62;
            if (!selectedWaypointId.isBlank()) {
                menuButton(graphics, context, "MOVE SELECTED", x + 8, rowY, 112, mouseX, mouseY,
                        ButtonKind.MENU_MOVE_SELECTED);
                rowY += 20;
            }
            if (!renderableChunk) {
                menuButton(graphics, context, "PENDING SCAN", x + 8, rowY, 112,
                        false, mouseX, mouseY, ButtonKind.CHUNK_ACTION, null, HoloMapVisualStyle.MUTED);
            } else if (chunkActions.isEmpty()) {
                menuButton(graphics, context, "NO CLAIM ACTION", x + 8, rowY, 112,
                        false, mouseX, mouseY, ButtonKind.CHUNK_ACTION, null, HoloMapVisualStyle.MUTED);
            } else {
                for (HoloMapChunkMenuAction action : chunkActions) {
                    menuButton(graphics, context, action.label(), x + 8, rowY, 112,
                            action.enabled(), mouseX, mouseY, ButtonKind.CHUNK_ACTION, action.menuId(),
                            action.color());
                    rowY += 20;
                }
            }
        }

        private void menuButton(GuiGraphicsExtractor graphics, TerminalRenderContext context, String label,
                int x, int y, int w, int mouseX, int mouseY, ButtonKind kind) {
            menuButton(graphics, context, label, x, y, w, true, mouseX, mouseY, kind,
                    HoloMapIds.id("button/" + kind.name().toLowerCase(Locale.ROOT)), ACCENT);
        }

        private void menuButton(GuiGraphicsExtractor graphics, TerminalRenderContext context, String label,
                int x, int y, int w, boolean enabled, int mouseX, int mouseY, ButtonKind kind,
                Identifier id, int color) {
            TerminalUi.compactButton(context, graphics, x, y, w, label, color == 0 ? ACCENT : color, enabled,
                    enabled && TerminalUi.inside(mouseX, mouseY, x, y, w, 16));
            if (enabled) {
                buttons.add(new Button(x, y, w, 16, kind, id == null
                        ? HoloMapIds.id("button/" + kind.name().toLowerCase(Locale.ROOT))
                        : id));
            }
        }

        private void drawMarker(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                MarkerPoint point, boolean highlighted) {
            HoloMapSnapshotPacket.MarkerData marker = point.marker();
            int color = colorForMarker(context, marker);
            if (marker.precision() == HoloMapPrecision.VIRTUAL) {
                color = withAlpha(color, 0x72);
            }
            HoloMapGlyphRenderer.drawMarker(graphics, marker, point.x(), point.y(), color, 4, highlighted);
        }

        private void drawDetailPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                HoloMapSnapshotPacket snapshot, List<HoloMapSnapshotPacket.MarkerData> markers,
                List<HoloMapWaypoint> waypoints, int x, int y, int w, int h, int mouseX, int mouseY) {
            TerminalUi.flatHudPanel(context, graphics, x, y, w, h, ACCENT);
            TerminalUi.line(context, graphics, "MAP INDEX", x + 12, y + 10, w - 24, TerminalUi.accent(context));
            int cy = y + 30;
            cy = metric(context, graphics, x + 12, cy, w - 24, "Visible", String.valueOf(markers.size()), ACCENT);
            cy = metric(context, graphics, x + 12, cy, w - 24, "Synced", String.valueOf(snapshot.markers().size()),
                    TerminalUi.muted(context));
            cy = metric(context, graphics, x + 12, cy, w - 24, "Waypoints", String.valueOf(waypoints.size()),
                    TerminalUi.warning(context));
            cy = metric(context, graphics, x + 12, cy, w - 24, "Layers", String.valueOf(snapshot.layers().size()),
                    TerminalUi.muted(context));
            cy = metric(context, graphics, x + 12, cy, w - 24, "Routes", String.valueOf(snapshot.routes().size()),
                    TerminalUi.success(context));
            cy = metric(context, graphics, x + 12, cy, w - 24, "Overlays", String.valueOf(snapshot.overlays().size()),
                    TerminalUi.danger(context));
            String dimension = context.player() == null ? "minecraft:overworld"
                    : context.player().level().dimension().identifier().toString();
            HoloMapTerrainClientState.DetailStats terrainStats = HoloMapTerrainClientState.detailStats(dimension);
            cy = metric(context, graphics, x + 12, cy, w - 24, "Real Chunks",
                    HoloMapTerrainClientState.tileCount(dimension) + " / " + HoloMapTerrainClientState.discoveredCount(),
                    TerminalUi.accent(context));
            cy = metric(context, graphics, x + 12, cy, w - 24, "Detail",
                    terrainStats.compactLabel(), TerminalUi.success(context));
            List<MapListEntry> entries = listEntries(context, markers, waypoints);
            TerminalUi.divider(graphics, x + 12, cy + 2, w - 24, ACCENT);
            cy += 12;
            TerminalUi.line(context, graphics, "RESULTS " + entries.size() + " / " + sortMode.label,
                    x + 12, cy, w - 24, TerminalUi.accent(context));
            cy += 14;
            int rows = Math.max(3, Math.min(8, (h - (cy - y) - 116) / 17));
            for (int i = 0; i < Math.min(rows, entries.size()); i++) {
                MapListEntry entry = entries.get(i);
                boolean selected = entry.id().equals(selectedMarkerId) || entry.id().equals(selectedWaypointId);
                int rowColor = selected ? TerminalUi.text(context) : entry.color(context);
                graphics.fill(x + 10, cy - 2, x + w - 10, cy + 14,
                        selected ? 0x4426DFF4 : TerminalUi.inside(mouseX, mouseY, x + 10, cy - 2, w - 20, 16)
                                ? 0x22163843 : 0x00000000);
                String suffix = entry.count() > 1 ? " x" + entry.count() : "";
                TerminalUi.line(context, graphics, entry.prefix() + " " + entry.title() + suffix,
                        x + 14, cy, w - 28, rowColor);
                listHits.add(new ListEntryHit(entry, x + 10, cy - 2, w - 20, 16));
                cy += 17;
            }
            TerminalUi.divider(graphics, x + 12, cy + 2, w - 24, ACCENT);
            cy += 12;
            HoloMapWaypoint selectedWaypoint = selectedWaypoint(waypoints);
            HoloMapSnapshotPacket.MarkerData selectedMarker = selectedMarkerId.isBlank() || selectedWaypoint != null
                    ? null
                    : selectedMarker(markers);
            if (selectedWaypoint != null) {
                TerminalUi.line(context, graphics, selectedWaypoint.title(), x + 12, cy, w - 24,
                        selectedWaypoint.color());
                cy += 16;
                cy = metric(context, graphics, x + 12, cy, w - 24, "Scope", selectedWaypoint.scope().name(),
                        selectedWaypoint.color());
                cy = metric(context, graphics, x + 12, cy, w - 24, "Dim", selectedWaypoint.dimension(),
                        TerminalUi.muted(context));
                cy = metric(context, graphics, x + 12, cy, w - 24, "XYZ",
                        (int) selectedWaypoint.x() + " / " + (int) selectedWaypoint.y() + " / " + (int) selectedWaypoint.z(),
                        TerminalUi.text(context));
                TerminalUi.compactButton(context, graphics, x + 12, cy + 6, 62, "DELETE",
                        TerminalUi.danger(context), true,
                        TerminalUi.inside(mouseX, mouseY, x + 12, cy + 6, 62, 16));
                buttons.add(new Button(x + 12, cy + 6, 62, 16, ButtonKind.WAYPOINT_DELETE, selectedWaypoint.id()));
                return;
            }
            if (selectedMarker == null) {
                TerminalUi.wrap(context, graphics, "Select a marker or waypoint, search by name/source/coords, or right-click the atlas to place a waypoint.",
                        x + 12, cy, w - 24, TerminalUi.muted(context));
                return;
            }
            TerminalUi.line(context, graphics, selectedMarker.title(), x + 12, cy, w - 24, colorForMarker(context, selectedMarker));
            cy += 16;
            cy = metric(context, graphics, x + 12, cy, w - 24, "State", selectedMarker.state().name(),
                    colorForMarker(context, selectedMarker));
            cy = metric(context, graphics, x + 12, cy, w - 24, "Source", selectedMarker.sourceId().toString(), TerminalUi.muted(context));
            cy = metric(context, graphics, x + 12, cy, w - 24, "Layer", selectedMarker.layerId().getPath().replace("layer/", ""),
                    TerminalUi.muted(context));
            cy = metric(context, graphics, x + 12, cy, w - 24, "Dim", selectedMarker.dimension(), TerminalUi.muted(context));
            cy = metric(context, graphics, x + 12, cy, w - 24, "XYZ",
                    (int) selectedMarker.x() + " / " + (int) selectedMarker.y() + " / " + (int) selectedMarker.z(),
                    selectedMarker.precise() ? TerminalUi.text(context) : TerminalUi.warning(context));
            if (!selectedMarker.routeId().isBlank()) {
                cy = metric(context, graphics, x + 12, cy, w - 24, "Route", selectedMarker.routeId(), TerminalUi.success(context));
            }
            TerminalUi.wrap(context, graphics, selectedMarker.summary(), x + 12, cy + 8, w - 24, TerminalUi.text(context));
        }

        private int metric(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int x, int y, int w, String label, String value, int color) {
            TerminalUi.line(context, graphics, label, x, y, Math.max(42, w / 3), TerminalUi.muted(context));
            TerminalUi.line(context, graphics, value, x + Math.max(46, w / 3), y,
                    Math.max(42, w - Math.max(46, w / 3)), color);
            return y + 13;
        }

        private HoloMapSnapshotPacket.MarkerData selectedMarker(List<HoloMapSnapshotPacket.MarkerData> markers) {
            if (!selectedMarkerId.isBlank()) {
                for (HoloMapSnapshotPacket.MarkerData marker : markers) {
                    if (selectedMarkerId.equals(marker.id().toString())) {
                        return marker;
                    }
                }
            }
            return markers.isEmpty() ? null : markers.getFirst();
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

        private int detailLabelLimit() {
            if (detailMode == 0) {
                return 0;
            }
            int configured = configuredDetailLabelLimit();
            return detailMode == 1 ? Math.min(14, configured) : configured;
        }

        private static int configuredDetailLabelLimit() {
            try {
                return Math.max(0, Config.DETAIL_LABEL_LIMIT.get());
            } catch (RuntimeException exception) {
                return 42;
            }
        }

        private List<HoloMapSnapshotPacket.MarkerData> visibleMarkers(String dimension) {
            return HoloMapClientState.markersForDimension(dimension).stream()
                    .filter(marker -> layerEnabled.getOrDefault(marker.layerId(), true))
                    .filter(marker -> HoloMapVisibility.visibleInNormalView(marker.state()))
                    .filter(this::matchesStateFilter)
                    .filter(this::matchesSearch)
                    .sorted(Comparator.comparingInt(this::markerLabelPriority).reversed()
                            .thenComparingDouble(marker -> distanceFromCamera(marker.x(), marker.z()))
                            .thenComparing((HoloMapSnapshotPacket.MarkerData marker) -> marker.layerId().toString())
                            .thenComparing(marker -> marker.state().ordinal())
                            .thenComparing(HoloMapSnapshotPacket.MarkerData::title))
                    .toList();
        }

        private int markerLabelPriority(HoloMapSnapshotPacket.MarkerData marker) {
            int score = marker.priority();
            if (marker.id().toString().equals(selectedMarkerId)) {
                score += 1000;
            }
            if (distanceFromCamera(marker.x(), marker.z()) <= 128.0D) {
                score += 120;
            }
            score += switch (marker.kind()) {
                case MISSION -> 80;
                case HAZARD -> 70;
                case ROUTE -> 60;
                case CRASH_SITE, BASE_OUTPOST -> 45;
                case REGION, ORBITAL_SCAN, NEXUS_ANOMALY, DRONE_SCAN -> 30;
                case GENERIC, STRUCTURE, FACTION -> 10;
            };
            if (marker.precision() == HoloMapPrecision.VIRTUAL) {
                score -= 25;
            }
            return score;
        }

        private double distanceFromCamera(double x, double z) {
            double dx = x - centerX;
            double dz = z - centerZ;
            return Math.sqrt(dx * dx + dz * dz);
        }

        private List<HoloMapWaypoint> visibleWaypoints(TerminalRenderContext context) {
            if (!showWaypoints) {
                return List.of();
            }
            String dimension = context.player() == null
                    ? "minecraft:overworld"
                    : context.player().level().dimension().identifier().toString();
            return HoloMapWaypointClientState.waypoints().stream()
                    .filter(HoloMapWaypoint::visible)
                    .filter(waypoint -> waypoint.inDimension(dimension))
                    .filter(this::matchesSearch)
                    .sorted(Comparator.comparing(HoloMapWaypoint::scope)
                            .thenComparing(HoloMapWaypoint::title, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(waypoint -> waypoint.id().toString()))
                    .toList();
        }

        private List<MapListEntry> listEntries(TerminalRenderContext context,
                List<HoloMapSnapshotPacket.MarkerData> markers, List<HoloMapWaypoint> waypoints) {
            List<MapListEntry> entries = new ArrayList<>();
            for (HoloMapWaypoint waypoint : waypoints) {
                entries.add(MapListEntry.waypoint(waypoint, distanceToPlayer(context, waypoint.x(), waypoint.z())));
            }
            Map<String, MapListEntryGroup> groups = new LinkedHashMap<>();
            for (HoloMapSnapshotPacket.MarkerData marker : markers) {
                String key = marker.kind() + "|" + marker.layerId() + "|"
                        + marker.title().toLowerCase(Locale.ROOT);
                groups.computeIfAbsent(key, ignored -> new MapListEntryGroup(marker,
                                distanceToPlayer(context, marker.x(), marker.z())))
                        .add(marker, distanceToPlayer(context, marker.x(), marker.z()),
                                marker.id().toString().equals(selectedMarkerId));
            }
            for (MapListEntryGroup group : groups.values()) {
                entries.add(group.entry());
            }
            Comparator<MapListEntry> comparator = switch (sortMode) {
                case NEAREST -> Comparator.comparingDouble(MapListEntry::distance)
                        .thenComparing(MapListEntry::title, String.CASE_INSENSITIVE_ORDER);
                case NAME -> Comparator.comparing(MapListEntry::title, String.CASE_INSENSITIVE_ORDER);
                case LAYER -> Comparator.comparing(MapListEntry::layer, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(MapListEntry::title, String.CASE_INSENSITIVE_ORDER);
                case STATE -> Comparator.comparing(MapListEntry::state, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(MapListEntry::title, String.CASE_INSENSITIVE_ORDER);
            };
            entries.sort(comparator);
            return entries;
        }

        private double distanceToPlayer(TerminalRenderContext context, double x, double z) {
            if (context.player() == null) {
                double dx = x - centerX;
                double dz = z - centerZ;
                return Math.sqrt(dx * dx + dz * dz);
            }
            double dx = x - context.player().getX();
            double dz = z - context.player().getZ();
            return Math.sqrt(dx * dx + dz * dz);
        }

        private boolean matchesSearch(HoloMapSnapshotPacket.MarkerData marker) {
            String query = normalizedSearch();
            if (query.isBlank()) {
                return true;
            }
            String haystack = (marker.title() + " " + marker.summary() + " " + marker.layerId()
                    + " " + marker.sourceId() + " " + marker.dimension()
                    + " " + (int) marker.x() + " " + (int) marker.y() + " " + (int) marker.z())
                    .toLowerCase(Locale.ROOT);
            return haystack.contains(query);
        }

        private boolean matchesSearch(HoloMapWaypoint waypoint) {
            String query = normalizedSearch();
            if (query.isBlank()) {
                return true;
            }
            String haystack = (waypoint.title() + " " + waypoint.scope() + " " + waypoint.dimension()
                    + " " + (int) waypoint.x() + " " + (int) waypoint.y() + " " + (int) waypoint.z())
                    .toLowerCase(Locale.ROOT);
            return haystack.contains(query);
        }

        private String normalizedSearch() {
            return searchText == null ? "" : searchText.strip().toLowerCase(Locale.ROOT);
        }

        private void focusListEntry(TerminalRenderContext context, MapListEntry entry) {
            if (entry == null) {
                return;
            }
            centerX = entry.x();
            centerZ = entry.z();
            cameraReady = true;
            if (entry.marker() != null) {
                selectedMarkerId = entry.marker().id().toString();
                selectedWaypointId = "";
            } else if (entry.waypoint() != null) {
                selectedWaypointId = entry.waypoint().id().toString();
                selectedMarkerId = "";
            }
            context.playCommandSound();
            requestTerrain(context, true);
        }

        private void createWaypoint(TerminalRenderContext context, Scope scope) {
            if (context.player() == null) {
                return;
            }
            long time = context.player().level().getGameTime();
            String dimension = context.player().level().dimension().identifier().toString();
            double y = Math.floor(context.player().getY());
            String title = scope == Scope.LOCAL
                    ? "Local " + (int) actionWorldX + ", " + (int) actionWorldZ
                    : scope == Scope.PERSONAL
                            ? "Personal " + (int) actionWorldX + ", " + (int) actionWorldZ
                            : "Shared " + (int) actionWorldX + ", " + (int) actionWorldZ;
            int color = scope == Scope.SHARED ? 0xFFFFDA73 : scope == Scope.PERSONAL ? 0xFF92F7A6 : ACCENT;
            HoloMapWaypoint waypoint = HoloMapWaypoint.create(scope, context.player().getUUID(), dimension,
                    actionWorldX, y, actionWorldZ, title, color, time);
            if (scope == Scope.LOCAL) {
                HoloMapLocalWaypointStore.upsert(waypoint);
                selectedWaypointId = waypoint.id().toString();
                selectedMarkerId = "";
                context.playCommandSound();
            } else {
                EchoNetClientActions.sendServerboundAction(HoloMapWaypointActionPacket.upsert(waypoint));
            }
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
            }
            selectedWaypointId = "";
        }

        private void moveSelectedWaypointToAction(TerminalRenderContext context) {
            if (context.player() == null) {
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
            long time = context.player().level().getGameTime();
            HoloMapWaypoint moved = new HoloMapWaypoint(
                    selected.id(),
                    selected.owner(),
                    selected.scope(),
                    context.player().level().dimension().identifier().toString(),
                    actionWorldX,
                    Math.floor(context.player().getY()),
                    actionWorldZ,
                    selected.title(),
                    selected.color(),
                    selected.icon(),
                    selected.visible(),
                    selected.createdTime(),
                    time);
            if (moved.scope() == Scope.LOCAL) {
                HoloMapLocalWaypointStore.upsert(moved);
            } else {
                EchoNetClientActions.sendServerboundAction(HoloMapWaypointActionPacket.upsert(moved));
            }
            selectedWaypointId = moved.id().toString();
            selectedMarkerId = "";
            context.playCommandSound();
        }

        private void copyActionCoordinates(TerminalRenderContext context) {
            String coords = (int) actionWorldX + " " + (context.player() == null ? 64 : (int) context.player().getY())
                    + " " + (int) actionWorldZ;
            Minecraft.getInstance().keyboardHandler.setClipboard(coords);
            context.playCommandSound();
        }

        private void sendChunkAction(TerminalRenderContext context, Identifier menuId) {
            if (context.player() == null || menuId == null) {
                return;
            }
            String dimension = context.player().level().dimension().identifier().toString();
            int chunkX = actionChunkX();
            int chunkZ = actionChunkZ();
            HoloMapChunkMenuAction action = HoloMapClientChunkActions.action(menuId, dimension, chunkX, chunkZ);
            if (action == null || !action.enabled()
                    || !HoloMapTerrainClientState.hasRenderableTile(dimension, chunkX, chunkZ)) {
                return;
            }
            EchoNetClientActions.sendServerboundAction(new HoloMapChunkActionPacket(
                    action.providerId(), action.actionId(), dimension, chunkX, chunkZ));
            requestRefresh(context);
        }

        private int actionChunkX() {
            return Math.floorDiv((int) Math.floor(actionWorldX), 16);
        }

        private int actionChunkZ() {
            return Math.floorDiv((int) Math.floor(actionWorldZ), 16);
        }

        private boolean matchesStateFilter(HoloMapSnapshotPacket.MarkerData marker) {
            return switch (stateFilter) {
                case ALL -> true;
                case OPEN -> marker.state() == IMapMarker.MarkerState.DISCOVERED;
                case LOCKED -> marker.state() == IMapMarker.MarkerState.LOCKED;
                case CHECKED -> marker.state() == IMapMarker.MarkerState.CHECKED;
            };
        }

        private void button(GuiGraphicsExtractor graphics, TerminalRenderContext context, String label,
                int x, int y, int w, boolean enabled, int mouseX, int mouseY, ButtonKind kind, String payload) {
            TerminalUi.compactButton(context, graphics, x, y, w, label, ACCENT, enabled,
                    TerminalUi.inside(mouseX, mouseY, x, y, w, 16));
            buttons.add(new Button(x, y, w, 16, kind, payload == null || payload.isBlank()
                    ? HoloMapIds.id("button/" + kind.name().toLowerCase(Locale.ROOT))
                    : Identifier.tryParse(payload)));
        }

        private static int colorForMarker(TerminalRenderContext context, HoloMapSnapshotPacket.MarkerData marker) {
            if (marker.state() == IMapMarker.MarkerState.LOCKED) {
                return TerminalUi.muted(context);
            }
            if (marker.state() == IMapMarker.MarkerState.CHECKED) {
                return TerminalUi.success(context);
            }
            return switch (marker.kind()) {
                case CRASH_SITE -> 0xFFFFA05B;
                case ROUTE -> TerminalUi.success(context);
                case HAZARD -> TerminalUi.danger(context);
                case MISSION -> TerminalUi.accent(context);
                case BASE_OUTPOST -> TerminalUi.warning(context);
                case ORBITAL_SCAN -> 0xFFA58BFF;
                case NEXUS_ANOMALY -> 0xFFFF8FEA;
                case DRONE_SCAN -> 0xFF7CF7D4;
                case REGION, GENERIC, STRUCTURE, FACTION -> TerminalUi.text(context);
            };
        }

        private static int withAlpha(int color, int alpha) {
            return ((Math.max(0, Math.min(255, alpha)) & 0xFF) << 24) | (color & 0x00FFFFFF);
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }

        private static String layerLabel(String title) {
            String clean = title == null ? "LAYER" : title.toUpperCase(Locale.ROOT);
            clean = clean.replace("BASES/OUTPOSTS", "BASES").replace("ORBITAL SCANS", "ORBITAL")
                    .replace("NEXUS/ANOMALY", "NEXUS").replace("DRONES/SCANS", "DRONES");
            return clean.length() > 10 ? clean.substring(0, 10) : clean;
        }

        private static void drawLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
            int dx = x1 - x0;
            int dy = y1 - y0;
            int steps = Math.max(Math.abs(dx), Math.abs(dy));
            if (steps <= 0) {
                graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
                return;
            }
            for (int i = 0; i <= steps; i++) {
                int x = x0 + dx * i / steps;
                int y = y0 + dy * i / steps;
                graphics.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

    private enum ButtonKind {
        CENTER,
        REFRESH,
        TEST,
        STATE,
        DETAIL,
        LAYER,
        WAYPOINTS,
        SORT,
        SEARCH_CLEAR,
        MENU_LOCAL,
        MENU_PERSONAL,
        MENU_SHARED,
        MENU_COPY,
        MENU_MOVE_SELECTED,
        CHUNK_ACTION,
        WAYPOINT_DELETE
    }

    private record Button(int x, int y, int w, int h, ButtonKind kind, Identifier layerId) {
    }

    private record MarkerHit(HoloMapSnapshotPacket.MarkerData marker, int x, int y) {
    }

    private record WaypointHit(HoloMapWaypoint waypoint, int x, int y) {
    }

    private record ListEntryHit(MapListEntry entry, int x, int y, int w, int h) {
    }

    private record MarkerPoint(HoloMapSnapshotPacket.MarkerData marker, int x, int y) {
    }

    private record MapListEntry(
            String id,
            String title,
            String layer,
            String state,
            double x,
            double z,
            double distance,
            HoloMapSnapshotPacket.MarkerData marker,
            HoloMapWaypoint waypoint,
            int count) {
        private static MapListEntry marker(HoloMapSnapshotPacket.MarkerData marker, double distance, int count) {
            return new MapListEntry(marker.id().toString(), marker.title(),
                    marker.layerId().getPath().replace("layer/", ""), marker.state().name(),
                    marker.x(), marker.z(), distance, marker, null, count);
        }

        private static MapListEntry waypoint(HoloMapWaypoint waypoint, double distance) {
            return new MapListEntry(waypoint.id().toString(), waypoint.title(),
                    "waypoint", waypoint.scope().name(), waypoint.x(), waypoint.z(), distance, null, waypoint, 1);
        }

        private String prefix() {
            return waypoint == null ? "M" : "W";
        }

        private int color(TerminalRenderContext context) {
            if (waypoint != null) {
                return waypoint.color();
            }
            return HoloMapTab.colorForMarker(context, marker);
        }
    }

    private static final class MapListEntryGroup {
        private HoloMapSnapshotPacket.MarkerData marker;
        private double distance;
        private int count;
        private boolean selected;

        private MapListEntryGroup(HoloMapSnapshotPacket.MarkerData marker, double distance) {
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

        private MapListEntry entry() {
            return MapListEntry.marker(marker, distance, count);
        }
    }

    private record Bounds(double minX, double maxX, double minZ, double maxZ) {
        private static Bounds from(List<HoloMapSnapshotPacket.MarkerData> markers) {
            double minX = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (HoloMapSnapshotPacket.MarkerData marker : markers) {
                minX = Math.min(minX, marker.x());
                maxX = Math.max(maxX, marker.x());
                minZ = Math.min(minZ, marker.z());
                maxZ = Math.max(maxZ, marker.z());
            }
            if (maxX - minX < 32.0D) {
                minX -= 16.0D;
                maxX += 16.0D;
            }
            if (maxZ - minZ < 32.0D) {
                minZ -= 16.0D;
                maxZ += 16.0D;
            }
            return new Bounds(minX, maxX, minZ, maxZ);
        }

        private int projectX(HoloMapSnapshotPacket.MarkerData marker, int x, int w) {
            double t = (marker.x() - minX) / Math.max(1.0D, maxX - minX);
            return x + (int) Math.round(t * w);
        }

        private int projectZ(HoloMapSnapshotPacket.MarkerData marker, int y, int h) {
            double t = (marker.z() - minZ) / Math.max(1.0D, maxZ - minZ);
            return y + (int) Math.round(t * h);
        }
    }
}
