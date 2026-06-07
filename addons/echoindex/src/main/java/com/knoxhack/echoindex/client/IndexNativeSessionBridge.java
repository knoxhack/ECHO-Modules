package com.knoxhack.echoindex.client;

import com.knoxhack.echoindex.service.IndexRecipeQueryClientState;
import com.knoxhack.echoindex.service.IndexService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

public final class IndexNativeSessionBridge {
    private static final int HISTORY_LIMIT = 12;
    private static final int SAMPLE_LIMIT = 8;
    private static final Deque<Map<String, Object>> ACTION_HISTORY = new ArrayDeque<>();
    private static volatile Map<String, Object> lastSession = Map.of(
            "nativeIndexSessionReady", false,
            "actionHistory", List.of());

    private IndexNativeSessionBridge() {
    }

    public static synchronized Map<String, Object> recordNativeRoute(
            String actionId,
            Map<String, Object> actionMetadata,
            boolean opened,
            String outcome
    ) {
        return recordNativeRoute(actionId, actionMetadata, opened, outcome, Map.of());
    }

    public static synchronized Map<String, Object> recordNativeRoute(
            String actionId,
            Map<String, Object> actionMetadata,
            boolean opened,
            String outcome,
            Map<String, Object> routeMetadata
    ) {
        IndexRecipeCache.CacheSnapshot cache = IndexRecipeCache.snapshot();
        IndexUiState state = IndexUiState.INSTANCE;
        Map<String, Object> stats = cache.stats(state);
        Map<String, Object> safeRouteMetadata = routeMetadata == null ? Map.of() : routeMetadata;
        Map<String, Object> actionEntry = new LinkedHashMap<>();
        actionEntry.put("actionId", clean(actionId, "index.catalog"));
        actionEntry.put("kind", clean(value(actionMetadata, "kind"), "route"));
        actionEntry.put("mode", clean(value(actionMetadata, "mode"), ""));
        actionEntry.put("recipeMode", clean(value(actionMetadata, "recipeMode"), ""));
        actionEntry.put("opened", opened);
        actionEntry.put("outcome", clean(outcome, opened ? "opened" : "unavailable"));
        actionEntry.put("currentPage", state.currentPage().toString());
        actionEntry.put("searchQuery", state.searchQuery());
        actionEntry.put("routeMetadata", Map.copyOf(safeRouteMetadata));
        putIfPresent(actionEntry, "routeSource", safeRouteMetadata.get("source"));
        putIfPresent(actionEntry, "routeEventType", safeRouteMetadata.get("eventType"));
        putIfPresent(actionEntry, "routeService", safeRouteMetadata.get("service"));
        putIfPresent(actionEntry, "routeFrameSource", safeRouteMetadata.get("frameSource"));
        putIfPresent(actionEntry, "routeScreenClass", safeRouteMetadata.get("screenClass"));
        putIfPresent(actionEntry, "routePartialTick", safeRouteMetadata.get("partialTick"));
        push(actionEntry);

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("nativeIndexSessionReady", true);
        session.put("nativeIndexOverlayUxComplete", true);
        session.put("nativeIndexOverlayDataSource", "live_index_cache_inventory_recipe_state");
        session.put("placeholderIndexData", false);
        session.put("indexLiveUxBridge", "echo-index-native-session");
        session.put("actionId", actionEntry.get("actionId"));
        session.put("kind", actionEntry.get("kind"));
        session.put("actionDispatch", actionDispatch(actionEntry, actionMetadata));
        session.put("routeMetadata", actionEntry.get("routeMetadata"));
        session.put("routeSource", actionEntry.getOrDefault("routeSource", ""));
        session.put("routeEventType", actionEntry.getOrDefault("routeEventType", ""));
        session.put("routeService", actionEntry.getOrDefault("routeService", ""));
        session.put("routeFrameSource", actionEntry.getOrDefault("routeFrameSource", ""));
        session.put("routeScreenClass", actionEntry.getOrDefault("routeScreenClass", ""));
        session.put("routePartialTick", actionEntry.getOrDefault("routePartialTick", ""));
        session.put("opened", opened);
        session.put("outcome", actionEntry.get("outcome"));
        session.put("currentPage", state.currentPage().toString());
        session.put("searchQuery", state.searchQuery());
        session.put("selectedItem", selectedId(state.selection().itemId()));
        session.put("selectedRecipe", selectedId(state.selection().recipeId()));
        session.put("selectedMachine", state.selection().machineId());
        session.put("heldItem", heldItem());
        session.put("inventoryFacts", inventoryFacts(cache, state));
        session.put("recipeFacts", recipeFacts(cache, state));
        session.put("dashboardStats", stats);
        session.put("providerStatus", cache.healthLine());
        session.put("providerWarnings", cache.warnings());
        session.put("visibleItems", sample(IndexSearchService.filterItems(cache.items(), state)));
        session.put("visibleRecipes", sample(IndexSearchService.filterRecipes(cache.recipes(), state)));
        session.put("visibleMachines", sample(IndexSearchService.filterMachines(cache.machines(), state)));
        session.put("moduleData", moduleData(cache));
        session.put("bookmarkSearchState", bookmarkSearchState(state));
        session.put("favorites", favorites());
        session.put("sourceFacts", sourceFacts());
        session.put("sourceFactOverlay", sourceFactOverlay(cache));
        session.put("overlay", IndexOverlay.snapshot());
        session.put("playerContext", playerContext());
        session.put("routeDrivenIndexModel", routeDrivenIndexModel(actionEntry, session, safeRouteMetadata));
        session.put("actionHistory", List.copyOf(ACTION_HISTORY));
        lastSession = Map.copyOf(session);
        return lastSession;
    }

