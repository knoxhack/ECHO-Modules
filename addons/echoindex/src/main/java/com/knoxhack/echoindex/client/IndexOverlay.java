package com.knoxhack.echoindex.client;

import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echoindex.Config;
import com.knoxhack.echoindex.EchoIndexClient;
import com.knoxhack.echoindex.network.IndexActionPacket;
import com.knoxhack.echoindex.service.ClientIndexState;
import com.knoxhack.echoindex.service.IndexRecipePlan;
import com.knoxhack.echoindex.service.IndexRecipePlanner;
import com.knoxhack.echoindex.service.IndexRecipeQueryClientState;
import com.knoxhack.echoindex.service.IndexRecipeSnapshot;
import com.knoxhack.echoindex.service.IndexService;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.mojang.blaze3d.platform.InputConstants;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public final class IndexOverlay {
    private static int BG = 0xE8060D13;
    private static int PANEL = IndexThemeStyle.FALLBACK_PANEL;
    private static int ROW = IndexThemeStyle.FALLBACK_ROW;
    private static int TEXT = IndexThemeStyle.FALLBACK_TEXT;
    private static int MUTED = IndexThemeStyle.FALLBACK_MUTED;
    private static int CYAN = IndexThemeStyle.FALLBACK_ACCENT;
    private static int WARN = IndexThemeStyle.FALLBACK_WARNING;
    private static final int HEADER_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 24;
    private static final int INNER_PAD = 10;
    private static final int GRID_SCROLLBAR_GUTTER = 8;
    private static final int OVERLAY_RESPONSIVE_MAX_COLUMNS = 32;
    private static final int SPLIT_DETAIL_MIN_WIDTH = 460;
    private static final int SPLIT_DETAIL_MIN_HEIGHT = 220;
    private static final int GROUP_HEADER_HEIGHT = 22;
    private static final int GROUP_BODY_PAD = 5;
    private static final int GROUP_GAP = 6;
    private static final int QUICK_JUMP_HEIGHT = 36;
    private static final int ICON_BUTTON_SIZE = 20;
    private static final int QUICK_JUMP_ICON_WIDTH = 20;
    private static final int QUICK_JUMP_ICON_HEIGHT = 18;
    private static final int GROUP_HEADER_ICON_SIZE = 20;
    private static final int SLOT_SIZE_COMPACT = 20;
    private static final int SLOT_SIZE_NORMAL = 22;
    private static final int SLOT_SIZE_LARGE = 26;
    private static final int SEARCH_DEBOUNCE_MS = 120;
    private static final List<Hitbox> HITBOXES = new ArrayList<>();
    private static final List<IndexRecipeUi.SlotHit> SLOT_HITS = new ArrayList<>();
    private static final Map<String, OverlayScreenState> SCREEN_STATES = new HashMap<>();
    private static final Map<String, PanelBounds> PANEL_BOUNDS = new HashMap<>();
    private static final Set<String> COLLAPSED_GROUPS = new HashSet<>();
    private static final Set<String> FULLY_EXPANDED_GROUPS = new HashSet<>();
    private static final Set<String> PINNED_GROUPS = new LinkedHashSet<>();
    private static final Set<String> HIDDEN_GROUPS = new LinkedHashSet<>();
    private static final Map<String, Integer> GROUP_OFFSETS = new HashMap<>();
    private static final List<HistoryEntry> DETAIL_HISTORY = new ArrayList<>();
    private static final int MAX_HISTORY = 16;

    private static String search = "";
    private static String pendingSearch = "";
    private static String debouncedSearch = "";
    private static long searchEditedAt;
    private static boolean searchFocused;
    private static boolean collapsed;
    private static int scroll;
    private static int horizontalScroll;
    private static int quickJumpPage;
    private static int panelX;
    private static int panelY;
    private static int panelW;
    private static int panelH;
    private static int lastGridX;
    private static int lastGridY;
    private static int lastGridW;
    private static int lastGridH;
    private static ItemStack hoveredStack = ItemStack.EMPTY;
    private static String categoryFilter = "";
    private static boolean bookmarkedOnly;
    private static boolean showHiddenMods;
    private static boolean showEmptyMods;
    private static boolean minecraftFirst = true;
    private static boolean echoPriority = true;
    private static String showOnlyGroup = "";
    private static int groupStateRevision;
    private static String activeScreenKey = "";
    private static Config.GridDensity gridDensity = Config.GridDensity.NORMAL;
    private static boolean gridDensityOverridden;
    private static ItemStack detailStack = ItemStack.EMPTY;
    private static IndexRecipeUi.ViewMode detailMode = IndexRecipeUi.ViewMode.RECIPES;
    private static Identifier detailCategory;
    private static int detailSelected;
    private static int historyIndex = -1;
    private static DragMode dragMode = DragMode.NONE;
    private static int dragMouseX;
    private static int dragMouseY;
    private static int dragPanelX;
    private static int dragPanelY;
    private static int dragPanelW;
    private static int dragPanelH;
    private static int dragThumbOffset;
    private static int lastMouseX;
    private static int lastMouseY;
    private static ScrollbarMetrics verticalScrollbar;
    private static ScrollbarMetrics horizontalScrollbar;
    private static long lastSyncRequestMillis;
    private static GridCacheKey gridCacheKey;
    private static List<ItemStack> gridCacheItems = List.of();
    private static GroupViewCacheKey groupViewCacheKey;
    private static List<IndexModGroup> groupViewCacheGroups = List.of();
    private static Set<String> currentMatchedItemKeys = Set.of();
    private static GroupMode groupMode = GroupMode.MOD;
    private static IndexViewMode indexViewMode = IndexViewMode.COMPACT;
    private static PopupKind popupKind = PopupKind.NONE;
    private static int popupX;
    private static int popupY;
    private static String popupGroupId = "";
    private static DetailBaseCacheKey detailBaseCacheKey;
    private static List<IndexRecipeView> detailBaseCacheViews = List.of();
    private static DetailCacheKey detailCacheKey;
    private static List<IndexRecipeView> detailCacheViews = List.of();
    private static final Map<ModeCountKey, Integer> MODE_COUNT_CACHE = new HashMap<>();

    private IndexOverlay() {
    }

    public static Map<String, Object> render(GuiGraphicsExtractor graphics, Object deltaTracker) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("surface", "INDEX");
        state.put("renderer", IndexOverlay.class.getName());
        state.put("rendered", false);
        if (graphics == null) {
            state.put("skipped", "missing_graphics");
            return Map.copyOf(state);
        }
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (!active(screen)) {
            saveScreenState();
            HITBOXES.clear();
            SLOT_HITS.clear();
            hoveredStack = ItemStack.EMPTY;
            verticalScrollbar = null;
            horizontalScrollbar = null;
            state.put("skipped", "inactive_screen");
            return Map.copyOf(state);
        }
        syncScreenState(screen);
        render(screen, graphics, lastMouseX, lastMouseY);
        state.put("rendered", true);
        state.put("screenClass", screen.getClass().getName());
        state.put("collapsed", collapsed);
        state.put("search", search);
        state.put("gridDensity", gridDensity.name());
        return Map.copyOf(state);
    }

    public static Map<String, Object> snapshot() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("surface", "INDEX");
        state.put("renderer", IndexOverlay.class.getName());
        state.put("activeScreenKey", activeScreenKey);
        state.put("collapsed", collapsed);
        state.put("search", search);
        state.put("pendingSearch", pendingSearch);
        state.put("debouncedSearch", debouncedSearch);
        state.put("searchFocused", searchFocused);
        state.put("bookmarkedOnly", bookmarkedOnly);
        state.put("categoryFilter", categoryFilter);
        state.put("groupMode", groupMode.name());
        state.put("viewMode", indexViewMode.name());
        state.put("gridDensity", gridDensity.name());
        state.put("panelX", panelX);
        state.put("panelY", panelY);
        state.put("panelWidth", panelW);
        state.put("panelHeight", panelH);
        state.put("scroll", scroll);
        state.put("horizontalScroll", horizontalScroll);
        state.put("hitboxCount", HITBOXES.size());
        state.put("slotHitCount", SLOT_HITS.size());
        state.put("detailItem", detailStack.isEmpty() ? "" : IndexService.itemId(detailStack.getItem()).toString());
        state.put("detailMode", detailMode.name());
        state.put("detailRecipeCount", detailCacheViews.size());
        state.put("historySize", DETAIL_HISTORY.size());
        state.put("pinnedGroups", List.copyOf(PINNED_GROUPS));
        state.put("hiddenGroups", List.copyOf(HIDDEN_GROUPS));
        state.put("collapsedGroups", List.copyOf(COLLAPSED_GROUPS));
        state.put("matchedItemCount", currentMatchedItemKeys.size());
        return Map.copyOf(state);
    }

    public static void onRender(Object event) {
        AbstractContainerScreen<?> screen = cast(call(event, "getContainerScreen"), AbstractContainerScreen.class);
        GuiGraphicsExtractor graphics = cast(call(event, "getGuiGraphics"), GuiGraphicsExtractor.class);
        if (screen == null || graphics == null) {
            return;
        }
        if (!active(screen)) {
            saveScreenState();
            HITBOXES.clear();
            SLOT_HITS.clear();
            hoveredStack = ItemStack.EMPTY;
            verticalScrollbar = null;
            horizontalScrollbar = null;
            return;
        }
        syncScreenState(screen);
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(-screen.getLeftPos(), -screen.getTopPos());
            render(screen, graphics, (int) Math.round(doubleCall(event, "getMouseX")),
                    (int) Math.round(doubleCall(event, "getMouseY")));
        } finally {
            graphics.pose().popMatrix();
        }
    }

    public static void onMouseClicked(Object event) {
        if (handleNativeRouteMouseClicked(cast(call(event, "getScreen"), Screen.class),
                doubleCall(event, "getMouseX"), doubleCall(event, "getMouseY"),
                intCall(event, "getButton"), intCall(call(event, "getMouseButtonEvent"), "modifiers"))) {
            setCanceled(event);
        }
    }

    public static void onMouseDragged(Object event) {
        if (handleNativeRouteMouseDragged(cast(call(event, "getScreen"), Screen.class),
                doubleCall(event, "getMouseX"), doubleCall(event, "getMouseY"))) {
            setCanceled(event);
        }
    }

    public static void onMouseReleased(Object event) {
        if (handleNativeRouteMouseReleased(cast(call(event, "getScreen"), Screen.class))) {
            setCanceled(event);
        }
    }

    public static void onMouseScrolled(Object event) {
        if (handleNativeRouteMouseScrolled(cast(call(event, "getScreen"), Screen.class),
                doubleCall(event, "getMouseX"), doubleCall(event, "getMouseY"),
                doubleCall(event, "getScrollDeltaY"))) {
            setCanceled(event);
        }
    }

    public static void onKeyPressed(Object event) {
        KeyEvent keyEvent = cast(call(event, "getKeyEvent"), KeyEvent.class);
        if (keyEvent != null && handleNativeRouteKeyPressed(cast(call(event, "getScreen"), Screen.class), keyEvent.key(),
                EchoIndexClient.SHOW_RECIPE_KEY.matches(keyEvent),
                EchoIndexClient.SHOW_USAGE_KEY.matches(keyEvent),
                EchoIndexClient.BOOKMARK_KEY.matches(keyEvent))) {
            setCanceled(event);
        }
    }

    public static void onCharTyped(Object event) {
        CharacterEvent character = cast(call(event, "getCharacterEvent"), CharacterEvent.class);
        if (handleNativeRouteCharTyped(
                cast(call(event, "getScreen"), Screen.class),
                character == null ? "" : character.codepointAsString(),
                character != null && character.isAllowedChatCharacter())) {
            setCanceled(event);
        }
    }

    public static boolean handleNativeRouteInput(String eventType, Map<String, Object> eventMetadata) {
        Screen screen = Minecraft.getInstance().screen;
        return switch (eventType) {
            case "mouse_clicked" -> handleNativeRouteMouseClicked(
                    screen,
                    doubleMetadata(eventMetadata, "mouseX"),
                    doubleMetadata(eventMetadata, "mouseY"),
                    intMetadata(eventMetadata, "button"),
                    intMetadata(eventMetadata, "modifiers"));
            case "mouse_dragged" -> handleNativeRouteMouseDragged(
                    screen,
                    doubleMetadata(eventMetadata, "mouseX"),
                    doubleMetadata(eventMetadata, "mouseY"));
            case "mouse_released" -> handleNativeRouteMouseReleased(screen);
            case "mouse_scrolled" -> handleNativeRouteMouseScrolled(
                    screen,
                    doubleMetadata(eventMetadata, "mouseX"),
                    doubleMetadata(eventMetadata, "mouseY"),
                    doubleMetadata(eventMetadata, "scrollDeltaY"));
            case "key_pressed" -> handleNativeRouteKeyPressed(
                    screen,
                    intMetadata(eventMetadata, "key"),
                    booleanMetadata(eventMetadata, "recipeKey"),
                    booleanMetadata(eventMetadata, "usageKey"),
                    booleanMetadata(eventMetadata, "bookmarkKey"));
            case "character_typed" -> handleNativeRouteCharTyped(
                    screen,
                    text(eventMetadata.get("character")),
                    booleanMetadata(eventMetadata, "allowedChatCharacter"));
            default -> false;
        };
    }

    private static boolean handleNativeRouteMouseClicked(
            Screen screen,
            double mouseX,
            double mouseY,
            int button,
            int modifiers
    ) {
        if (!active(screen) || !inside(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && beginScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        List<Hitbox> hitboxes = List.copyOf(HITBOXES);
        for (int index = hitboxes.size() - 1; index >= 0; index--) {
            Hitbox hitbox = hitboxes.get(index);
            if (inside(mouseX, mouseY, hitbox.x(), hitbox.y(), hitbox.w(), hitbox.h())) {
                hitbox.action().click(button, modifiers);
                saveScreenState();
                return true;
            }
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && beginPanelDrag(mouseX, mouseY)) {
            saveScreenState();
            return true;
        }
        searchFocused = false;
        closePopup();
        saveScreenState();
        return true;
    }

    private static boolean handleNativeRouteMouseDragged(Screen screen, double mouseX, double mouseY) {
        if (!active(screen) || dragMode == DragMode.NONE) {
            return false;
        }
        int dx = (int) Math.round(mouseX - dragMouseX);
        int dy = (int) Math.round(mouseY - dragMouseY);
        if (dragMode == DragMode.MOVE) {
            panelX = clamp(dragPanelX + dx, 4, Math.max(4, screen.width - panelW - 4));
            panelY = clamp(dragPanelY + dy, 4, Math.max(4, screen.height - panelH - 4));
            storePanelBounds();
        } else if (dragMode == DragMode.RESIZE) {
            panelW = clamp(dragPanelW + dx, 160, Math.max(160, screen.width - panelX - 4));
            panelH = clamp(dragPanelH + dy, 180, Math.max(180, screen.height - panelY - 4));
            storePanelBounds();
        } else if (dragMode == DragMode.VERTICAL_SCROLL || dragMode == DragMode.HORIZONTAL_SCROLL) {
            updateScrollbarDrag(mouseX, mouseY);
        }
        return true;
    }

    private static boolean handleNativeRouteMouseReleased(Screen screen) {
        if (!active(screen) || dragMode == DragMode.NONE) {
            return false;
        }
        boolean panelDrag = dragMode == DragMode.MOVE || dragMode == DragMode.RESIZE;
        dragMode = DragMode.NONE;
        if (panelDrag) {
            storePanelBounds();
        }
        saveScreenState();
        return true;
    }

    private static boolean handleNativeRouteMouseScrolled(
            Screen screen,
            double mouseX,
            double mouseY,
            double scrollDeltaY
    ) {
        if (!active(screen) || collapsed || !inside(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
            return false;
        }
        boolean overGrid = inside(mouseX, mouseY, lastGridX, lastGridY, lastGridW, lastGridH);
        if (!detailStack.isEmpty() && !overGrid) {
            for (IndexRecipeUi.SlotHit hit : List.copyOf(SLOT_HITS)) {
                if (hit.choiceCyclable() && inside(mouseX, mouseY, hit.x(), hit.y(), hit.w(), hit.h())) {
                    IndexRecipeUi.cycleChoice(hit, scrollDeltaY > 0 ? 1 : -1);
                    saveScreenState();
                    return true;
                }
            }
            int max = Math.max(0, detailViews().size() - 1);
            detailSelected = clamp(detailSelected - (int) Math.round(scrollDeltaY), 0, max);
        } else {
            if (shiftDown()) {
                horizontalScroll = Math.max(0, horizontalScroll - (int) Math.round(scrollDeltaY * 26.0D));
            } else {
                scroll = Math.max(0, scroll - (int) Math.round(scrollDeltaY * 26.0D));
            }
        }
        saveScreenState();
        return true;
    }

    private static boolean handleNativeRouteKeyPressed(
            Screen screen,
            int key,
            boolean recipeKey,
            boolean usageKey,
            boolean bookmarkKey
    ) {
        if (!active(screen)) {
            return false;
        }
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) {
                    search = search.substring(0, search.offsetByCodePoints(search.length(), -1));
                    markSearchEdited();
                    resetGridScroll();
                }
            } else if (key == GLFW.GLFW_KEY_ESCAPE) {
                if (!search.isEmpty()) {
                    search = "";
                    markSearchEdited();
                    resetGridScroll();
                } else {
                    searchFocused = false;
                }
            } else if (key == GLFW.GLFW_KEY_ENTER) {
                commitSearchNow();
                List<ItemStack> items = gridItems();
                if (!items.isEmpty()) {
                    openDetail(items.getFirst(), IndexRecipeUi.ViewMode.RECIPES);
                }
                searchFocused = false;
            }
            saveScreenState();
            return true;
        }
        if (screenHasFocusedInput(screen)) {
            return false;
        }
        if (recipeKey) {
            ItemStack hoveredInventoryStack = hoveredInventoryStack(screen);
            if (!hoveredInventoryStack.isEmpty()) {
                openRecipeScreen(hoveredInventoryStack, IndexRecipeScreen.Mode.RECIPES, "index_overlay_recipe_key");
                return true;
            }
        }
        if (usageKey) {
            ItemStack hoveredInventoryStack = hoveredInventoryStack(screen);
            if (!hoveredInventoryStack.isEmpty()) {
                openRecipeScreen(hoveredInventoryStack, IndexRecipeScreen.Mode.USES, "index_overlay_usage_key");
                return true;
            }
        }
        if (bookmarkKey && !hoveredStack.isEmpty()) {
            toggleBookmark(IndexService.itemId(hoveredStack.getItem()));
            return true;
        }
        if (!detailStack.isEmpty() && key == GLFW.GLFW_KEY_B) {
            toggleBookmark(IndexService.itemId(detailStack.getItem()));
            return true;
        }
        if (!detailStack.isEmpty() && key == GLFW.GLFW_KEY_P) {
            toggleFocusedRecipePin();
            return true;
        }
        if (!detailStack.isEmpty() && key == GLFW.GLFW_KEY_R) {
            setDetailMode(IndexRecipeUi.ViewMode.RECIPES);
            return true;
        }
        if (!detailStack.isEmpty() && key == GLFW.GLFW_KEY_U) {
            setDetailMode(IndexRecipeUi.ViewMode.USES);
            return true;
        }
        if (!detailStack.isEmpty() && key == GLFW.GLFW_KEY_S) {
            setDetailMode(IndexRecipeUi.ViewMode.SOURCES);
            return true;
        }
        if (recipeKey && !hoveredStack.isEmpty()) {
            openDetail(hoveredStack, IndexRecipeUi.ViewMode.RECIPES);
            return true;
        }
        if (usageKey && !hoveredStack.isEmpty()) {
            openDetail(hoveredStack, IndexRecipeUi.ViewMode.USES);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && !detailStack.isEmpty() && !searchFocused) {
            closeDetail();
            return true;
        }
        if (!detailStack.isEmpty() && key == GLFW.GLFW_KEY_LEFT) {
            if (historyBack()) {
                saveScreenState();
            } else {
                detailSelected = Math.max(0, detailSelected - 1);
            }
            return true;
        }
        if (!detailStack.isEmpty() && key == GLFW.GLFW_KEY_RIGHT) {
            if (historyForward()) {
                saveScreenState();
            } else {
                detailSelected = Math.min(Math.max(0, detailViews().size() - 1), detailSelected + 1);
            }
            return true;
        }
        if (detailStack.isEmpty() && key == GLFW.GLFW_KEY_LEFT) {
            horizontalScroll = Math.max(0, horizontalScroll - gridStep());
            return true;
        }
        if (detailStack.isEmpty() && key == GLFW.GLFW_KEY_RIGHT) {
            horizontalScroll += gridStep();
            return true;
        }
        return false;
    }

    private static boolean handleNativeRouteCharTyped(Screen screen, String character, boolean allowedChatCharacter) {
        if (!active(screen) || !searchFocused) {
            return false;
        }
        if (allowedChatCharacter && search.length() < 80) {
            search += character == null ? "" : character;
            markSearchEdited();
            resetGridScroll();
            return true;
        }
        return false;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static double doubleMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? 0.0D : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0.0D;
        }
    }

    private static boolean booleanMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    private static Object call(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static int intCall(Object target, String methodName) {
        Object value = call(target, methodName);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static double doubleCall(Object target, String methodName) {
        Object value = call(target, methodName);
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }

    private static void setCanceled(Object event) {
        if (event == null) {
            return;
        }
        try {
            event.getClass().getMethod("setCanceled", boolean.class).invoke(event, true);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static <T> T cast(Object value, Class<T> type) {
        return type.isInstance(value) ? type.cast(value) : null;
    }

    private static void render(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        refreshThemeAliases();
        HITBOXES.clear();
        SLOT_HITS.clear();
        hoveredStack = ItemStack.EMPTY;
        verticalScrollbar = null;
        horizontalScrollbar = null;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        layout(screen);
        Font font = Minecraft.getInstance().font;
        int veilAlpha = collapsed ? 16 : 24;
        EchoCyberGlassUi.focusVeil(graphics, screen.width, screen.height, panelX, panelY, panelW, panelH, veilAlpha);
        if (collapsed) {
            graphics.fill(panelX, panelY, panelX + 22, panelY + 86, 0xD8071117);
            EchoCyberGlassUi.calmFrame(graphics, panelX, panelY, 22, 86, CYAN);
            graphics.text(font, text("screen.echoindex.overlay.collapsed"), panelX + 3, panelY + 8, CYAN, false);
            tooltipIfHovered(graphics, font, mouseX, mouseY, panelX, panelY, 22, 86,
                    tr("screen.echoindex.overlay.tooltip.expand"));
            HITBOXES.add(new Hitbox(panelX, panelY, 22, 86, (button, modifiers) -> collapsed = false));
            return;
        }

        drawPanelChrome(graphics);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + HEADER_HEIGHT, 0x22163843);
        IndexThemeStyle.icon(graphics, panelX + 8, panelY + 6, 16);
        graphics.text(font, text("screen.echoindex.overlay.title"), panelX + 28, panelY + 9, CYAN, false);
        int densityX = panelX + panelW - 68;
        button(graphics, font, densityX, panelY + 5, 18, 16, densityLabel(), true);
        tooltipIfHovered(graphics, font, mouseX, mouseY, densityX, panelY + 5, 18, 16,
                tr("screen.echoindex.overlay.tooltip.density"),
                tr("screen.echoindex.overlay.tooltip.density.current", densityName()));
        HITBOXES.add(new Hitbox(densityX, panelY + 5, 18, 16, (button, modifiers) -> cycleGridDensity()));
        int refreshX = panelX + panelW - 46;
        button(graphics, font, refreshX, panelY + 5, 18, 16, text("screen.echoindex.overlay.button.refresh"), true);
        tooltipIfHovered(graphics, font, mouseX, mouseY, refreshX, panelY + 5, 18, 16,
                tr("screen.echoindex.overlay.tooltip.refresh"));
        HITBOXES.add(new Hitbox(refreshX, panelY + 5, 18, 16,
                (button, modifiers) -> {
                    IndexService.INSTANCE.rebuildRecipes(Minecraft.getInstance().player, "overlay refresh button");
                    requestServerSync(true);
                }));
        int collapseX = panelX + panelW - 24;
        button(graphics, font, collapseX, panelY + 5, 16, 16, text("screen.echoindex.overlay.button.collapse"), true);
        tooltipIfHovered(graphics, font, mouseX, mouseY, collapseX, panelY + 5, 16, 16,
                tr("screen.echoindex.overlay.tooltip.collapse"));
        HITBOXES.add(new Hitbox(collapseX, panelY + 5, 16, 16, (button, modifiers) -> collapsed = true));

        int searchX = panelX + INNER_PAD;
        int searchY = panelY + HEADER_HEIGHT + 6;
        int searchW = panelW - INNER_PAD * 2;
        drawSearch(graphics, font, searchX, searchY, searchW);
        int controlsY = searchY + 25;
        drawIndexControls(graphics, font, searchX, controlsY, searchW, mouseX, mouseY);
        List<IndexModGroup> groups = visibleGroups();
        int quickJumpY = controlsY + 24;
        drawQuickJump(graphics, font, searchX, quickJumpY, searchW, groups, mouseX, mouseY);
        int bodyY = quickJumpY + QUICK_JUMP_HEIGHT + 6;
        int bodyH = Math.max(48, panelH - (bodyY - panelY) - FOOTER_HEIGHT - 4);

        if (detailStack.isEmpty()) {
            renderGroupedBrowser(graphics, font, mouseX, mouseY, searchX, bodyY, searchW, bodyH, groups);
        } else if (searchW >= SPLIT_DETAIL_MIN_WIDTH && bodyH >= SPLIT_DETAIL_MIN_HEIGHT) {
            renderSplitDetail(graphics, font, mouseX, mouseY, searchX, bodyY, searchW, bodyH, groups);
        } else {
            renderDetail(graphics, font, mouseX, mouseY, searchX, bodyY, searchW, bodyH);
        }
        drawFooter(graphics, font, searchX, mouseX, mouseY);
        drawPopup(graphics, font, mouseX, mouseY);
        graphics.outline(panelX + panelW - 12, panelY + panelH - 12, 8, 8,
                dragMode == DragMode.RESIZE ? CYAN : 0x6638DFF4);
        tooltipIfHovered(graphics, font, mouseX, mouseY, panelX + panelW - 14, panelY + panelH - 14, 14, 14,
                tr("screen.echoindex.overlay.tooltip.resize"));
    }

    private static void drawPanelChrome(GuiGraphicsExtractor graphics) {
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, IndexThemeStyle.alpha(BG, 232));
        if (cinematicStyle() && panelW >= 430 && panelH >= 300) {
            renderCoreFrame(graphics, panelX, panelY, panelW, panelH);
        } else {
            EchoCyberGlassUi.calmFrame(graphics, panelX, panelY, panelW, panelH, CYAN);
            graphics.fill(panelX, panelY, panelX + Math.max(38, panelW / 5), panelY + 1, 0xAA38DFF4);
            graphics.fill(panelX, panelY + panelH - 1, panelX + Math.max(24, panelW / 7), panelY + panelH, 0x7738DFF4);
        }
    }

    private static void drawSearch(GuiGraphicsExtractor graphics, Font font, int x, int y, int w) {
        graphics.fill(x, y, x + w, y + 19, 0xDD05090E);
        graphics.outline(x, y, w, 19, searchFocused ? CYAN : 0x6638DFF4);
        int clearW = search.isBlank() ? 0 : 16;
        int textW = Math.max(16, w - 18 - clearW);
        String label = search.isBlank() && !searchFocused ? text("screen.echoindex.search") : search + (searchFocused ? "_" : "");
        graphics.text(font, trim(font, label, textW), x + 6, y + 6, search.isBlank() ? MUTED : TEXT, false);
        if (search.isBlank()) {
            graphics.text(font, "*", x + w - 12, y + 5, searchFocused ? CYAN : MUTED, false);
            HITBOXES.add(new Hitbox(x, y, w, 19, (button, modifiers) -> searchFocused = true));
        } else {
            int clearX = x + w - 17;
            boolean hover = inside(lastMouseX, lastMouseY, clearX, y + 2, 15, 15);
            graphics.fill(clearX, y + 2, clearX + 15, y + 17, hover ? 0xAA123241 : 0x66071117);
            graphics.outline(clearX, y + 2, 15, 15, hover ? CYAN : 0x5538DFF4);
            graphics.centeredText(font, "X", clearX + 7, y + 6, hover ? TEXT : MUTED);
            tooltipIfHovered(graphics, font, lastMouseX, lastMouseY, clearX, y + 2, 15, 15,
                    tr("screen.echoindex.overlay.tooltip.clear_search"));
            HITBOXES.add(new Hitbox(x, y, Math.max(0, clearX - x), 19, (button, modifiers) -> searchFocused = true));
            HITBOXES.add(new Hitbox(clearX, y + 2, 15, 15, (button, modifiers) -> clearSearch()));
        }
    }

    private static void drawIndexControls(GuiGraphicsExtractor graphics, Font font, int x, int y, int w,
            int mouseX, int mouseY) {
        int cx = x;
        itemIconButton(graphics, font, cx, y, new ItemStack(Items.COMPASS),
                search.isBlank() && categoryFilter.isBlank() && !bookmarkedOnly && showOnlyGroup.isBlank(), mouseX, mouseY,
                tr("screen.echoindex.overlay.tooltip.filter_all"),
                (button, modifiers) -> clearFilters(true));
        cx += 24;
        itemIconButton(graphics, font, cx, y, new ItemStack(Items.CRAFTING_TABLE),
                "$blocks".equals(categoryFilter), mouseX, mouseY,
                tr("screen.echoindex.overlay.tooltip.filter_blocks"),
                (button, modifiers) -> setFilter("$blocks"));
        cx += 24;
        itemIconButton(graphics, font, cx, y, new ItemStack(Items.IRON_PICKAXE),
                "$tools".equals(categoryFilter), mouseX, mouseY,
                tr("screen.echoindex.overlay.tooltip.filter_tools"),
                (button, modifiers) -> setFilter("$tools"));
        cx += 24;
        itemIconButton(graphics, font, cx, y, new ItemStack(Items.IRON_SWORD),
                "$combat".equals(categoryFilter), mouseX, mouseY,
                tr("screen.echoindex.overlay.tooltip.filter_combat"),
                (button, modifiers) -> setFilter("$combat"));
        cx += 28;

        int sortW = Math.min(78, Math.max(58, font.width(text("screen.echoindex.overlay.sort.mod")) + 18));
        int viewW = Math.min(92, Math.max(70, font.width(viewLabel()) + 18));
        int sortX = Math.max(cx, x + w - sortW - viewW - 8);
        compactButton(graphics, font, sortX, y, sortW, 20, sortLabel(), true, mouseX, mouseY);
        tooltipIfHovered(graphics, font, mouseX, mouseY, sortX, y, sortW, 20,
                tr("screen.echoindex.overlay.tooltip.group_mode"));
        HITBOXES.add(new Hitbox(sortX, y, sortW, 20, (button, modifiers) -> {
            groupMode = groupMode == GroupMode.MOD ? GroupMode.CATEGORY : GroupMode.MOD;
            markGroupStateChanged();
            resetGridScroll();
            closePopup();
        }));
        int viewX = sortX + sortW + 8;
        compactButton(graphics, font, viewX, y, viewW, 20, viewLabel(), true, mouseX, mouseY);
        tooltipIfHovered(graphics, font, mouseX, mouseY, viewX, y, viewW, 20,
                tr("screen.echoindex.overlay.tooltip.view_mode"));
        HITBOXES.add(new Hitbox(viewX, y, viewW, 20, (button, modifiers) -> {
            indexViewMode = indexViewMode == IndexViewMode.COMPACT ? IndexViewMode.DETAILED : IndexViewMode.COMPACT;
            markGroupStateChanged();
            closePopup();
        }));
    }

    private static void itemIconButton(GuiGraphicsExtractor graphics, Font font, int x, int y, ItemStack icon,
            boolean selected, int mouseX, int mouseY, Component tooltip, ClickAction action) {
        boolean hover = inside(mouseX, mouseY, x, y, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE);
        slimIconSurface(graphics, x, y, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE, hover, selected, CYAN);
        if (icon != null && !icon.isEmpty()) {
            drawScaledItem(graphics, font, icon, x, y, ICON_BUTTON_SIZE, false);
        }
        if (tooltip != null && hover) {
            graphics.setComponentTooltipForNextFrame(font, List.of(tooltip), mouseX, mouseY);
        }
        HITBOXES.add(new Hitbox(x, y, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE, action));
    }

    private static void drawQuickJump(GuiGraphicsExtractor graphics, Font font, int x, int y, int w,
            List<IndexModGroup> groups, int mouseX, int mouseY) {
        graphics.fill(x, y, x + w, y + QUICK_JUMP_HEIGHT, 0x99071117);
        graphics.outline(x, y, w, QUICK_JUMP_HEIGHT, 0xAA38DFF4);
        graphics.text(font, text("screen.echoindex.overlay.quick_jump"), x + 6, y + 4, CYAN, false);
        int iconY = y + 14;
        int iconX = x + 8;
        int maxIcons = Math.max(1, (w - 34) / 24);
        int start = clamp(quickJumpPage * maxIcons, 0, Math.max(0, groups.size() - 1));
        int end = Math.min(groups.size(), start + maxIcons);
        for (int index = start; index < end; index++) {
            IndexModGroup group = groups.get(index);
            boolean selected = inside(mouseX, mouseY, iconX, iconY, QUICK_JUMP_ICON_WIDTH, QUICK_JUMP_ICON_HEIGHT);
            int accent = group.accentColor() == null ? CYAN : group.accentColor();
            slimIconSurface(graphics, iconX, iconY, QUICK_JUMP_ICON_WIDTH, QUICK_JUMP_ICON_HEIGHT,
                    selected, selected, accent);
            if (!group.iconItem().isEmpty()) {
                drawScaledItem(graphics, font, group.iconItem(), iconX + 1, iconY,
                        Math.min(QUICK_JUMP_ICON_WIDTH, QUICK_JUMP_ICON_HEIGHT), false);
            }
            tooltipIfHovered(graphics, font, mouseX, mouseY, iconX, iconY, QUICK_JUMP_ICON_WIDTH, QUICK_JUMP_ICON_HEIGHT,
                    Component.literal(group.displayName()),
                    Component.literal(group.modId()));
            HITBOXES.add(new Hitbox(iconX, iconY, QUICK_JUMP_ICON_WIDTH, QUICK_JUMP_ICON_HEIGHT, (button, modifiers) -> {
                scroll = Math.max(0, GROUP_OFFSETS.getOrDefault(group.modId(), 0));
                closePopup();
            }));
            iconX += 24;
        }
        if (end < groups.size()) {
            int arrowX = x + w - 24;
            compactButton(graphics, font, arrowX, iconY, 18, 18, ">", true, mouseX, mouseY);
            HITBOXES.add(new Hitbox(arrowX, iconY, 18, 18, (button, modifiers) -> {
                quickJumpPage++;
                if (quickJumpPage * maxIcons >= groups.size()) {
                    quickJumpPage = 0;
                }
            }));
        }
    }

    private static int drawFilterButtons(GuiGraphicsExtractor graphics, Font font, int x, int y, int width,
            int mouseX, int mouseY) {
        ChipCursor cursor = new ChipCursor(x, y);
        cursor = filterChip(graphics, font, x, cursor, width, 34, "All",
                categoryFilter.isBlank() && !bookmarkedOnly, mouseX, mouseY,
                (button, modifiers) -> clearFilters(false));
        cursor = filterChip(graphics, font, x, cursor, width, 48, "Blocks", "$blocks".equals(categoryFilter),
                mouseX, mouseY, (button, modifiers) -> setFilter("$blocks"));
        cursor = filterChip(graphics, font, x, cursor, width, 62, "Machines", "$machines".equals(categoryFilter),
                mouseX, mouseY, (button, modifiers) -> setFilter("$machines"));
        cursor = filterChip(graphics, font, x, cursor, width, 42, "Tools", "$tools".equals(categoryFilter),
                mouseX, mouseY, (button, modifiers) -> setFilter("$tools"));
        cursor = filterChip(graphics, font, x, cursor, width, 56, "Combat", "$combat".equals(categoryFilter),
                mouseX, mouseY, (button, modifiers) -> setFilter("$combat"));
        cursor = filterChip(graphics, font, x, cursor, width, 46, "ECHO", "$echo".equals(categoryFilter),
                mouseX, mouseY, (button, modifiers) -> setFilter("$echo"));
        cursor = filterChip(graphics, font, x, cursor, width, 26, "*", bookmarkedOnly, mouseX, mouseY,
                (button, modifiers) -> {
                    bookmarkedOnly = !bookmarkedOnly;
                    resetGridScroll();
                });
        cursor = filterChip(graphics, font, x, cursor, width, 48, "Clear", false, mouseX, mouseY,
                (button, modifiers) -> clearFilters(true));
        return cursor.y() + 17;
    }

    private static ChipCursor filterChip(GuiGraphicsExtractor graphics, Font font, int rowX, ChipCursor cursor, int rowW,
            int preferredW, String label, boolean selected, int mouseX, int mouseY, ClickAction action) {
        int w = Math.min(Math.max(24, rowW), Math.max(preferredW, font.width(label) + 12));
        int cx = cursor.x();
        int cy = cursor.y();
        if (cx > rowX && cx + w > rowX + rowW) {
            cx = rowX;
            cy += 19;
        }
        chip(graphics, font, cx, cy, w, label, selected, mouseX, mouseY);
        HITBOXES.add(new Hitbox(cx, cy, w, 17, action));
        return new ChipCursor(cx + w + 4, cy);
    }

    private static int drawActiveTokens(GuiGraphicsExtractor graphics, Font font, int x, int y, int w) {
        List<String> tokens = new ArrayList<>();
        if (!search.isBlank()) {
            tokens.add("\"" + search.trim() + "\"");
        }
        if (!categoryFilter.isBlank()) {
            tokens.add(categoryFilter);
        }
        if (bookmarkedOnly) {
            tokens.add("bookmarked");
        }
        if (tokens.isEmpty()) {
            return 0;
        }
        graphics.text(font, trim(font, "Filters: " + String.join("  ", tokens), w), x, y, MUTED, false);
        return 12;
    }

    private static void renderGrid(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY,
            int gridX, int gridY, int gridW, int gridH) {
        lastGridX = gridX;
        lastGridY = gridY;
        lastGridW = gridW;
        lastGridH = gridH;
        List<ItemStack> items = gridItems();
        GridLayout grid = gridLayout(gridW, gridH, items.size());
        int step = grid.step();
        int slot = grid.slot();
        int columns = grid.columns();
        int rows = (items.size() + columns - 1) / columns;
        int contentH = rows * step;
        int maxVerticalScroll = Math.max(0, contentH - gridH);
        scroll = clamp(scroll, 0, maxVerticalScroll);
        horizontalScroll = 0;
        graphics.enableScissor(gridX, gridY, gridX + gridW, gridY + gridH);
        int startY = gridY - scroll;
        int firstRow = Math.max(0, scroll / step);
        int lastRow = Math.min(Math.max(0, rows - 1), (scroll + gridH) / step + 1);
        for (int row = firstRow; row <= lastRow; row++) {
            int y = startY + row * step;
            if (y < gridY - step || y > gridY + gridH) {
                continue;
            }
            for (int column = 0; column < columns; column++) {
                int i = row * columns + column;
                if (i >= items.size()) {
                    break;
                }
                ItemStack stack = items.get(i);
                int x = gridX + grid.columnOffset(column);
                itemSlot(graphics, font, stack, x, y, slot, mouseX, mouseY);
                Identifier itemId = IndexService.itemId(stack.getItem());
                if (ClientIndexState.isBookmarked(itemId)) {
                    graphics.text(font, "*", x + Math.max(12, slot - 5), y - 1, WARN, false);
                }
                HITBOXES.add(new Hitbox(x, y, slot, slot, (button, modifiers) -> {
                    if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 || button == 2) {
                        toggleBookmark(itemId);
                    } else {
                        openDetail(stack, button == 1 ? IndexRecipeUi.ViewMode.USES : IndexRecipeUi.ViewMode.RECIPES);
                    }
                }));
            }
        }
        graphics.disableScissor();
        drawGridScrollbars(graphics, font, gridX, gridY, gridW, gridH, gridW, contentH);
        if (items.isEmpty()) {
            graphics.text(font, text("screen.echoindex.no_results"), gridX, gridY + 8, MUTED, false);
        }
    }

    private static void renderGroupedBrowser(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY,
            int gridX, int gridY, int gridW, int gridH, List<IndexModGroup> groups) {
        lastGridX = gridX;
        lastGridY = gridY;
        lastGridW = gridW;
        lastGridH = gridH;
        GroupMetrics metrics = groupMetrics(groups, gridW);
        int contentH = metrics.contentHeight();
        int maxVerticalScroll = Math.max(0, contentH - gridH);
        scroll = clamp(scroll, 0, maxVerticalScroll);
        horizontalScroll = 0;
        graphics.enableScissor(gridX, gridY, gridX + gridW, gridY + gridH);
        for (GroupLayout layout : metrics.layouts()) {
            if (layout.y() + layout.height() < scroll - GROUP_GAP || layout.y() > scroll + gridH + GROUP_GAP) {
                continue;
            }
            drawGroupSection(graphics, font, layout, gridX, gridY, gridW, mouseX, mouseY);
        }
        graphics.disableScissor();
        drawGridScrollbars(graphics, font, gridX, gridY, gridW, gridH, gridW, contentH);
        if (groups.isEmpty()) {
            graphics.text(font, text("screen.echoindex.no_results"), gridX, gridY + 8, MUTED, false);
        }
    }

    private static GroupMetrics groupMetrics(List<IndexModGroup> groups, int width) {
        GROUP_OFFSETS.clear();
        int slot = groupedSlotSize();
        int step = groupedStep();
        int columns = groupedColumns(width);
        int y = 0;
        List<GroupLayout> layouts = new ArrayList<>();
        for (IndexModGroup group : groups) {
            GROUP_OFFSETS.put(group.modId(), y);
            int totalRows = (group.visibleItems().size() + columns - 1) / columns;
            boolean fullExpanded = FULLY_EXPANDED_GROUPS.contains(group.modId());
            int rows = group.isCollapsed()
                    ? 0
                    : indexViewMode == IndexViewMode.DETAILED || fullExpanded ? totalRows : Math.min(2, totalRows);
            int bodyH = rows <= 0 ? 0 : GROUP_BODY_PAD * 2 + rows * step;
            int height = GROUP_HEADER_HEIGHT + bodyH;
            layouts.add(new GroupLayout(group, y, height, rows, columns, slot, step, fullExpanded));
            y += height + GROUP_GAP;
        }
        return new GroupMetrics(layouts, Math.max(0, y - GROUP_GAP));
    }

    private static void drawGroupSection(GuiGraphicsExtractor graphics, Font font, GroupLayout layout,
            int gridX, int gridY, int gridW, int mouseX, int mouseY) {
        IndexModGroup group = layout.group();
        int x = gridX;
        int y = gridY + layout.y() - scroll;
        int h = layout.height();
        boolean headerHover = inside(mouseX, mouseY, x, y, gridW, GROUP_HEADER_HEIGHT);
        int accent = group.accentColor() == null ? CYAN : group.accentColor();
        int outline = group.isCollapsed() ? 0x5538DFF4 : headerHover ? 0xCC66E8FF : IndexThemeStyle.alpha(accent, 150);
        graphics.fill(x, y, x + gridW, y + h, group.isHidden() ? 0x52050A0F : 0x7A071117);
        graphics.outline(x, y, gridW, h, outline);
        graphics.fill(x + 1, y + 1, x + gridW - 1, y + GROUP_HEADER_HEIGHT, headerHover ? 0x88123241 : 0x66102630);
        if (headerHover) {
            graphics.fill(x + 1, y + GROUP_HEADER_HEIGHT - 1, x + gridW - 1, y + GROUP_HEADER_HEIGHT, 0xAA38DFF4);
        }
        slimIconSurface(graphics, x + 1, y + 1, GROUP_HEADER_ICON_SIZE, GROUP_HEADER_ICON_SIZE,
                headerHover, false, accent);
        if (!group.iconItem().isEmpty()) {
            drawScaledItem(graphics, font, group.iconItem(), x + 1, y + 1, GROUP_HEADER_ICON_SIZE, false);
        }
        int nameX = x + 26;
        if (group.isPinned()) {
            graphics.text(font, "*", nameX, y + 7, WARN, false);
            nameX += 8;
        }
        String count = headerCountLabel(group);
        int chevronX = x + gridW - 14;
        int countMax = Math.min(54, Math.max(28, gridW / 5));
        int countW = Math.min(countMax, Math.max(24, font.width(count)));
        int countX = chevronX - countW - 6;
        int allX = countX - 40;
        graphics.text(font, trim(font, group.displayName().toUpperCase(Locale.ROOT),
                Math.max(24, countX - nameX - 6)), nameX, y + 7,
                group.isHidden() ? MUTED : CYAN, false);
        graphics.text(font, trim(font, count, countW), countX, y + 7, group.isHidden() ? MUTED : TEXT, false);
        if (layout.fullExpanded() && indexViewMode == IndexViewMode.COMPACT && allX > nameX + 42) {
            int allW = Math.min(34, Math.max(24, font.width("All") + 8));
            graphics.fill(allX, y + 4, allX + allW, y + 17, 0x44102630);
            graphics.outline(allX, y + 4, allW, 13, IndexThemeStyle.alpha(accent, 170));
            graphics.centeredText(font, "All", allX + allW / 2, y + 8, accent);
        }
        graphics.text(font, groupChevron(layout), chevronX, y + 7, CYAN, false);
        tooltipIfHovered(graphics, font, mouseX, mouseY, x, y, gridW, GROUP_HEADER_HEIGHT,
                Component.literal(group.displayName()),
                Component.literal("Mod ID: " + group.modId()),
                Component.literal("Items: " + group.itemCount()),
                Component.literal(group.version().isBlank() ? "Version: unknown" : "Version: " + group.version()),
                Component.literal("State: " + groupStateLabel(layout)),
                Component.literal("Click: " + groupNextClickLabel(layout)));
        HITBOXES.add(new Hitbox(x, y, gridW, GROUP_HEADER_HEIGHT, (button, modifiers) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                openGroupPopup(group.modId(), mouseX, mouseY);
            } else if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
                expandOnlyGroup(group.modId(), gridW);
            } else {
                cycleGroupExpansion(group.modId(), gridW);
            }
        }));

        if (layout.rows() <= 0) {
            return;
        }
        int bodyY = y + GROUP_HEADER_HEIGHT;
        int startX = x + GROUP_BODY_PAD;
        int startY = bodyY + GROUP_BODY_PAD;
        int visibleSlots = Math.min(group.visibleItems().size(), layout.rows() * layout.columns());
        int firstRow = clamp((gridY - startY - layout.slot()) / layout.step(), 0, Math.max(0, layout.rows() - 1));
        int lastRow = clamp((gridY + lastGridH - startY) / layout.step() + 1, 0, Math.max(0, layout.rows() - 1));
        for (int row = firstRow; row <= lastRow; row++) {
            int itemY = startY + row * layout.step();
            for (int column = 0; column < layout.columns(); column++) {
                int index = row * layout.columns() + column;
                if (index >= visibleSlots) {
                    break;
                }
                int itemX = startX + column * layout.step();
                ItemStack stack = group.visibleItems().get(index);
                itemSlot(graphics, font, stack, itemX, itemY, layout.slot(), mouseX, mouseY);
                String key = stackKey(stack);
                if (activeBrowseFilter() && currentMatchedItemKeys.contains(key)) {
                    graphics.outline(itemX - 1, itemY - 1, layout.slot() + 2, layout.slot() + 2, 0xDD66E8FF);
                }
                Identifier itemId = IndexService.itemId(stack.getItem());
                if (ClientIndexState.isBookmarked(itemId)) {
                    graphics.text(font, "*", itemX + Math.max(12, layout.slot() - 5), itemY - 1, WARN, false);
                }
                HITBOXES.add(new Hitbox(itemX, itemY, layout.slot(), layout.slot(), (button, modifiers) -> {
                    if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                        toggleBookmark(itemId);
                    } else {
                        openDetail(stack, button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                                ? IndexRecipeUi.ViewMode.USES
                                : IndexRecipeUi.ViewMode.RECIPES);
                    }
                    closePopup();
                }));
            }
        }
    }

    private static void renderSplitDetail(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY,
            int x, int y, int w, int h, List<IndexModGroup> groups) {
        int gridW = Math.min(220, Math.max(150, w * 36 / 100));
        if (w - gridW < 300) {
            gridW = Math.max(140, w - 308);
        }
        graphics.text(font, "Groups", x, y, MUTED, false);
        renderGroupedBrowser(graphics, font, mouseX, mouseY, x, y + 12, gridW, Math.max(40, h - 12), groups);
        int detailX = x + gridW + 8;
        renderDetail(graphics, font, mouseX, mouseY, detailX, y, w - gridW - 8, h);
    }

    private static void renderDetail(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY,
            int x, int y, int w, int h) {
        List<IndexRecipeView> allViews = detailViews();
        detailSelected = clamp(detailSelected, 0, Math.max(0, allViews.size() - 1));
        graphics.fill(x, y, x + w, y + h, 0x82071117);
        graphics.fill(x, y, x + w, y + 38, 0x50102630);
        graphics.outline(x, y, w, 38, 0x5538DFF4);
        button(graphics, font, x + 4, y + 4, 22, 17, "<", historyIndex > 0);
        HITBOXES.add(new Hitbox(x + 4, y + 4, 22, 17, (button, modifiers) -> historyBack()));
        button(graphics, font, x + 28, y + 4, 22, 17, ">", historyIndex + 1 < DETAIL_HISTORY.size());
        HITBOXES.add(new Hitbox(x + 28, y + 4, 22, 17, (button, modifiers) -> historyForward()));
        button(graphics, font, x + 52, y + 4, 54, 17, "Results", true);
        HITBOXES.add(new Hitbox(x + 52, y + 4, 54, 17, (button, modifiers) -> closeDetail()));
        int openX = x + w - 50;
        boolean showOpen = w >= 235;
        if (showOpen) {
            button(graphics, font, openX, y + 4, 46, 17, "Open", true);
            HITBOXES.add(new Hitbox(openX, y + 4, 46, 17, (button, modifiers) ->
                    openRecipeScreen(detailStack, screenMode(detailMode), "index_overlay_detail_open")));
        }
        int titleRight = showOpen ? openX : x + w - 4;
        if (w >= 190 && !detailStack.isEmpty()) {
            Identifier itemId = IndexService.itemId(detailStack.getItem());
            IndexAddonPresentation.Style style = IndexAddonPresentation.style(itemId.getNamespace());
            drawScaledItem(graphics, font, detailStack, x + 111, y + 4, 20, false);
            int nameColor = ClientIndexState.isBookmarked(itemId) ? WARN : TEXT;
            graphics.text(font, trim(font, detailStack.getHoverName().getString(),
                    Math.max(36, titleRight - (x + 134) - 6)), x + 134, y + 6, nameColor, false);
            drawDetailPill(graphics, font, x + 134, y + 20, style.shortLabel(), style.accent(), 46);
            if (x + 184 < titleRight - 8) {
                drawDetailPill(graphics, font, x + 184, y + 20, style.displayName(), 0x8845CFEA,
                        Math.max(34, titleRight - (x + 188) - 6));
            }
            if (inside(mouseX, mouseY, x + 112, y + 4, Math.max(80, titleRight - (x + 112)), 32)) {
                graphics.setComponentTooltipForNextFrame(font, List.of(
                        detailStack.getHoverName(),
                        Component.literal(style.displayName() + (style.version().isBlank() ? "" : " " + style.version())),
                        Component.literal(itemId.toString())),
                        mouseX, mouseY);
            }
        }

        int chipY = y + 43;
        int modeGap = 4;
        int modeW = Math.max(42, (w - modeGap * 2) / 3);
        modeChip(graphics, font, x, chipY, modeW, IndexRecipeUi.ViewMode.RECIPES, mouseX, mouseY);
        modeChip(graphics, font, x + modeW + modeGap, chipY, modeW, IndexRecipeUi.ViewMode.USES, mouseX, mouseY);
        modeChip(graphics, font, x + (modeW + modeGap) * 2, chipY,
                Math.max(42, w - (modeW + modeGap) * 2), IndexRecipeUi.ViewMode.SOURCES, mouseX, mouseY);

        boolean compactRails = h < 260 || w < 260;
        int railY = chipY + 21;
        if (!compactRails) {
            int recentH = drawRecent(graphics, font, x, railY, w, mouseX, mouseY);
            railY += recentH;
        }
        boolean showCategories = !compactRails || h >= 220;
        if (showCategories) {
            drawCategoryChips(graphics, font, x, railY, w, allViews, mouseX, mouseY);
            railY += 22;
        }
        IndexRecipeView selectedTraceRecipe = allViews.isEmpty() ? null : allViews.get(detailSelected);
        int traceH = drawTracePath(graphics, font, x, railY, w, selectedTraceRecipe, mouseX, mouseY);
        int cardY = railY + traceH;
        int reservedBottom = 48;
        int cardX = x + 2;
        int cardW = w - 4;
        int cardShellH = Math.max(70, h - (cardY - y));
        int cardH = Math.max(40, cardShellH - reservedBottom);
        IndexRecipeUi.recordCardSelection(detailMode, detailSelected, allViews.size());
        IndexRecipeUi.drawRecipeCardBackground(graphics, cardX, cardY, cardW, cardShellH, true);
        if (allViews.isEmpty()) {
            graphics.textWithWordWrap(font, net.minecraft.network.chat.Component.literal(
                    IndexRecipeUi.emptyMessage(Minecraft.getInstance().player, detailStack.getItem(), detailMode)),
                    x + 12, cardY + 14, w - 24, MUTED);
            return;
        }
        IndexRecipeView recipe = allViews.get(detailSelected);
        IndexRecipeUi.drawRecipeCardContents(graphics, font, recipe, cardX, cardY, cardW, cardH,
                detailStack, mouseX, mouseY, SLOT_HITS);
        int actionY = cardY + cardH + 4;
        drawRecipeActions(graphics, font, recipe, x + 8, actionY, w - 16, mouseX, mouseY, true);
        for (IndexRecipeUi.SlotHit hit : SLOT_HITS) {
            HITBOXES.add(new Hitbox(hit.x(), hit.y(), hit.w(), hit.h(), (button, modifiers) -> {
                if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 || button == 2) {
                    toggleBookmark(IndexService.itemId(hit.stack().getItem()));
                } else if (button == 1 && hit.choiceCyclable()) {
                    IndexRecipeUi.cycleChoice(hit, 1);
                } else {
                    openDetail(hit.stack(), modeForSlot(hit, button));
                }
            }));
        }
        String page = (detailSelected + 1) + " / " + allViews.size();
        int prevX = x + Math.max(0, w / 2 - 52);
        int nextX = x + Math.min(w - 24, w / 2 + 28);
        button(graphics, font, prevX, y + h - 19, 24, 16, "<", detailSelected > 0);
        HITBOXES.add(new Hitbox(prevX, y + h - 19, 24, 16,
                (button, modifiers) -> detailSelected = Math.max(0, detailSelected - 1)));
        graphics.centeredText(font, page, x + w / 2, y + h - 15, MUTED);
        button(graphics, font, nextX, y + h - 19, 24, 16, ">", detailSelected + 1 < allViews.size());
        HITBOXES.add(new Hitbox(nextX, y + h - 19, 24, 16,
                (button, modifiers) -> detailSelected = Math.min(allViews.size() - 1, detailSelected + 1)));
    }

    private static void drawRecipeActions(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe,
            int x, int y, int w, int mouseX, int mouseY, boolean allowTransfer) {
        IndexRecipePlan plan = IndexRecipePlanner.plan(Minecraft.getInstance().player, recipe);
        int pinW = plan.pinned() ? 48 : 34;
        button(graphics, font, x, y, pinW, 16, plan.pinned() ? "Unpin" : "Pin", true);
        HITBOXES.add(new Hitbox(x, y, pinW, 16, (button, modifiers) -> sendRecipeAction(
                plan.pinned() ? IndexActionPacket.Action.UNPIN_RECIPE : IndexActionPacket.Action.PIN_RECIPE,
                recipe.id())));
        int tx = x + pinW + 5;
        if (plan.missingCount() > 0 && !detailStack.isEmpty()) {
            button(graphics, font, tx, y, 44, 16, "Trace", true);
            HITBOXES.add(new Hitbox(tx, y, 44, 16,
                    (button, modifiers) -> IndexRecipeTraceState.open(detailStack, recipe, plan)));
            tx += 49;
        }
        if (allowTransfer && plan.canTransfer()) {
            if (tx + 54 <= x + w) {
                button(graphics, font, tx, y, 54, 16, "Transfer", true);
                HITBOXES.add(new Hitbox(tx, y, 54, 16,
                        (button, modifiers) -> sendRecipeAction(IndexActionPacket.Action.TRANSFER_RECIPE, recipe.id())));
            }
        } else {
            String note = IndexRecipeUi.statusDetail(plan, allowTransfer);
            int noteW = Math.max(0, x + w - tx - 4);
            if (!note.isBlank() && noteW >= 28) {
                graphics.text(font, trim(font, note, noteW), tx, y + 5,
                        IndexRecipeUi.statusColor(plan, allowTransfer), false);
            }
        }
    }

    private static int drawTracePath(GuiGraphicsExtractor graphics, Font font, int x, int y, int w,
            IndexRecipeView selectedRecipe, int mouseX, int mouseY) {
        IndexRecipeTraceState.Trace trace = IndexRecipeTraceState.current();
        if (!traceApplies(trace)) {
            return 0;
        }
        int h = trace.entries().isEmpty() ? 18 : 42;
        graphics.fill(x, y, x + w, y + h, 0x88102630);
        graphics.outline(x, y, w, h, 0x5538DFF4);
        String path = "Path: " + trace.rootStack().getHoverName().getString() + " > missing inputs";
        graphics.text(font, trim(font, path, w - 84), x + 6, y + 6, MUTED, false);
        button(graphics, font, x + w - 76, y + 2, 70, 16,
                selectedRecipe != null && selectedRecipe.id().equals(trace.rootRecipeId()) ? "Tracing" : "Root", true);
        HITBOXES.add(new Hitbox(x + w - 76, y + 2, 70, 16,
                (button, modifiers) -> openRecipe(trace.rootRecipeId())));
        if (trace.entries().isEmpty()) {
            return h + 4;
        }
        int cx = x + 6;
        int cy = y + 22;
        for (IndexRecipeTraceState.TraceEntry entry : trace.entries().stream().limit(4).toList()) {
            int chipW = Math.min(92, Math.max(42, font.width(entry.stack().getHoverName().getString()) + 20));
            if (cx + chipW > x + w - 4) {
                break;
            }
            boolean selected = !detailStack.isEmpty() && IndexService.itemId(detailStack.getItem()).equals(entry.itemId());
            chip(graphics, font, cx, cy, chipW, entry.stack().getHoverName().getString(), selected, mouseX, mouseY);
            if (inside(mouseX, mouseY, cx, cy, chipW, 17)) {
                graphics.setComponentTooltipForNextFrame(font, List.of(
                        entry.stack().getHoverName(),
                        net.minecraft.network.chat.Component.literal(entry.countLabel()),
                        net.minecraft.network.chat.Component.literal(entry.dataLabel())),
                        cx + chipW / 2, cy + 9);
            }
            HITBOXES.add(new Hitbox(cx, cy, chipW, 17,
                    (button, modifiers) -> openDetail(entry.stack(), IndexRecipeUi.ViewMode.RECIPES)));
            cx += chipW + 4;
        }
        return h + 4;
    }

    private static boolean traceApplies(IndexRecipeTraceState.Trace trace) {
        if (trace == null || !trace.active() || detailStack.isEmpty()) {
            return false;
        }
        Identifier detailId = IndexService.itemId(detailStack.getItem());
        if (detailId.equals(trace.rootItemId())) {
            return true;
        }
        return trace.entries().stream().anyMatch(entry -> detailId.equals(entry.itemId()));
    }

    private static IndexRecipeUi.ViewMode modeForSlot(IndexRecipeUi.SlotHit hit, int button) {
        if (button == 1) {
            return IndexRecipeUi.ViewMode.USES;
        }
        IndexSlotRole role = hit.role();
        return role == IndexSlotRole.OUTPUT ? IndexRecipeUi.ViewMode.RECIPES : IndexRecipeUi.ViewMode.USES;
    }

    private static void modeChip(GuiGraphicsExtractor graphics, Font font, int x, int y, int w,
            IndexRecipeUi.ViewMode mode, int mouseX, int mouseY) {
        int count = modeCount(mode);
        chip(graphics, font, x, y, w, modeLabel(mode) + " " + count, detailMode == mode, mouseX, mouseY);
        HITBOXES.add(new Hitbox(x, y, w, 17, (button, modifiers) -> setDetailMode(mode)));
    }

    private static int drawRecent(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int mouseX, int mouseY) {
        List<HistoryEntry> recent = recentHistory();
        if (recent.isEmpty()) {
            return 0;
        }
        graphics.text(font, "Recent", x, y + 5, MUTED, false);
        int cx = x + 42;
        for (HistoryEntry entry : recent) {
            ItemStack stack = itemStack(entry.itemId());
            if (stack.isEmpty()) {
                continue;
            }
            boolean selected = !detailStack.isEmpty() && IndexService.itemId(detailStack.getItem()).equals(entry.itemId());
            boolean hover = inside(mouseX, mouseY, cx, y, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE);
            slimIconSurface(graphics, cx, y, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE, hover, selected, CYAN);
            drawScaledItem(graphics, font, stack, cx, y, ICON_BUTTON_SIZE, false);
            if (hover) {
                IndexTooltipUtil.showItemTooltip(graphics, font, stack, cx + 10, y + 10);
            }
            HITBOXES.add(new Hitbox(cx, y, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE, (button, modifiers) -> restoreHistory(entry)));
            cx += 23;
            if (cx + ICON_BUTTON_SIZE > x + w) {
                break;
            }
        }
        return 23;
    }

    private static void drawDetailPill(GuiGraphicsExtractor graphics, Font font, int x, int y, String label,
            int color, int maxWidth) {
        int width = Math.min(Math.max(28, maxWidth), Math.max(28, font.width(label) + 10));
        graphics.fill(x, y, x + width, y + 13, 0x55102630);
        graphics.outline(x, y, width, 13, color);
        graphics.centeredText(font, trim(font, label, width - 6), x + width / 2, y + 4, color);
    }

    private static void drawCategoryChips(GuiGraphicsExtractor graphics, Font font, int x, int y, int w,
            List<IndexRecipeView> views, int mouseX, int mouseY) {
        chip(graphics, font, x, y, 34, "All", detailCategory == null, mouseX, mouseY);
        HITBOXES.add(new Hitbox(x, y, 34, 17, (button, modifiers) -> {
            detailCategory = null;
            detailSelected = 0;
        }));
        Set<Identifier> categories = new LinkedHashSet<>();
        for (IndexRecipeView view : baseDetailViews()) {
            categories.add(view.categoryId());
        }
        int cx = x + 38;
        for (Identifier category : categories) {
            String label = IndexAddonPresentation.compactCategoryLabel(category);
            int chipW = Math.min(96, Math.max(42, font.width(label) + 12));
            if (cx + chipW > x + w) {
                break;
            }
            chip(graphics, font, cx, y, chipW, label, category.equals(detailCategory), mouseX, mouseY);
            if (inside(mouseX, mouseY, cx, y, chipW, 17)) {
                graphics.setComponentTooltipForNextFrame(font, List.of(
                        Component.literal(IndexAddonPresentation.categoryLabel(category)),
                        Component.literal(category.toString())),
                        mouseX, mouseY);
            }
            HITBOXES.add(new Hitbox(cx, y, chipW, 17, (button, modifiers) -> {
                detailCategory = category.equals(detailCategory) ? null : category;
                detailSelected = 0;
            }));
            cx += chipW + 4;
        }
    }

    private static void drawFooter(GuiGraphicsExtractor graphics, Font font, int x, int mouseX, int mouseY) {
        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshot(Minecraft.getInstance().player);
        int footerY = panelY + panelH - 20;
        graphics.fill(panelX + 1, footerY - 3, panelX + panelW - 1, panelY + panelH - 1, 0x88071117);
        List<IndexModGroup> groups = visibleGroups();
        int visibleItems = groups.stream().mapToInt(group -> group.visibleItems().size()).sum();
        String left = groups.size() + " mods \u2022 " + visibleItems + " items";
        int optionsW = 82;
        int optionsX = panelX + panelW - optionsW - 10;
        int diagnosticsX = optionsX - 50;
        int leftMax = Math.max(36, diagnosticsX - x - 6);
        graphics.text(font, trim(font, left, leftMax), x, footerY, CYAN, false);
        compactButton(graphics, font, optionsX, footerY - 4, optionsW, 17,
                text("screen.echoindex.overlay.options") + " v", true, mouseX, mouseY);
        HITBOXES.add(new Hitbox(optionsX, footerY - 4, optionsW, 17, (button, modifiers) ->
                toggleOptionsPopup(optionsX, footerY - 126)));
        int warnings = snapshot.warnings().size();
        if ((warnings > 0 || snapshot.recipesStillLoading() || Config.DEBUG_SHOW_RECIPE_IDS.get())
                && diagnosticsX >= x + 44) {
            int bx = diagnosticsX;
            String label = snapshot.recipesStillLoading()
                    ? text("screen.echoindex.overlay.diagnostics.loading_short")
                    : text("screen.echoindex.overlay.diagnostics.warning_short");
            button(graphics, font, bx, footerY - 4, 44, 16, label, warnings == 0);
            if (inside(mouseX, mouseY, bx, footerY - 4, 44, 16)) {
                graphics.setComponentTooltipForNextFrame(font, List.of(
                        tr("screen.echoindex.overlay.tooltip.diagnostics"),
                        tr(snapshot.recipesStillLoading()
                                ? "screen.echoindex.overlay.tooltip.diagnostics.loading"
                                : "screen.echoindex.overlay.tooltip.diagnostics.open")),
                        mouseX, mouseY);
            }
            HITBOXES.add(new Hitbox(bx, footerY - 4, 44, 16,
                    (button, modifiers) -> openDiagnosticsScreen("index_overlay_diagnostics_button")));
        }
    }

    private static void openRecipeScreen(ItemStack stack, IndexRecipeScreen.Mode mode, String transitionSource) {
        IndexRecipeScreen.Mode resolvedMode = mode == null ? IndexRecipeScreen.Mode.RECIPES : mode;
        EchoNativeLoadStatus lifecycleStatus = EchoIndexClient.publishNativeScreenLifecycle(
                "open",
                "index.inventory_overlay.open_recipe",
                IndexRecipeScreen.class.getName(),
                Map.of(
                        "targetScreenClass", IndexRecipeScreen.class.getName(),
                        "transitionSource", transitionSource == null ? "" : transitionSource,
                        "recipeMode", resolvedMode.name(),
                        "itemId", stack == null || stack.isEmpty()
                                ? ""
                                : IndexService.itemId(stack.getItem()).toString()
                ));
        if (EchoIndexClient.nativeLoaderClientActiveForScreens()
                && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
            return;
        }
        Minecraft.getInstance().setScreen(new IndexRecipeScreen(stack, resolvedMode));
    }

    private static void openDiagnosticsScreen(String transitionSource) {
        EchoNativeLoadStatus lifecycleStatus = EchoIndexClient.publishNativeScreenLifecycle(
                "open",
                "index.inventory_overlay.open_diagnostics",
                IndexDiagnosticsScreen.class.getName(),
                Map.of(
                        "targetScreenClass", IndexDiagnosticsScreen.class.getName(),
                        "transitionSource", transitionSource == null ? "" : transitionSource
                ));
        if (EchoIndexClient.nativeLoaderClientActiveForScreens()
                && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
            return;
        }
        Minecraft.getInstance().setScreen(new IndexDiagnosticsScreen());
    }

    private static int drawPinnedDrawer(GuiGraphicsExtractor graphics, Font font, int x, int y, int maxX,
            int mouseX, int mouseY) {
        Set<Identifier> pinned = ClientIndexState.pinnedRecipes();
        if (pinned.isEmpty() || x + 20 > maxX) {
            return x;
        }
        graphics.text(font, text("screen.echoindex.overlay.footer.pins"), x, y + 5, WARN, false);
        int cx = x + 28;
        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshot(Minecraft.getInstance().player);
        List<Identifier> orderedPins = pinned.stream()
                .sorted((left, right) -> Integer.compare(pinGroup(snapshot.byId().get(left)), pinGroup(snapshot.byId().get(right))))
                .limit(6)
                .toList();
        for (Identifier id : orderedPins) {
            IndexRecipeView recipe = snapshot.byId().get(id);
            if (recipe == null || cx + 22 > maxX) {
                continue;
            }
            ItemStack icon = IndexRecipeUi.recipeIcon(recipe, ItemStack.EMPTY);
            IndexRecipePlan plan = IndexRecipePlanner.plan(Minecraft.getInstance().player, recipe);
            boolean hover = inside(mouseX, mouseY, cx, y, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE);
            slimIconSurface(graphics, cx, y, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE, hover, false,
                    IndexRecipeUi.statusColor(plan, true));
            drawScaledItem(graphics, font, icon, cx, y, ICON_BUTTON_SIZE, false);
            if (plan.missingCount() > 0) {
                graphics.text(font, "!", cx + 14, y + 10, WARN, false);
            } else if (plan.sourceCard()) {
                graphics.text(font, "S", cx + 14, y + 10, MUTED, false);
            }
            if (hover) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(icon.getHoverName());
                tooltip.add(Component.literal(IndexRecipeUi.statusLabel(plan, true)));
                tooltip.add(pinTooltip(plan));
                IndexTooltipUtil.appendModName(tooltip, icon);
                graphics.setComponentTooltipForNextFrame(font, tooltip, cx + 10, y + 10);
            }
            HITBOXES.add(new Hitbox(cx, y, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE, (button, modifiers) -> openRecipe(recipe.id())));
            cx += 23;
        }
        return cx + 4;
    }

    private static int pinGroup(IndexRecipeView recipe) {
        if (recipe == null) {
            return 4;
        }
        IndexRecipePlan plan = IndexRecipePlanner.plan(Minecraft.getInstance().player, recipe);
        if (plan.canTransfer()) {
            return 0;
        }
        if (plan.missingCount() > 0) {
            return 1;
        }
        if (plan.sourceCard()) {
            return 3;
        }
        return 2;
    }

    private static Component pinTooltip(IndexRecipePlan plan) {
        if (plan == null) {
            return tr("screen.echoindex.overlay.tooltip.pin.unavailable");
        }
        if (plan.canTransfer()) {
            return tr("screen.echoindex.overlay.tooltip.pin.ready");
        }
        if (plan.missingCount() > 0) {
            return tr("screen.echoindex.overlay.tooltip.pin.missing", plan.missingCount());
        }
        if (plan.sourceCard()) {
            return tr("screen.echoindex.overlay.tooltip.pin.source");
        }
        return plan.transferBlocker().isBlank()
                ? tr("screen.echoindex.overlay.tooltip.pin.plan_only")
                : Component.literal(plan.transferBlocker());
    }

    private static boolean active(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> container) || Minecraft.getInstance().player == null) {
            return false;
        }
        String name = screen.getClass().getName();
        return IndexService.INSTANCE.overlayEnabled(Minecraft.getInstance().player)
                && !IndexService.INSTANCE.excludedScreen(name)
                && !name.contains("IndexDiagnosticsScreen")
                && container.getImageWidth() > 0;
    }

    private static boolean screenHasFocusedInput(Screen screen) {
        GuiEventListener focused = screen.getFocused();
        return focused != null && focused.isFocused();
    }

    private static ItemStack hoveredInventoryStack(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return ItemStack.EMPTY;
        }
        int left = container.getLeftPos();
        int top = container.getTopPos();
        for (Slot slot : container.getMenu().slots) {
            if (slot == null || !slot.isActive() || !slot.hasItem()) {
                continue;
            }
            if (inside(lastMouseX, lastMouseY, left + slot.x, top + slot.y, 16, 16)) {
                return slot.getItem().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private static void layout(Screen screen) {
        AbstractContainerScreen<?> container = (AbstractContainerScreen<?>) screen;
        int margin = 6;
        int gap = 8;
        int minW = Math.min(260, Math.max(160, screen.width - margin * 2));
        int requestedW = Config.OVERLAY_WIDTH.get();
        Config.OverlayLayout overlayLayout = Config.OVERLAY_LAYOUT.get();
        if (overlayLayout == Config.OverlayLayout.COMPACT && requestedW <= 238) {
            overlayLayout = Config.OverlayLayout.JEI;
            requestedW = 300;
        }
        int maxScreenW = Math.max(minW, screen.width - margin * 2);
        int maxDrawerW = Math.min(maxScreenW, detailStack.isEmpty() ? 460 : 560);
        int desiredW = clamp(requestedW, minW, maxDrawerW);
        int availableH = Math.max(180, screen.height - margin * 2);
        int compactH = clamp(Math.max(container.getImageHeight(), 220), 160, availableH);
        int tallH = Math.min(Math.max(340, availableH * 4 / 5), availableH);
        panelH = switch (overlayLayout) {
            case COMPACT -> compactH;
            case TALL -> tallH;
            case JEI -> availableH;
        };
        int desiredY = switch (overlayLayout) {
            case COMPACT -> container.getTopPos();
            case TALL -> container.getTopPos() + container.getImageHeight() / 2 - panelH / 2;
            case JEI -> margin;
        };
        panelY = clamp(desiredY, margin, Math.max(margin, screen.height - panelH - margin));

        int rightStart = container.getLeftPos() + container.getImageWidth() + gap;
        int rightSpace = screen.width - margin - rightStart;
        int leftSpace = container.getLeftPos() - gap - margin;
        boolean preferLeft = Config.OVERLAY_SIDE.get() == Config.OverlaySide.LEFT;
        boolean useLeft;
        if (preferLeft && leftSpace >= minW) {
            useLeft = true;
        } else if (!preferLeft && rightSpace >= minW) {
            useLeft = false;
        } else if (rightSpace >= minW) {
            useLeft = false;
        } else if (leftSpace >= minW) {
            useLeft = true;
        } else {
            useLeft = preferLeft;
        }
        int available = useLeft ? leftSpace : rightSpace;
        int availableMax = Math.max(minW, Math.min(available, maxDrawerW));
        int targetW = desiredW;
        if (!detailStack.isEmpty()) {
            targetW = Math.max(targetW, Math.min(520, availableMax));
        }
        panelW = clamp(Math.min(targetW, availableMax), minW, maxDrawerW);
        panelX = useLeft ? container.getLeftPos() - gap - panelW : rightStart;
        panelX = clamp(panelX, margin, Math.max(margin, screen.width - panelW - margin));
        PanelBounds saved = PANEL_BOUNDS.get(activeScreenKey);
        if (saved != null) {
            panelW = clamp(saved.w(), minW, maxDrawerW);
            panelH = clamp(saved.h(), 180, availableH);
            panelX = clamp(saved.x(), margin, Math.max(margin, screen.width - panelW - margin));
            panelY = clamp(saved.y(), margin, Math.max(margin, screen.height - panelH - margin));
        }
    }

    private static boolean beginPanelDrag(double mouseX, double mouseY) {
        if (inside(mouseX, mouseY, panelX + panelW - 14, panelY + panelH - 14, 14, 14)) {
            dragMode = DragMode.RESIZE;
        } else if (inside(mouseX, mouseY, panelX, panelY, panelW, 25)) {
            dragMode = DragMode.MOVE;
        } else {
            dragMode = DragMode.NONE;
            return false;
        }
        dragMouseX = (int) Math.round(mouseX);
        dragMouseY = (int) Math.round(mouseY);
        dragPanelX = panelX;
        dragPanelY = panelY;
        dragPanelW = panelW;
        dragPanelH = panelH;
        return true;
    }

    private static void storePanelBounds() {
        if (!activeScreenKey.isBlank()) {
            PANEL_BOUNDS.put(activeScreenKey, new PanelBounds(panelX, panelY, panelW, panelH));
        }
    }

    private static void itemSlot(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        itemSlot(graphics, font, stack, x, y, SLOT_SIZE_NORMAL, mouseX, mouseY);
    }

    private static void slimIconSurface(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            boolean hover, boolean selected, int accent) {
        int fill = selected ? 0x76123241 : hover ? 0x5C102630 : 0x36071117;
        int border = selected || hover
                ? IndexThemeStyle.alpha(accent, selected ? 204 : 170)
                : IndexThemeStyle.alpha(accent, 74);
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.outline(x, y, width, height, border);
        if ((selected || hover) && width >= 10 && height >= 4) {
            graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1,
                    IndexThemeStyle.alpha(accent, selected ? 168 : 116));
        }
    }

    private static void drawScaledItem(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y,
            int slotSize, boolean decorations) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        int iconSize = Math.max(16, Math.min(22, slotSize - (slotSize <= 18 ? 1 : 2)));
        int iconX = x + Math.max(0, (slotSize - iconSize) / 2);
        int iconY = y + Math.max(0, (slotSize - iconSize) / 2);
        if (iconSize == 16) {
            graphics.item(stack, iconX, iconY);
        } else {
            float scale = iconSize / 16.0F;
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate(iconX, iconY);
                graphics.pose().scale(scale, scale);
                graphics.item(stack, 0, 0);
            } finally {
                graphics.pose().popMatrix();
            }
        }
        if (decorations) {
            int decorationInset = Math.max(1, (slotSize - 16) / 2);
            graphics.itemDecorations(font, stack, x + decorationInset, y + decorationInset);
        }
    }

    private static void itemSlot(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y, int slotSize,
            int mouseX, int mouseY) {
        int size = Math.max(18, slotSize);
        boolean hover = inside(mouseX, mouseY, x, y, size, size);
        slimIconSurface(graphics, x, y, size, size, hover, false,
                hover ? IndexRecipeUi.SLOT_OUTLINE_HOVER : IndexRecipeUi.SLOT_OUTLINE);
        drawScaledItem(graphics, font, stack, x, y, size, true);
        if (hover) {
            hoveredStack = stack;
            IndexTooltipUtil.showItemTooltip(graphics, font, stack, itemTooltip(stack), mouseX, mouseY);
        }
    }

    private static int slotSize() {
        return switch (currentGridDensity()) {
            case COMPACT -> SLOT_SIZE_COMPACT;
            case LARGE -> SLOT_SIZE_LARGE;
            case NORMAL -> SLOT_SIZE_NORMAL;
        };
    }

    private static int gridStep() {
        return slotSize() + switch (currentGridDensity()) {
            case COMPACT -> 4;
            case LARGE -> 7;
            case NORMAL -> 5;
        };
    }

    private static GridLayout gridLayout(int gridW, int gridH, int itemCount) {
        GridLayout full = gridLayout(gridW, false);
        int rows = (itemCount + full.columns() - 1) / full.columns();
        if (rows * full.step() <= gridH) {
            return full;
        }
        return gridLayout(gridW, true);
    }

    private static GridLayout gridLayout(int gridW, boolean reserveScrollbar) {
        int slot = slotSize();
        int step = gridStep();
        int gap = Math.max(1, step - slot);
        int usableW = Math.max(slot, gridW - (reserveScrollbar ? GRID_SCROLLBAR_GUTTER : 0));
        int compactGridW = Math.max(slot, Config.OVERLAY_WIDTH.get() - INNER_PAD * 2);
        boolean enlarged = usableW > compactGridW + step;
        int maxColumns = enlarged ? OVERLAY_RESPONSIVE_MAX_COLUMNS : Math.max(1, Config.OVERLAY_MAX_COLUMNS.get());
        int fitColumns = Math.max(1, (usableW + gap) / step);
        int columns = Math.max(1, Math.min(maxColumns, fitColumns));
        return new GridLayout(slot, step, columns);
    }

    private static Config.GridDensity currentGridDensity() {
        if (gridDensityOverridden) {
            return gridDensity;
        }
        try {
            gridDensity = Config.OVERLAY_GRID_DENSITY.get();
        } catch (RuntimeException exception) {
            gridDensity = Config.GridDensity.NORMAL;
        }
        return gridDensity;
    }

    private static String densityLabel() {
        return switch (currentGridDensity()) {
            case COMPACT -> "C";
            case NORMAL -> "N";
            case LARGE -> "L";
        };
    }

    private static String densityName() {
        return switch (currentGridDensity()) {
            case COMPACT -> text("screen.echoindex.overlay.density.compact");
            case NORMAL -> text("screen.echoindex.overlay.density.normal");
            case LARGE -> text("screen.echoindex.overlay.density.large");
        };
    }

    private static void cycleGridDensity() {
        gridDensity = switch (currentGridDensity()) {
            case COMPACT -> Config.GridDensity.NORMAL;
            case NORMAL -> Config.GridDensity.LARGE;
            case LARGE -> Config.GridDensity.COMPACT;
        };
        gridDensityOverridden = true;
        resetGridScroll();
        gridCacheKey = null;
        saveScreenState();
    }

    private static List<IndexModGroup> visibleGroups() {
        String query = effectiveSearch();
        GroupViewCacheKey key = new GroupViewCacheKey(
                activeScreenKey,
                IndexService.INSTANCE.itemCatalogRevision(),
                ClientIndexState.revision(),
                query,
                categoryFilter,
                bookmarkedOnly,
                groupMode,
                indexViewMode,
                showHiddenMods,
                showEmptyMods,
                minecraftFirst,
                echoPriority,
                showOnlyGroup,
                groupStateRevision);
        if (key.equals(groupViewCacheKey)) {
            return groupViewCacheGroups;
        }
        groupViewCacheKey = key;
        boolean filtering = activeBrowseFilter(query);
        Set<String> matchedKeys = matchingItemKeys(query);
        currentMatchedItemKeys = matchedKeys;
        List<IndexModGroup> rawGroups = groupMode == GroupMode.MOD
                ? IndexModGroupCache.groups(Minecraft.getInstance().player)
                : categoryGroups(Minecraft.getInstance().player);
        List<IndexModGroup> groups = new ArrayList<>();
        for (IndexModGroup raw : rawGroups) {
            boolean pinned = PINNED_GROUPS.contains(raw.modId());
            boolean hidden = HIDDEN_GROUPS.contains(raw.modId());
            if (!showOnlyGroup.isBlank() && !showOnlyGroup.equals(raw.modId())) {
                continue;
            }
            if (hidden && !showHiddenMods) {
                continue;
            }
            List<ItemStack> visible = filtering
                    ? raw.allItems().stream().filter(stack -> matchedKeys.contains(stackKey(stack))).toList()
                    : raw.allItems();
            if (visible.isEmpty() && (!showEmptyMods || !filtering)) {
                continue;
            }
            groups.add(raw.withState(visible, COLLAPSED_GROUPS.contains(raw.modId()), pinned, hidden));
        }
        groups.sort(IndexOverlay::compareGroups);
        groupViewCacheGroups = List.copyOf(groups);
        quickJumpPage = clamp(quickJumpPage, 0, Math.max(0, (groups.size() - 1) / 1));
        return groupViewCacheGroups;
    }

    private static int compareGroups(IndexModGroup left, IndexModGroup right) {
        int pinned = Boolean.compare(right.isPinned(), left.isPinned());
        if (pinned != 0) {
            return pinned;
        }
        if (groupMode == GroupMode.MOD && minecraftFirst) {
            int minecraft = Boolean.compare("minecraft".equals(right.modId()), "minecraft".equals(left.modId()));
            if (minecraft != 0) {
                return minecraft;
            }
        }
        if (groupMode == GroupMode.MOD && echoPriority) {
            int echo = Boolean.compare(isEchoGroup(right.modId()), isEchoGroup(left.modId()));
            if (echo != 0) {
                return echo;
            }
        }
        return left.displayName().compareToIgnoreCase(right.displayName());
    }

    private static boolean isEchoGroup(String modId) {
        return modId != null && modId.toLowerCase(Locale.ROOT).startsWith("echo");
    }

    private static Set<String> matchingItemKeys(String query) {
        if (!activeBrowseFilter(query)) {
            return Set.of();
        }
        String serviceQuery = browseServiceQuery(query);
        List<ItemStack> candidates = serviceQuery.isBlank()
                ? IndexService.INSTANCE.itemCatalog(Minecraft.getInstance().player)
                : IndexService.INSTANCE.filteredItemsUnbounded(Minecraft.getInstance().player, serviceQuery);
        Set<String> keys = new HashSet<>();
        for (ItemStack stack : candidates) {
            if (bookmarkedOnly && !ClientIndexState.isBookmarked(IndexService.itemId(stack.getItem()))) {
                continue;
            }
            keys.add(stackKey(stack));
        }
        return keys;
    }

    private static String browseServiceQuery(String query) {
        StringBuilder builder = new StringBuilder();
        if (query != null && !query.isBlank()) {
            builder.append(query.trim());
        }
        if (!categoryFilter.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(categoryFilter);
        }
        return builder.toString();
    }

    private static boolean activeBrowseFilter() {
        return activeBrowseFilter(effectiveSearch());
    }

    private static boolean activeBrowseFilter(String query) {
        return (query != null && !query.isBlank()) || !categoryFilter.isBlank() || bookmarkedOnly;
    }

    private static List<IndexModGroup> categoryGroups(Player player) {
        Map<String, CategoryBucket> buckets = new LinkedHashMap<>();
        buckets.put("$machines", new CategoryBucket("$machines", text("echoindex.category.machines"),
                new ItemStack(Items.CRAFTING_TABLE), 0xFF66E8FF));
        buckets.put("$blocks", new CategoryBucket("$blocks", text("echoindex.category.blocks"),
                new ItemStack(Items.BRICKS), 0xFF66E8FF));
        buckets.put("$tools", new CategoryBucket("$tools", text("echoindex.category.tools"),
                new ItemStack(Items.IRON_PICKAXE), 0xFF66E8FF));
        buckets.put("$combat", new CategoryBucket("$combat", text("echoindex.category.combat"),
                new ItemStack(Items.IRON_SWORD), 0xFF66E8FF));
        buckets.put("$echo", new CategoryBucket("$echo", "ECHO", new ItemStack(Items.REDSTONE), 0xFFFFD166));
        buckets.put("$other", new CategoryBucket("$other", text("screen.echoindex.overlay.category.other"),
                new ItemStack(Items.CHEST), 0xFF8CA7B5));
        for (ItemStack stack : IndexService.INSTANCE.itemCatalog(player)) {
            buckets.get(categoryGroupId(stack)).items().add(stack.copy());
        }
        return buckets.values().stream()
                .map(CategoryBucket::toGroup)
                .toList();
    }

    private static String categoryGroupId(ItemStack stack) {
        Identifier id = IndexService.itemId(stack.getItem());
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String namespace = id.getNamespace().toLowerCase(Locale.ROOT);
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (stack.getItem() instanceof BlockItem && hasAny(path + " " + name,
                "machine", "station", "bench", "workbench", "forge", "fabricator", "generator",
                "press", "grinder", "compressor", "refinery", "smelter", "reclaimer", "scanner",
                "terminal", "console", "dock", "beacon", "scrubber", "purifier", "hopper",
                "condenser", "charger", "array", "core")) {
            return "$machines";
        }
        if (stack.getItem() instanceof BlockItem) {
            return "$blocks";
        }
        if (stack.isDamageableItem()) {
            return "$tools";
        }
        if (hasAny(path + " " + name, "sword", "bow", "armor", "shield", "rifle", "gun", "blade",
                "hammer", "staff", "dagger", "chakram", "gauntlet", "launcher", "lance", "knife")) {
            return "$combat";
        }
        if (namespace.startsWith("echo") || name.contains("echo") || path.contains("echo")) {
            return "$echo";
        }
        return "$other";
    }

    private static boolean hasAny(String value, String... needles) {
        String haystack = value == null ? "" : value;
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static int groupedSlotSize() {
        return indexViewMode == IndexViewMode.COMPACT ? SLOT_SIZE_COMPACT : slotSize();
    }

    private static int groupedStep() {
        return groupedSlotSize() + (indexViewMode == IndexViewMode.COMPACT ? 4 : Math.max(4, gridStep() - slotSize() + 3));
    }

    private static int groupedColumns(int width) {
        int slot = groupedSlotSize();
        int step = groupedStep();
        int usable = Math.max(slot, width - GROUP_BODY_PAD * 2 - GRID_SCROLLBAR_GUTTER);
        return Math.max(1, usable / step);
    }

    private static String headerCountLabel(IndexModGroup group) {
        if (activeBrowseFilter()) {
            return group.visibleItems().size() + " match";
        }
        return Integer.toString(group.itemCount());
    }

    private static String sortLabel() {
        return groupMode == GroupMode.MOD
                ? text("screen.echoindex.overlay.sort.mod")
                : text("screen.echoindex.overlay.sort.category");
    }

    private static String viewLabel() {
        return indexViewMode == IndexViewMode.COMPACT
                ? text("screen.echoindex.overlay.view.compact")
                : text("screen.echoindex.overlay.view.detailed");
    }

    private static void cycleGroupExpansion(String groupId, int width) {
        int oldOffset = GROUP_OFFSETS.getOrDefault(groupId, scroll);
        if (indexViewMode == IndexViewMode.DETAILED) {
            FULLY_EXPANDED_GROUPS.remove(groupId);
            if (!COLLAPSED_GROUPS.add(groupId)) {
                COLLAPSED_GROUPS.remove(groupId);
            }
        } else if (COLLAPSED_GROUPS.remove(groupId)) {
            FULLY_EXPANDED_GROUPS.remove(groupId);
        } else if (FULLY_EXPANDED_GROUPS.remove(groupId)) {
            COLLAPSED_GROUPS.add(groupId);
        } else {
            FULLY_EXPANDED_GROUPS.add(groupId);
        }
        markGroupStateChanged();
        preserveGroupScroll(groupId, width, oldOffset);
        closePopup();
    }

    private static void expandOnlyGroup(String groupId, int width) {
        int oldOffset = GROUP_OFFSETS.getOrDefault(groupId, scroll);
        COLLAPSED_GROUPS.clear();
        FULLY_EXPANDED_GROUPS.clear();
        for (IndexModGroup group : groupViewCacheGroups) {
            if (!group.modId().equals(groupId)) {
                COLLAPSED_GROUPS.add(group.modId());
            }
        }
        FULLY_EXPANDED_GROUPS.add(groupId);
        markGroupStateChanged();
        preserveGroupScroll(groupId, width, oldOffset);
        closePopup();
    }

    private static void preserveGroupScroll(String groupId, int width, int oldOffset) {
        if (width <= 0 || lastGridH <= 0) {
            return;
        }
        GroupMetrics metrics = groupMetrics(visibleGroups(), width);
        int newOffset = GROUP_OFFSETS.getOrDefault(groupId, oldOffset);
        int maxScroll = Math.max(0, metrics.contentHeight() - lastGridH);
        scroll = clamp(scroll + newOffset - oldOffset, 0, maxScroll);
    }

    private static String groupChevron(GroupLayout layout) {
        if (layout.group().isCollapsed()) {
            return ">";
        }
        if (indexViewMode == IndexViewMode.DETAILED || layout.fullExpanded()) {
            return "^";
        }
        return "v";
    }

    private static String groupStateLabel(GroupLayout layout) {
        if (layout.group().isCollapsed()) {
            return "Collapsed";
        }
        if (indexViewMode == IndexViewMode.DETAILED) {
            return "Detailed - all rows";
        }
        return layout.fullExpanded() ? "Full expanded" : "Compact preview";
    }

    private static String groupNextClickLabel(GroupLayout layout) {
        if (layout.group().isCollapsed()) {
            return "show compact preview";
        }
        if (indexViewMode == IndexViewMode.DETAILED || layout.fullExpanded()) {
            return "collapse section";
        }
        return "expand all items";
    }

    private static void markGroupStateChanged() {
        groupStateRevision++;
        groupViewCacheKey = null;
    }

    private static List<ItemStack> gridItems() {
        String query = browseServiceQuery(effectiveSearch());
        GridCacheKey key = new GridCacheKey(activeScreenKey, query, ClientIndexState.revision());
        if (!key.equals(gridCacheKey)) {
            gridCacheKey = key;
            List<ItemStack> items = List.copyOf(IndexService.INSTANCE.filteredItemsUnbounded(Minecraft.getInstance().player, query));
            if (bookmarkedOnly) {
                items = items.stream()
                        .filter(stack -> ClientIndexState.isBookmarked(IndexService.itemId(stack.getItem())))
                        .toList();
            }
            gridCacheItems = items;
        }
        return gridCacheItems;
    }

    private static List<IndexRecipeView> baseDetailViews() {
        if (detailStack.isEmpty()) {
            return List.of();
        }
        DetailBaseCacheKey key = new DetailBaseCacheKey(activeScreenKey, IndexService.itemId(detailStack.getItem()),
                detailMode, recipeSnapshotGeneration(), ClientIndexState.revision(), IndexRecipeQueryClientState.revision());
        if (!key.equals(detailBaseCacheKey)) {
            detailBaseCacheKey = key;
            detailBaseCacheViews = List.copyOf(
                    IndexRecipeUi.viewsFor(Minecraft.getInstance().player, detailStack.getItem(), detailMode));
            detailCacheKey = null;
        }
        return detailBaseCacheViews;
    }

    private static List<IndexRecipeView> detailViews() {
        List<IndexRecipeView> views = baseDetailViews();
        DetailCacheKey key = new DetailCacheKey(detailBaseCacheKey, detailCategory);
        if (!key.equals(detailCacheKey)) {
            detailCacheKey = key;
            detailCacheViews = detailCategory == null ? views
                    : views.stream().filter(view -> detailCategory.equals(view.categoryId())).toList();
        }
        return detailCacheViews;
    }

    private static int modeCount(IndexRecipeUi.ViewMode mode) {
        if (detailStack.isEmpty()) {
            return 0;
        }
        ModeCountKey key = new ModeCountKey(activeScreenKey, IndexService.itemId(detailStack.getItem()), mode,
                recipeSnapshotGeneration(), ClientIndexState.revision(), IndexRecipeQueryClientState.revision());
        Integer cached = MODE_COUNT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        int count = IndexRecipeUi.viewsFor(Minecraft.getInstance().player, detailStack.getItem(), mode).size();
        if (MODE_COUNT_CACHE.size() > 64) {
            MODE_COUNT_CACHE.clear();
        }
        MODE_COUNT_CACHE.put(key, count);
        return count;
    }

    private static long recipeSnapshotGeneration() {
        return IndexService.INSTANCE.recipeSnapshot(Minecraft.getInstance().player).generation();
    }

    private static void openDetail(ItemStack stack, IndexRecipeUi.ViewMode mode) {
        detailStack = stack == null ? ItemStack.EMPTY : stack.copy();
        detailMode = mode == null ? IndexRecipeUi.ViewMode.RECIPES : mode;
        detailCategory = null;
        detailSelected = 0;
        searchFocused = false;
        resetGridScroll();
        pushHistory(detailStack, detailMode);
        saveScreenState();
    }

    private static void closeDetail() {
        detailStack = ItemStack.EMPTY;
        detailCategory = null;
        detailSelected = 0;
        saveScreenState();
    }

    private static void openRecipe(Identifier recipeId) {
        IndexRecipeView recipe = IndexService.INSTANCE.recipeSnapshot(Minecraft.getInstance().player).byId().get(recipeId);
        if (recipe == null) {
            recipe = IndexRecipeQueryClientState.recipe(recipeId).orElse(null);
        }
        if (recipe == null) {
            return;
        }
        ItemStack stack = IndexRecipeUi.recipeIcon(recipe, ItemStack.EMPTY);
        if (stack.isEmpty()) {
            return;
        }
        IndexRecipeUi.ViewMode mode = IndexRecipeUi.sourceCard(recipe)
                ? IndexRecipeUi.ViewMode.SOURCES
                : IndexRecipeUi.ViewMode.RECIPES;
        openDetail(stack, mode);
        detailCategory = recipe.categoryId();
        List<IndexRecipeView> views = detailViews();
        for (int i = 0; i < views.size(); i++) {
            if (views.get(i).id().equals(recipe.id())) {
                detailSelected = i;
                break;
            }
        }
    }

    private static void toggleFocusedRecipePin() {
        List<IndexRecipeView> views = detailViews();
        if (views.isEmpty()) {
            return;
        }
        IndexRecipeView recipe = views.get(clamp(detailSelected, 0, views.size() - 1));
        boolean pinned = ClientIndexState.isRecipePinned(recipe.id());
        sendRecipeAction(pinned ? IndexActionPacket.Action.UNPIN_RECIPE : IndexActionPacket.Action.PIN_RECIPE,
                recipe.id());
    }

    private static void setDetailMode(IndexRecipeUi.ViewMode mode) {
        detailMode = mode == null ? IndexRecipeUi.ViewMode.RECIPES : mode;
        detailCategory = null;
        detailSelected = 0;
        if (!detailStack.isEmpty()) {
            pushHistory(detailStack, detailMode);
        }
        saveScreenState();
    }

    private static void pushHistory(ItemStack stack, IndexRecipeUi.ViewMode mode) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        HistoryEntry entry = new HistoryEntry(IndexService.itemId(stack.getItem()), mode);
        if (historyIndex >= 0 && historyIndex < DETAIL_HISTORY.size() && DETAIL_HISTORY.get(historyIndex).equals(entry)) {
            return;
        }
        while (DETAIL_HISTORY.size() > historyIndex + 1) {
            DETAIL_HISTORY.removeLast();
        }
        DETAIL_HISTORY.add(entry);
        while (DETAIL_HISTORY.size() > MAX_HISTORY) {
            DETAIL_HISTORY.removeFirst();
        }
        historyIndex = DETAIL_HISTORY.size() - 1;
    }

    private static boolean historyBack() {
        if (historyIndex <= 0) {
            return false;
        }
        historyIndex--;
        restoreHistory(DETAIL_HISTORY.get(historyIndex));
        return true;
    }

    private static boolean historyForward() {
        if (historyIndex + 1 >= DETAIL_HISTORY.size()) {
            return false;
        }
        historyIndex++;
        restoreHistory(DETAIL_HISTORY.get(historyIndex));
        return true;
    }

    private static void restoreHistory(HistoryEntry entry) {
        ItemStack stack = itemStack(entry.itemId());
        if (stack.isEmpty()) {
            return;
        }
        detailStack = stack;
        detailMode = entry.mode();
        detailCategory = null;
        detailSelected = 0;
        searchFocused = false;
        for (int i = 0; i < DETAIL_HISTORY.size(); i++) {
            if (DETAIL_HISTORY.get(i).equals(entry)) {
                historyIndex = i;
                break;
            }
        }
        saveScreenState();
    }

    private static List<HistoryEntry> recentHistory() {
        List<HistoryEntry> recent = new ArrayList<>();
        for (int i = DETAIL_HISTORY.size() - 1; i >= 0 && recent.size() < 5; i--) {
            HistoryEntry entry = DETAIL_HISTORY.get(i);
            if (recent.stream().noneMatch(existing -> existing.itemId().equals(entry.itemId()))) {
                recent.add(entry);
            }
        }
        return recent;
    }

    private static IndexRecipeScreen.Mode screenMode(IndexRecipeUi.ViewMode mode) {
        return switch (mode) {
            case USES -> IndexRecipeScreen.Mode.USES;
            case SOURCES -> IndexRecipeScreen.Mode.SOURCES;
            case RECIPES -> IndexRecipeScreen.Mode.RECIPES;
        };
    }

    private static void syncScreenState(Screen screen) {
        String key = screen.getClass().getName();
        if (key.equals(activeScreenKey)) {
            return;
        }
        saveScreenState();
        activeScreenKey = key;
        requestServerSync(false);
        OverlayScreenState state = SCREEN_STATES.get(key);
        if (state == null) {
            collapsed = false;
            gridDensity = Config.OVERLAY_GRID_DENSITY.get();
            gridDensityOverridden = false;
            detailStack = ItemStack.EMPTY;
            detailMode = IndexRecipeUi.ViewMode.RECIPES;
            detailCategory = null;
            detailSelected = 0;
            return;
        }
        collapsed = state.collapsed();
        gridDensity = state.gridDensity();
        gridDensityOverridden = true;
        detailMode = state.mode();
        detailCategory = state.category();
        detailSelected = state.selected();
        detailStack = itemStack(state.itemId());
    }

    private static void requestServerSync(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastSyncRequestMillis < 5000L) {
            return;
        }
        lastSyncRequestMillis = now;
        EchoNetClientActions.sendServerboundAction(new IndexActionPacket(IndexActionPacket.Action.REQUEST_SYNC, null));
    }

    private static void saveScreenState() {
        if (activeScreenKey.isBlank()) {
            return;
        }
        SCREEN_STATES.put(activeScreenKey, new OverlayScreenState(collapsed,
                detailStack.isEmpty() ? null : IndexService.itemId(detailStack.getItem()),
                detailMode, detailCategory, detailSelected, currentGridDensity()));
    }

    private static ItemStack itemStack(Identifier id) {
        if (id == null) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    private static void setFilter(String filter) {
        String next = filter == null ? "" : filter;
        categoryFilter = next.equals(categoryFilter) ? "" : next;
        searchFocused = false;
        markGroupStateChanged();
        closePopup();
        resetGridScroll();
    }

    private static void clearFilters(boolean includeSearch) {
        categoryFilter = "";
        bookmarkedOnly = false;
        showOnlyGroup = "";
        if (includeSearch) {
            search = "";
            markSearchEdited();
        }
        searchFocused = false;
        markGroupStateChanged();
        closePopup();
        resetGridScroll();
    }

    private static void clearSearch() {
        if (search.isBlank()) {
            searchFocused = true;
            return;
        }
        search = "";
        searchFocused = true;
        markSearchEdited();
        resetGridScroll();
    }

    private static String effectiveSearch() {
        if (search.isBlank()) {
            pendingSearch = "";
            debouncedSearch = "";
            return "";
        }
        long now = System.currentTimeMillis();
        if (!search.equals(pendingSearch)) {
            pendingSearch = search;
            searchEditedAt = now;
        }
        if (now - searchEditedAt >= SEARCH_DEBOUNCE_MS) {
            debouncedSearch = search.trim();
        }
        return debouncedSearch;
    }

    private static void markSearchEdited() {
        pendingSearch = search;
        searchEditedAt = System.currentTimeMillis();
        if (search.isBlank()) {
            debouncedSearch = "";
        }
        gridCacheKey = null;
        groupViewCacheKey = null;
    }

    private static void commitSearchNow() {
        pendingSearch = search;
        debouncedSearch = search.trim();
        searchEditedAt = 0L;
        gridCacheKey = null;
        groupViewCacheKey = null;
    }

    private static void toggleBookmark(Identifier id) {
        boolean currently = ClientIndexState.isBookmarked(id);
        EchoNetClientActions.sendServerboundAction(new IndexActionPacket(
                currently ? IndexActionPacket.Action.UNBOOKMARK : IndexActionPacket.Action.BOOKMARK,
                id));
    }

    private static void sendRecipeAction(IndexActionPacket.Action action, Identifier recipeId) {
        if (recipeId != null) {
            EchoNetClientActions.sendServerboundAction(new IndexActionPacket(action, recipeId));
        }
    }

    private static void resetGridScroll() {
        scroll = 0;
        horizontalScroll = 0;
    }

    private static void drawGridScrollbars(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, int contentW, int contentH) {
        verticalScrollbar = null;
        horizontalScrollbar = null;
        int maxVerticalScroll = Math.max(0, contentH - h);
        int maxHorizontalScroll = Math.max(0, contentW - w);
        if (maxVerticalScroll > 0) {
            int trackX = x + w - 7;
            int trackW = 6;
            int thumbH = Math.max(14, h * h / Math.max(h, contentH));
            int thumbY = y + (h - thumbH) * scroll / maxVerticalScroll;
            IndexThemeStyle.scrollbar(graphics, trackX, y, trackW, h, trackX, thumbY, trackW, thumbH);
            verticalScrollbar = new ScrollbarMetrics(trackX, y, trackW, h, trackX, thumbY, trackW, thumbH,
                    maxVerticalScroll, true);
            tooltipIfHovered(graphics, font, lastMouseX, lastMouseY, trackX, y, trackW, h,
                    tr("screen.echoindex.overlay.tooltip.scroll_vertical"));
        }
        if (maxHorizontalScroll > 0) {
            int trackY = y + h - 7;
            int trackH = 6;
            int thumbW = Math.max(14, w * w / Math.max(w, contentW));
            int thumbX = x + (w - thumbW) * horizontalScroll / maxHorizontalScroll;
            IndexThemeStyle.scrollbar(graphics, x, trackY, w, trackH, thumbX, trackY, thumbW, trackH);
            horizontalScrollbar = new ScrollbarMetrics(x, trackY, w, trackH, thumbX, trackY, thumbW, trackH,
                    maxHorizontalScroll, false);
            tooltipIfHovered(graphics, font, lastMouseX, lastMouseY, x, trackY, w, trackH,
                    tr("screen.echoindex.overlay.tooltip.scroll_horizontal"));
        }
    }

    private static List<Component> itemTooltip(ItemStack stack) {
        List<Component> tooltip = new ArrayList<>(IndexTooltipUtil.itemTooltip(stack,
                tr("screen.echoindex.overlay.tooltip.item_actions")));
        String query = effectiveSearch();
        if (activeBrowseFilter(query) && currentMatchedItemKeys.contains(stackKey(stack))) {
            tooltip.add(Component.literal(text("screen.echoindex.overlay.tooltip.match", query))
                    .withStyle(ChatFormatting.AQUA));
        }
        return tooltip;
    }

    private static boolean beginScrollbarDrag(double mouseX, double mouseY) {
        if (verticalScrollbar != null && verticalScrollbar.insideTrack(mouseX, mouseY)) {
            dragMode = DragMode.VERTICAL_SCROLL;
            dragThumbOffset = verticalScrollbar.insideThumb(mouseX, mouseY)
                    ? (int) Math.round(mouseY) - verticalScrollbar.thumbY()
                    : verticalScrollbar.thumbH() / 2;
            updateScrollbarDrag(mouseX, mouseY);
            return true;
        }
        if (horizontalScrollbar != null && horizontalScrollbar.insideTrack(mouseX, mouseY)) {
            dragMode = DragMode.HORIZONTAL_SCROLL;
            dragThumbOffset = horizontalScrollbar.insideThumb(mouseX, mouseY)
                    ? (int) Math.round(mouseX) - horizontalScrollbar.thumbX()
                    : horizontalScrollbar.thumbW() / 2;
            updateScrollbarDrag(mouseX, mouseY);
            return true;
        }
        return false;
    }

    private static void updateScrollbarDrag(double mouseX, double mouseY) {
        ScrollbarMetrics metrics = dragMode == DragMode.VERTICAL_SCROLL ? verticalScrollbar : horizontalScrollbar;
        if (metrics == null) {
            return;
        }
        int trackStart = metrics.vertical() ? metrics.trackY() : metrics.trackX();
        int trackSize = metrics.vertical() ? metrics.trackH() : metrics.trackW();
        int thumbSize = metrics.vertical() ? metrics.thumbH() : metrics.thumbW();
        int trackRange = Math.max(1, trackSize - thumbSize);
        int mouse = (int) Math.round(metrics.vertical() ? mouseY : mouseX);
        int thumbStart = clamp(mouse - dragThumbOffset, trackStart, trackStart + trackRange);
        int nextScroll = (int) Math.round((thumbStart - trackStart) * (double) metrics.maxScroll() / trackRange);
        if (metrics.vertical()) {
            scroll = clamp(nextScroll, 0, metrics.maxScroll());
        } else {
            horizontalScroll = clamp(nextScroll, 0, metrics.maxScroll());
        }
    }

    private static void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, String label, boolean active) {
        IndexThemeStyle.button(graphics, font, x, y, w, h, label, false, active, CYAN);
    }

    private static void compactButton(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h,
            String label, boolean active, int mouseX, int mouseY) {
        boolean hover = active && inside(mouseX, mouseY, x, y, w, h);
        IndexThemeStyle.button(graphics, font, x, y, w, h, trim(font, label, w - 6), hover, active, CYAN);
    }

    private static void drawPopup(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        if (popupKind == PopupKind.GROUP_ACTIONS) {
            drawGroupPopup(graphics, font, mouseX, mouseY);
        } else if (popupKind == PopupKind.OPTIONS) {
            drawOptionsPopup(graphics, font, mouseX, mouseY);
        }
    }

    private static void drawGroupPopup(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        int w = 118;
        int h = 76;
        drawMenuPanel(graphics, popupX, popupY, w, h);
        int y = popupY + 4;
        boolean pinned = PINNED_GROUPS.contains(popupGroupId);
        menuItem(graphics, font, popupX + 4, y, w - 8,
                pinned ? text("screen.echoindex.overlay.action.unpin_mod") : text("screen.echoindex.overlay.action.pin_mod"),
                mouseX, mouseY, () -> {
                    if (!PINNED_GROUPS.add(popupGroupId)) {
                        PINNED_GROUPS.remove(popupGroupId);
                    }
                    markGroupStateChanged();
                    closePopup();
                });
        y += 17;
        menuItem(graphics, font, popupX + 4, y, w - 8, text("screen.echoindex.overlay.action.hide_mod"),
                mouseX, mouseY, () -> {
                    HIDDEN_GROUPS.add(popupGroupId);
                    COLLAPSED_GROUPS.remove(popupGroupId);
                    FULLY_EXPANDED_GROUPS.remove(popupGroupId);
                    markGroupStateChanged();
                    closePopup();
                });
        y += 17;
        menuItem(graphics, font, popupX + 4, y, w - 8,
                showOnlyGroup.equals(popupGroupId)
                        ? text("screen.echoindex.overlay.action.show_all_mods")
                        : text("screen.echoindex.overlay.action.show_only_mod"),
                mouseX, mouseY, () -> {
                    showOnlyGroup = showOnlyGroup.equals(popupGroupId) ? "" : popupGroupId;
                    FULLY_EXPANDED_GROUPS.clear();
                    markGroupStateChanged();
                    resetGridScroll();
                    closePopup();
                });
        y += 17;
        menuItem(graphics, font, popupX + 4, y, w - 8, text("screen.echoindex.overlay.action.copy_mod_id"),
                mouseX, mouseY, () -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(popupGroupId);
                    closePopup();
                });
    }

    private static void drawOptionsPopup(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        int w = 154;
        int h = 156;
        drawMenuPanel(graphics, popupX, popupY, w, h);
        int y = popupY + 4;
        y = optionItem(graphics, font, y, w, mouseX, mouseY, groupMode == GroupMode.MOD,
                text("screen.echoindex.overlay.option.group_mod"), () -> setGroupMode(GroupMode.MOD));
        y = optionItem(graphics, font, y, w, mouseX, mouseY, groupMode == GroupMode.CATEGORY,
                text("screen.echoindex.overlay.option.group_category"), () -> setGroupMode(GroupMode.CATEGORY));
        y = optionItem(graphics, font, y, w, mouseX, mouseY, indexViewMode == IndexViewMode.COMPACT,
                text("screen.echoindex.overlay.option.compact"), () -> setIndexViewMode(IndexViewMode.COMPACT));
        y = optionItem(graphics, font, y, w, mouseX, mouseY, indexViewMode == IndexViewMode.DETAILED,
                text("screen.echoindex.overlay.option.detailed"), () -> setIndexViewMode(IndexViewMode.DETAILED));
        y = optionItem(graphics, font, y, w, mouseX, mouseY, showHiddenMods,
                text("screen.echoindex.overlay.option.show_hidden"), () -> {
                    showHiddenMods = !showHiddenMods;
                    markGroupStateChanged();
                    closePopup();
                });
        y = optionItem(graphics, font, y, w, mouseX, mouseY, showEmptyMods,
                text("screen.echoindex.overlay.option.show_empty"), () -> {
                    showEmptyMods = !showEmptyMods;
                    markGroupStateChanged();
                    closePopup();
                });
        y = optionItem(graphics, font, y, w, mouseX, mouseY, minecraftFirst,
                text("screen.echoindex.overlay.option.minecraft_first"), () -> {
                    minecraftFirst = !minecraftFirst;
                    markGroupStateChanged();
                    closePopup();
                });
        y = optionItem(graphics, font, y, w, mouseX, mouseY, echoPriority,
                text("screen.echoindex.overlay.option.echo_priority"), () -> {
                    echoPriority = !echoPriority;
                    markGroupStateChanged();
                    closePopup();
                });
        menuItem(graphics, font, popupX + 4, y, w - 8, text("screen.echoindex.overlay.option.reset"),
                mouseX, mouseY, IndexOverlay::resetIndexLayout);
    }

    private static int optionItem(GuiGraphicsExtractor graphics, Font font, int y, int w, int mouseX, int mouseY,
            boolean selected, String label, Runnable action) {
        menuItem(graphics, font, popupX + 4, y, w - 8, (selected ? "* " : "  ") + label, mouseX, mouseY, action);
        return y + 17;
    }

    private static void drawMenuPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        EchoCyberGlassUi.calmPanel(graphics, x, y, w, h, BG, CYAN);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0x7766E8FF);
    }

    private static void menuItem(GuiGraphicsExtractor graphics, Font font, int x, int y, int w,
            String label, int mouseX, int mouseY, Runnable action) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 16);
        graphics.fill(x, y, x + w, y + 16, hover ? 0x88123241 : 0x52071117);
        graphics.text(font, trim(font, label, w - 8), x + 4, y + 5, hover ? TEXT : MUTED, false);
        HITBOXES.add(new Hitbox(x, y, w, 16, (button, modifiers) -> action.run()));
    }

    private static void openGroupPopup(String groupId, int mouseX, int mouseY) {
        popupKind = PopupKind.GROUP_ACTIONS;
        popupGroupId = groupId == null ? "" : groupId;
        popupX = clamp(mouseX, panelX + 6, Math.max(panelX + 6, panelX + panelW - 124));
        popupY = clamp(mouseY, panelY + 6, Math.max(panelY + 6, panelY + panelH - 84));
        searchFocused = false;
    }

    private static void toggleOptionsPopup(int x, int y) {
        if (popupKind == PopupKind.OPTIONS) {
            closePopup();
            return;
        }
        popupKind = PopupKind.OPTIONS;
        popupGroupId = "";
        popupX = clamp(x, panelX + 6, Math.max(panelX + 6, panelX + panelW - 160));
        popupY = clamp(y, panelY + 6, Math.max(panelY + 6, panelY + panelH - 162));
        searchFocused = false;
    }

    private static void closePopup() {
        popupKind = PopupKind.NONE;
        popupGroupId = "";
    }

    private static void setGroupMode(GroupMode mode) {
        groupMode = mode == null ? GroupMode.MOD : mode;
        showOnlyGroup = "";
        COLLAPSED_GROUPS.clear();
        FULLY_EXPANDED_GROUPS.clear();
        markGroupStateChanged();
        resetGridScroll();
        closePopup();
    }

    private static void setIndexViewMode(IndexViewMode mode) {
        indexViewMode = mode == null ? IndexViewMode.COMPACT : mode;
        markGroupStateChanged();
        closePopup();
    }

    private static void resetIndexLayout() {
        COLLAPSED_GROUPS.clear();
        FULLY_EXPANDED_GROUPS.clear();
        PINNED_GROUPS.clear();
        HIDDEN_GROUPS.clear();
        showHiddenMods = false;
        showEmptyMods = false;
        minecraftFirst = true;
        echoPriority = true;
        showOnlyGroup = "";
        groupMode = GroupMode.MOD;
        indexViewMode = IndexViewMode.COMPACT;
        PANEL_BOUNDS.remove(activeScreenKey);
        categoryFilter = "";
        bookmarkedOnly = false;
        search = "";
        markSearchEdited();
        markGroupStateChanged();
        resetGridScroll();
        closePopup();
    }

    private static void renderCoreFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        try {
            Class.forName("com.knoxhack.echoindex.integration.IndexRenderCoreScreenIntegration")
                    .getMethod("drawOverlayFrame", GuiGraphicsExtractor.class, int.class, int.class, int.class, int.class)
                    .invoke(null, graphics, x, y, width, height);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    private static boolean cinematicStyle() {
        try {
            return Config.UI_CINEMATIC_STYLE.get();
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private static void chip(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, String label,
            boolean selected, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 17);
        IndexThemeStyle.chip(graphics, font, x, y, w, 17, trim(font, label, w - 8), selected, hover);
    }

    private static void refreshThemeAliases() {
        IndexRecipeUi.refreshTheme();
        BG = IndexThemeStyle.alpha(IndexRecipeUi.BG, 232);
        PANEL = IndexRecipeUi.PANEL;
        ROW = IndexRecipeUi.ROW;
        TEXT = IndexRecipeUi.TEXT;
        MUTED = IndexRecipeUi.MUTED;
        CYAN = IndexRecipeUi.CYAN;
        WARN = IndexRecipeUi.WARN;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private static boolean shiftDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String trim(Font font, String text, int width) {
        return IndexRecipeUi.trim(font, text, width);
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private static String text(String key, Object... args) {
        return tr(key, args).getString();
    }

    private static String stackKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return IndexService.itemId(stack.getItem()) + "#" + ItemStack.hashItemAndComponents(stack);
    }

    private static String modeLabel(IndexRecipeUi.ViewMode mode) {
        return switch (mode) {
            case USES -> text("screen.echoindex.uses");
            case SOURCES -> text("screen.echoindex.sources");
            case RECIPES -> text("screen.echoindex.recipes");
        };
    }

    private static void tooltipIfHovered(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY,
            int x, int y, int w, int h, Component... lines) {
        if (lines.length > 0 && inside(mouseX, mouseY, x, y, w, h)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(lines), mouseX, mouseY);
        }
    }

    private record GridCacheKey(String screenKey, String query, long clientRevision) {
    }

    private record GroupViewCacheKey(String screenKey, long catalogRevision, long clientRevision, String query,
            String categoryFilter, boolean bookmarkedOnly, GroupMode groupMode, IndexViewMode viewMode,
            boolean showHiddenMods, boolean showEmptyMods, boolean minecraftFirst, boolean echoPriority,
            String showOnlyGroup, int stateRevision) {
    }

    private record DetailBaseCacheKey(String screenKey, Identifier itemId, IndexRecipeUi.ViewMode mode,
            long recipeSnapshotGeneration, long clientRevision, long queryRevision) {
    }

    private record DetailCacheKey(DetailBaseCacheKey base, Identifier category) {
    }

    private record ModeCountKey(String screenKey, Identifier itemId, IndexRecipeUi.ViewMode mode,
            long recipeSnapshotGeneration, long clientRevision, long queryRevision) {
    }

    private record ChipCursor(int x, int y) {
    }

    private record Hitbox(int x, int y, int w, int h, ClickAction action) {
    }

    private record PanelBounds(int x, int y, int w, int h) {
    }

    private record GridLayout(int slot, int step, int columns) {
        int columnOffset(int column) {
            return column * step;
        }
    }

    private record GroupMetrics(List<GroupLayout> layouts, int contentHeight) {
    }

    private record GroupLayout(IndexModGroup group, int y, int height, int rows, int columns, int slot, int step,
            boolean fullExpanded) {
    }

    private record CategoryBucket(String id, String label, ItemStack icon, int accentColor, ArrayList<ItemStack> items) {
        private CategoryBucket(String id, String label, ItemStack icon, int accentColor) {
            this(id, label, icon, accentColor, new ArrayList<>());
        }

        private IndexModGroup toGroup() {
            return new IndexModGroup(id, label, icon, items.size(), items, items, false, false, false, "", accentColor);
        }
    }

    private record ScrollbarMetrics(int trackX, int trackY, int trackW, int trackH,
            int thumbX, int thumbY, int thumbW, int thumbH, int maxScroll, boolean vertical) {
        boolean insideTrack(double mouseX, double mouseY) {
            return inside(mouseX, mouseY, trackX, trackY, trackW, trackH);
        }

        boolean insideThumb(double mouseX, double mouseY) {
            return inside(mouseX, mouseY, thumbX, thumbY, thumbW, thumbH);
        }
    }

    private record OverlayScreenState(boolean collapsed, Identifier itemId, IndexRecipeUi.ViewMode mode,
            Identifier category, int selected, Config.GridDensity gridDensity) {
    }

    private record HistoryEntry(Identifier itemId, IndexRecipeUi.ViewMode mode) {
    }

    @FunctionalInterface
    private interface ClickAction {
        void click(int button, int modifiers);
    }

    private enum DragMode {
        NONE,
        MOVE,
        RESIZE,
        VERTICAL_SCROLL,
        HORIZONTAL_SCROLL
    }

    private enum GroupMode {
        MOD,
        CATEGORY
    }

    private enum IndexViewMode {
        COMPACT,
        DETAILED
    }

    private enum PopupKind {
        NONE,
        GROUP_ACTIONS,
        OPTIONS
    }
}