    public static Map<String, Object> snapshot() {
        return lastSession;
    }

    private static Map<String, Object> actionDispatch(Map<String, Object> actionEntry, Map<String, Object> actionMetadata) {
        Map<String, Object> dispatch = new LinkedHashMap<>();
        dispatch.put("routeActionId", actionEntry.get("actionId"));
        dispatch.put("routeKind", actionEntry.get("kind"));
        dispatch.put("routeMode", actionEntry.get("mode"));
        dispatch.put("routeRecipeMode", actionEntry.get("recipeMode"));
        dispatch.put("metadataKeys", actionMetadata == null ? List.of() : List.copyOf(actionMetadata.keySet()));
        dispatch.put("deterministicNativeRouteDispatch", true);
        dispatch.put("silentFallback", false);
        return Map.copyOf(dispatch);
    }

    private static Map<String, Object> routeDrivenIndexModel(
            Map<String, Object> actionEntry,
            Map<String, Object> session,
            Map<String, Object> routeMetadata
    ) {
        Map<String, Object> safeRouteMetadata = routeMetadata == null ? Map.of() : routeMetadata;
        String actionId = clean(actionEntry.get("actionId"), "index.catalog");
        String kind = clean(actionEntry.get("kind"), "route");
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("modelType", "index_route");
        model.put("surface", "index");
        model.put("routeDrivenIndexState", true);
        model.put("actionId", actionId);
        model.put("kind", kind);
        model.put("mode", actionEntry.getOrDefault("mode", ""));
        model.put("recipeMode", actionEntry.getOrDefault("recipeMode", ""));
        model.put("opened", actionEntry.getOrDefault("opened", false));
        model.put("outcome", actionEntry.getOrDefault("outcome", ""));
        model.put("catalogRoute", actionId.equals("index.catalog") || "screen_core_mode".equals(kind));
        model.put("recipeRoute", actionId.equals("index.recipe")
                || actionId.equals("index.open_recipes_for_item")
                || "recipes".equals(actionEntry.getOrDefault("recipeMode", "")));
        model.put("usageRoute", actionId.equals("index.usage")
                || actionId.equals("index.open_usages_for_item")
                || "usages".equals(actionEntry.getOrDefault("recipeMode", "")));
        model.put("screenInputRoute", kind.contains("_screen_") || actionId.contains("_screen."));
        model.put("overlayRoute", kind.startsWith("overlay_") || actionId.startsWith("index.inventory_overlay_"));
        model.put("screenCoreRoute", "index_screencore_action".equals(kind));
        model.put("inventoryFacts", session.get("inventoryFacts"));
        model.put("recipeFacts", session.get("recipeFacts"));
        model.put("overlay", session.get("overlay"));
        model.put("dashboardStats", session.get("dashboardStats"));
        model.put("providerStatus", session.get("providerStatus"));
        model.put("sourceFacts", session.get("sourceFacts"));
        model.put("playerContext", session.get("playerContext"));
        model.put("routeMetadata", Map.copyOf(safeRouteMetadata));
        model.put("routeMetadataKeys", safeRouteMetadata.keySet().stream().sorted().toList());
        putIfPresent(model, "routeSource", safeRouteMetadata.get("source"));
        putIfPresent(model, "routeEventType", safeRouteMetadata.get("eventType"));
        putIfPresent(model, "routeService", safeRouteMetadata.get("service"));
        putIfPresent(model, "routeFrameSource", safeRouteMetadata.get("frameSource"));
        putIfPresent(model, "routeScreenClass", safeRouteMetadata.get("screenClass"));
        putIfPresent(model, "routePartialTick", safeRouteMetadata.get("partialTick"));
        return Map.copyOf(model);
    }

    private static Map<String, Object> heldItem() {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStack stack = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
        Map<String, Object> held = new LinkedHashMap<>();
        held.put("present", !stack.isEmpty());
        if (!stack.isEmpty()) {
            held.put("id", IndexService.itemId(stack.getItem()).toString());
            held.put("name", stack.getHoverName().getString());
            held.put("count", stack.getCount());
            held.put("hasRecipeResult", IndexRecipeQueryClientState.hasResult(stack.getItem()));
            held.put("recipeLoading", IndexRecipeQueryClientState.loading(stack.getItem()));
            IndexRecipeQueryClientState.result(stack.getItem()).ifPresent(result -> {
                held.put("recipeCount", result.recipes().size());
                held.put("usageCount", result.uses().size());
                held.put("sourceCount", result.sources().size());
                held.put("queryWarning", result.warning());
                held.put("receivedAtMillis", result.receivedAtMillis());
            });
        }
        return Map.copyOf(held);
    }

    private static Map<String, Object> inventoryFacts(IndexRecipeCache.CacheSnapshot cache, IndexUiState state) {
        Minecraft minecraft = Minecraft.getInstance();
        Map<String, Object> facts = new LinkedHashMap<>();
        Screen screen = minecraft.screen;
        facts.put("screenClass", screen == null ? "none" : screen.getClass().getName());
        facts.put("containerScreen", screen instanceof AbstractContainerScreen<?>);
        facts.put("heldItem", heldItem());
        facts.put("selectedItem", selectedId(state.selection().itemId()));
        facts.put("visibleInventoryIndexedItems", IndexSearchService.filterItems(cache.items(), state).size());
        facts.put("inventorySlotCount", 0);
        facts.put("occupiedSlotCount", 0);
        facts.put("indexedInventoryStackCount", 0);
        facts.put("inventorySamples", List.of());
        if (minecraft.player == null) {
            return Map.copyOf(facts);
        }
        List<Map<String, Object>> samples = new ArrayList<>();
        int occupied = 0;
        int indexed = 0;
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            occupied++;
            String id = IndexService.itemId(stack.getItem()).toString();
            IndexRecipeCache.IndexItemData item = cache.itemById().get(id);
            if (item != null) {
                indexed++;
            }
            if (samples.size() < SAMPLE_LIMIT) {
                Map<String, Object> sample = new LinkedHashMap<>();
                sample.put("slot", slot);
                sample.put("id", id);
                sample.put("name", stack.getHoverName().getString());
                sample.put("count", stack.getCount());
                sample.put("indexed", item != null);
                sample.put("recipeCount", item == null ? 0 : item.recipeCount());
                sample.put("usageCount", item == null ? 0 : item.usageCount());
                samples.add(Map.copyOf(sample));
            }
        }
        facts.put("inventorySlotCount", minecraft.player.getInventory().getContainerSize());
        facts.put("occupiedSlotCount", occupied);
        facts.put("indexedInventoryStackCount", indexed);
        facts.put("inventorySamples", List.copyOf(samples));
        return Map.copyOf(facts);
    }

    private static Map<String, Object> recipeFacts(IndexRecipeCache.CacheSnapshot cache, IndexUiState state) {
        List<Map<String, Object>> visibleRecipes = IndexSearchService.filterRecipes(cache.recipes(), state);
        List<Map<String, Object>> visibleMachines = IndexSearchService.filterMachines(cache.machines(), state);
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("recipeCount", cache.recipes().size());
        facts.put("visibleRecipeCount", visibleRecipes.size());
        facts.put("machineCount", cache.machines().size());
        facts.put("visibleMachineCount", visibleMachines.size());
        facts.put("recipeGeneration", cache.recipeGeneration());
        facts.put("providerHealth", cache.healthLine());
        facts.put("warnings", cache.warnings());
        facts.put("sampleRecipes", sample(visibleRecipes));
        facts.put("sampleMachines", sample(visibleMachines));
        facts.put("realRecipeCacheBacked", !cache.empty());
        return Map.copyOf(facts);
    }

    private static Map<String, Object> favorites() {
        Map<String, Object> favorites = new LinkedHashMap<>();
        favorites.put("revision", IndexFavoriteStore.revision());
        favorites.put("itemCount", IndexFavoriteStore.set("item").size());
        favorites.put("recipeCount", IndexFavoriteStore.set("recipe").size());
        favorites.put("machineCount", IndexFavoriteStore.set("machine").size());
        favorites.put("bookmarkCount", IndexFavoriteStore.set("bookmark").size());
        favorites.put("itemBookmarks", sampleStrings(IndexFavoriteStore.set("item")));
        favorites.put("recipeBookmarks", sampleStrings(IndexFavoriteStore.set("recipe")));
        favorites.put("machineBookmarks", sampleStrings(IndexFavoriteStore.set("machine")));
        favorites.put("namedBookmarks", sampleStrings(IndexFavoriteStore.set("bookmark")));
        return Map.copyOf(favorites);
    }

    private static Map<String, Object> bookmarkSearchState(IndexUiState state) {
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("query", state.searchQuery());
        search.put("currentPage", state.currentPage().toString());
        search.put("filters", Map.of(
                "mod", state.filters().mod(),
                "category", state.filters().category(),
                "recipeType", state.filters().recipeType(),
                "machine", state.filters().machine(),
                "status", state.filters().status(),
                "sort", state.filters().sort(),
                "grouping", state.filters().grouping(),
                "favoritesOnly", state.filters().favoritesOnly(),
                "showLocked", state.filters().showLocked()));
        search.put("collapsedModSections", List.copyOf(state.collapsedModSections()));
        search.put("searchRevision", state.revision());
        search.put("favoriteRevision", IndexFavoriteStore.revision());
        search.put("bookmarkSearchReady", true);
        return Map.copyOf(search);
    }

    private static Map<String, Object> moduleData(IndexRecipeCache.CacheSnapshot cache) {
        List<Map<String, Object>> mods = cache.mods().stream()
                .map(IndexRecipeCache.IndexModData::toMap)
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("moduleCount", mods.size());
        data.put("modules", sample(mods));
        data.put("realModuleDataBacked", !mods.isEmpty());
        data.put("itemBackedModuleRows", cache.items().stream().map(IndexRecipeCache.IndexItemData::modId).distinct().count());
        data.put("recipeBackedModuleRows", cache.recipes().stream().map(IndexRecipeCache.IndexRecipeData::modId).distinct().count());
        return Map.copyOf(data);
    }

    private static Map<String, Object> sourceFacts() {
        IndexRecipeQueryClientState.Health health = IndexRecipeQueryClientState.health();
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("available", health.available());
        facts.put("generation", health.generation());
        facts.put("reason", health.reason());
        facts.put("rawRecipeCount", health.rawRecipeCount());
        facts.put("adaptedRecipeCount", health.adaptedRecipeCount());
        facts.put("sourceCardCount", health.sourceCardCount());
        facts.put("sourceFactCount", health.sourceFactCount());
        facts.put("usageItemCount", health.usageItemCount());
        facts.put("providerCount", health.providerCount());
        facts.put("warningCount", health.warningCount());
        facts.put("lastProviderError", health.lastProviderError());
        Object lastQueried = IndexRecipeQueryClientState.lastQueriedItem();
        facts.put("lastQueriedItem", lastQueried == null ? "" : String.valueOf(lastQueried));
        facts.put("lastQueryWarning", IndexRecipeQueryClientState.lastQueryWarning());
        facts.put("revision", IndexRecipeQueryClientState.revision());
        return Map.copyOf(facts);
    }

    private static Map<String, Object> sourceFactOverlay(IndexRecipeCache.CacheSnapshot cache) {
        Map<String, Object> overlay = new LinkedHashMap<>();
        Map<String, Object> facts = sourceFacts();
        overlay.put("available", facts.get("available"));
        overlay.put("sourceFactCount", facts.get("sourceFactCount"));
        overlay.put("sourceCardCount", facts.get("sourceCardCount"));
        overlay.put("usageItemCount", facts.get("usageItemCount"));
        overlay.put("providerCount", facts.get("providerCount"));
        overlay.put("cacheRecipeCount", cache.recipes().size());
        overlay.put("cacheItemCount", cache.items().size());
        overlay.put("liveSourceFactsBacked", Boolean.TRUE.equals(facts.get("available"))
                || ((Number) facts.getOrDefault("sourceFactCount", 0)).intValue() > 0
                || !cache.recipes().isEmpty());
        return Map.copyOf(overlay);
    }

    private static Map<String, Object> playerContext() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("playerPresent", minecraft.player != null);
        context.put("levelPresent", minecraft.level != null);
        context.put("screen", screen == null ? "none" : screen.getClass().getName());
        if (minecraft.player != null) {
            context.put("playerName", minecraft.player.getName().getString());
            context.put("dimension", minecraft.player.level().dimension().identifier().toString());
            context.put("blockPosition", minecraft.player.blockPosition().toShortString());
        }
        return Map.copyOf(context);
    }

    private static List<String> sampleStrings(Iterable<String> values) {
        List<String> result = new ArrayList<>();
        if (values == null) {
            return List.of();
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(value);
            }
            if (result.size() >= SAMPLE_LIMIT) {
                break;
            }
        }
        return List.copyOf(result);
    }

    private static List<Map<String, Object>> sample(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> sample = new ArrayList<>();
        for (int index = 0; index < Math.min(SAMPLE_LIMIT, rows.size()); index++) {
            sample.add(Map.copyOf(rows.get(index)));
        }
        return List.copyOf(sample);
    }

    private static void push(Map<String, Object> actionEntry) {
        ACTION_HISTORY.addFirst(Map.copyOf(actionEntry));
        while (ACTION_HISTORY.size() > HISTORY_LIMIT) {
            ACTION_HISTORY.removeLast();
        }
    }

    private static Object value(Map<String, Object> map, String key) {
        return map == null ? "" : map.get(key);
    }

    private static void putIfPresent(Map<String, Object> state, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            state.put(key, value);
        }
    }

    private static String selectedId(Object id) {
        return id == null ? "" : String.valueOf(id);
    }

    private static String clean(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }
}
