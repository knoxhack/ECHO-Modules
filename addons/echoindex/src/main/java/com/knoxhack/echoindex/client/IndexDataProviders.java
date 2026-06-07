package com.knoxhack.echoindex.client;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoDataProvider;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoindex.Config;
import com.knoxhack.echoindex.EchoIndex;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class IndexDataProviders {
    private static final EchoDataProvider PROVIDER = IndexDataProviders::resolve;

    private IndexDataProviders() {
    }

    public static void register() {
        EchoScreenRegistry.registerDataProvider("index", PROVIDER);
        EchoScreenRegistry.registerDataProvider(EchoIndex.id("index"), PROVIDER);
    }

    public static EchoDataContext context() {
        return EchoDataContext.empty()
                .missingPlaceholder("")
                .provider("index", PROVIDER)
                .put("selectedItem", selectedItem())
                .put("selectedRecipe", selectedRecipe())
                .put("selectedMachine", selectedMachine());
    }

    private static Object resolve(EchoDataContext context, List<String> path) {
        if (path == null || path.isEmpty()) {
            return dashboard();
        }
        String key = String.join(".", path);
        return switch (key) {
            case "dashboard" -> dashboard();
            case "nav.sections" -> navSections();
            case "items.all" -> snapshot().items().stream().map(IndexRecipeCache.IndexItemData::toMap).toList();
            case "items.visible" -> visibleItems();
            case "items.groupedByMod", "items.grouped_by_mod" -> groupedItemsByMod();
            case "items.selected", "item.selected" -> selectedItem();
            case "mods.all", "mods.visible" -> snapshot().mods().stream().map(IndexRecipeCache.IndexModData::toMap).toList();
            case "mod.sections" -> modSections();
            case "recipes.all" -> snapshot().recipes().stream().map(IndexRecipeCache.IndexRecipeData::toMap).toList();
            case "recipes.visible" -> visibleRecipes();
            case "recipe.selected" -> selectedRecipe();
            case "recipe.inputs" -> selectedRecipeList("inputs");
            case "recipe.outputs" -> selectedRecipeList("outputs");
            case "recipe.machine" -> selectedRecipeMachine();
            case "recipe.variants" -> selectedRecipeVariants();
            case "usages.visible" -> visibleUsages();
            case "usage.selected" -> selectedItem();
            case "usage.categories" -> usageCategories();
            case "machines.all" -> snapshot().machines().stream().map(IndexRecipeCache.IndexMachineData::toMap).toList();
            case "machines.visible" -> visibleMachines();
            case "machine.selected" -> selectedMachine();
            case "machine.recipeTypes" -> selectedMachineRecipeTypes();
            case "machine.recipes" -> selectedMachineRecipes();
            case "favorites.items" -> favoriteItems();
            case "favorites.recipes" -> favoriteRecipes();
            case "favorites.machines" -> favoriteMachines();
            case "history.recent" -> IndexHistoryStore.rows();
            case "history.summary" -> historySummary();
            case "filters.mods" -> modOptions();
            case "filters.categories" -> categoryOptions();
            case "filters.recipeTypes" -> recipeTypeOptions();
            case "filters.machines" -> machineOptions();
            case "filters.sortOptions" -> sortOptions();
            case "filters.active" -> activeFilterChips();
            case "filters.summary" -> filterSummary();
            case "filters.mod" -> state().filters().mod();
            case "filters.category" -> state().filters().category();
            case "filters.recipeType" -> state().filters().recipeType();
            case "filters.machine" -> state().filters().machine();
            case "filters.sort" -> state().filters().sort();
            case "filters.grouping" -> state().filters().grouping();
            case "search.query" -> state().searchQuery();
            case "search.results" -> searchResults();
            case "search.suggestions" -> searchSuggestions();
            case "settings" -> settings();
            case "debug.stats" -> snapshot().stats(state());
            case "debug.warnings" -> snapshot().warnings();
            default -> resolveNested(key);
        };
    }

    private static Object resolveNested(String key) {
        if (key.startsWith("dashboard.")) {
            return dashboard().get(key.substring("dashboard.".length()));
        }
        if (key.startsWith("settings.")) {
            return settings().get(key.substring("settings.".length()));
        }
        if (key.startsWith("history.summary.")) {
            return historySummary().get(key.substring("history.summary.".length()));
        }
        if (key.startsWith("filters.summary.")) {
            return filterSummary().get(key.substring("filters.summary.".length()));
        }
        return "";
    }

    private static IndexRecipeCache.CacheSnapshot snapshot() {
        return IndexRecipeCache.snapshot();
    }

    private static IndexUiState state() {
        return IndexUiState.INSTANCE;
    }

    private static Map<String, Object> dashboard() {
        IndexRecipeCache.CacheSnapshot snapshot = snapshot();
        Map<String, Object> stats = snapshot.stats(state());
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("title", "INDEX DATABASE");
        map.put("subtitle", "Items, recipes, usages, machines, and discoveries.");
        map.put("mode", IndexScreenCorePages.modeFor(state().currentPage()));
        map.put("query", state().searchQuery());
        map.put("selectedMod", state().filters().mod().isBlank() ? "All Mods" : state().filters().mod());
        map.put("itemCount", stats.get("itemCount"));
        map.put("recipeCount", stats.get("recipeCount"));
        map.put("machineCount", stats.get("machineCount"));
        map.put("modCount", stats.get("modCount"));
        map.put("visibleItemCount", stats.get("visibleItemCount"));
        map.put("visibleRecipeCount", stats.get("visibleRecipeCount"));
        map.put("visibleMachineCount", stats.get("visibleMachineCount"));
        map.put("providerStatus", snapshot.healthLine());
        map.put("providerHealthHint", providerHealthHint(snapshot));
        map.put("browseHint", "Search, filter, or open a section to inspect items, recipes, usages, and machines.");
        map.put("favoritesHint", "Saved records stay local to this player profile.");
        map.put("historyHint", "Recently opened Index records appear here for quick return.");
        map.put("cacheBuildTimeMs", snapshot.buildTimeMs());
        map.put("providerRefreshCount", snapshot.providerRefreshCount());
        map.put("debugEnabled", Config.DEBUG_SCREENCORE.get());
        return map;
    }

    private static String providerHealthHint(IndexRecipeCache.CacheSnapshot snapshot) {
        if (!snapshot.warnings().isEmpty()) {
            return snapshot.warnings().size() + " provider warning(s). Open Settings for details.";
        }
        return "Provider cache is ready. Optional integrations stay quiet when absent.";
    }

    private static List<Map<String, Object>> navSections() {
        return List.of(
                nav("Overview", "Dashboard", "index_dashboard", "INDEX DATABASE"),
                nav("Items", "Item Browser", "index_items", "Mod-separated grid"),
                nav("Recipes", "Recipe Browser", "index_recipes", "Crafting and machines"),
                nav("Usages", "Usage Browser", "index_usages", "Where selected items go"),
                nav("Machines", "Machine Browser", "index_machines", "Process cards"),
                nav("Mods", "Mod Browser", "index_mods", "Content by addon"),
                nav("Favorites", "Favorites", "index_favorites", "Pins and bookmarks"),
                nav("History", "History", "index_history", "Recently viewed"),
                nav("Settings", "Settings", "index_settings", "Filters and debug"));
    }

    private static Map<String, Object> nav(String id, String label, String page, String subtitle) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", id.toLowerCase(Locale.ROOT));
        map.put("label", label);
        map.put("title", label);
        map.put("page", "echoindex:" + page);
        map.put("subtitle", subtitle);
        map.put("selected", state().currentPage().equals(EchoIndex.id(page)));
        return map;
    }

    private static List<Map<String, Object>> visibleItems() {
        return IndexSearchService.filterItems(snapshot().items(), state());
    }

    private static List<Map<String, Object>> visibleRecipes() {
        return IndexSearchService.filterRecipes(snapshot().recipes(), state());
    }

    private static List<Map<String, Object>> visibleMachines() {
        return IndexSearchService.filterMachines(snapshot().machines(), state());
    }

    private static List<Map<String, Object>> groupedItemsByMod() {
        LinkedHashMap<String, List<Map<String, Object>>> byMod = new LinkedHashMap<>();
        for (Map<String, Object> item : visibleItems()) {
            String modId = String.valueOf(item.getOrDefault("modId", "minecraft"));
            byMod.computeIfAbsent(modId, ignored -> new ArrayList<>()).add(item);
        }
        ArrayList<Map<String, Object>> sections = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : byMod.entrySet()) {
            String modId = entry.getKey();
            IndexRecipeCache.IndexModData mod = snapshot().modById().get(modId);
            boolean collapsed = state().isCollapsed(modId);
            LinkedHashMap<String, Object> section = new LinkedHashMap<>();
            section.put("id", modId);
            section.put("modId", modId);
            section.put("name", mod == null ? IndexAddonPresentation.displayName(modId) : mod.name());
            section.put("icon", mod == null ? "minecraft:book" : mod.icon());
            section.put("itemCount", entry.getValue().size());
            section.put("recipeCount", mod == null ? 0 : mod.recipeCount());
            section.put("machineCount", mod == null ? 0 : mod.machineCount());
            section.put("summary", entry.getValue().size() + " visible item(s), "
                    + (mod == null ? 0 : mod.recipeCount()) + " recipe(s), "
                    + (mod == null ? 0 : mod.machineCount()) + " machine(s)");
            section.put("collapsed", collapsed);
            section.put("collapseLabel", collapsed ? "Expand" : "Collapse");
            section.put("collapseHint", collapsed
                    ? "Expand this mod section to show its indexed items."
                    : "Collapse this mod section to make the browser easier to scan.");
            section.put("items", collapsed ? List.of() : entry.getValue());
            sections.add(section);
        }
        return sections;
    }

    private static List<Map<String, Object>> modSections() {
        return groupedItemsByMod();
    }

    private static Map<String, Object> selectedItem() {
        Identifier selected = state().selection().itemId();
        IndexRecipeCache.IndexItemData data = selected == null ? null : snapshot().itemById().get(selected.toString());
        if (data == null && !visibleItems().isEmpty()) {
            return visibleItems().getFirst();
        }
        return data == null ? Map.of() : data.toMap();
    }

    private static Map<String, Object> selectedRecipe() {
        Identifier selected = state().selection().recipeId();
        IndexRecipeCache.IndexRecipeData data = selected == null ? null : snapshot().recipeById().get(selected.toString());
        if (data == null && !visibleRecipes().isEmpty()) {
            return visibleRecipes().getFirst();
        }
        return data == null ? Map.of() : data.toMap();
    }

    private static Map<String, Object> selectedMachine() {
        String selected = state().selection().machineId();
        IndexRecipeCache.IndexMachineData data = selected.isBlank() ? null : snapshot().machineById().get(selected);
        if (data == null && !visibleMachines().isEmpty()) {
            return visibleMachines().getFirst();
        }
        return data == null ? Map.of() : data.toMap();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> selectedRecipeList(String key) {
        Object value = selectedRecipe().get(key);
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private static Map<String, Object> selectedRecipeMachine() {
        Object machineId = selectedRecipe().get("machineId");
        if (machineId == null) {
            return Map.of();
        }
        IndexRecipeCache.IndexMachineData machine = snapshot().machineById().get(String.valueOf(machineId));
        return machine == null ? Map.of() : machine.toMap();
    }

    private static List<Map<String, Object>> selectedRecipeVariants() {
        Object type = selectedRecipe().get("type");
        Object output = selectedRecipe().get("outputSummary");
        if (type == null || output == null) {
            return List.of();
        }
        String typeString = String.valueOf(type);
        String outputString = String.valueOf(output);
        return snapshot().recipes().stream()
                .filter(recipe -> recipe.type().equals(typeString) && recipe.outputSummary().equals(outputString))
                .map(IndexRecipeCache.IndexRecipeData::toMap)
                .limit(12)
                .toList();
    }

    private static List<Map<String, Object>> visibleUsages() {
        String selectedItemId = String.valueOf(selectedItem().getOrDefault("id", ""));
        if (selectedItemId.isBlank()) {
            return List.of();
        }
        return snapshot().recipes().stream()
                .filter(recipe -> slotListContains(recipe.inputs(), selectedItemId))
                .filter(recipe -> IndexSearchService.normalize(state().searchQuery()).isBlank()
                        || IndexSearchService.filterRecipes(List.of(recipe), state()).size() == 1)
                .map(IndexRecipeCache.IndexRecipeData::toMap)
                .limit(Math.max(36, Config.UI_MAX_RENDERED_ITEMS.get()))
                .toList();
    }

    private static List<Map<String, Object>> usageCategories() {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        for (Map<String, Object> usage : visibleUsages()) {
            String type = String.valueOf(usage.getOrDefault("typeName", "Recipe"));
            counts.merge(type, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .map(entry -> option(entry.getKey(), entry.getKey(), entry.getValue() + " usage(s)"))
                .toList();
    }

    private static boolean slotListContains(List<Map<String, Object>> slots, String id) {
        for (Map<String, Object> slot : slots) {
            if (id.equals(String.valueOf(slot.getOrDefault("id", "")))) {
                return true;
            }
            Object alternatives = slot.get("alternatives");
            if (alternatives instanceof List<?> list) {
                for (Object alternative : list) {
                    if (alternative instanceof Map<?, ?> map && id.equals(String.valueOf(map.get("id")))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static List<Map<String, Object>> selectedMachineRecipeTypes() {
        Object types = selectedMachine().get("recipeTypes");
        if (types instanceof List<?> list) {
            return list.stream()
                    .map(type -> option(String.valueOf(type), String.valueOf(type), "Supported recipe type"))
                    .toList();
        }
        return List.of();
    }

    private static List<Map<String, Object>> selectedMachineRecipes() {
        String machineId = String.valueOf(selectedMachine().getOrDefault("id", ""));
        return snapshot().recipes().stream()
                .filter(recipe -> recipe.machineId().equals(machineId))
                .map(IndexRecipeCache.IndexRecipeData::toMap)
                .limit(Math.max(36, Config.UI_MAX_RENDERED_ITEMS.get()))
                .toList();
    }

    private static List<Map<String, Object>> favoriteItems() {
        return snapshot().items().stream()
                .filter(IndexRecipeCache.IndexItemData::favorite)
                .map(IndexRecipeCache.IndexItemData::toMap)
                .toList();
    }

    private static List<Map<String, Object>> favoriteRecipes() {
        return snapshot().recipes().stream()
                .filter(IndexRecipeCache.IndexRecipeData::bookmarked)
                .map(IndexRecipeCache.IndexRecipeData::toMap)
                .toList();
    }

    private static List<Map<String, Object>> favoriteMachines() {
        return snapshot().machines().stream()
                .filter(IndexRecipeCache.IndexMachineData::bookmarked)
                .map(IndexRecipeCache.IndexMachineData::toMap)
                .toList();
    }

    private static Map<String, Object> historySummary() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        int count = IndexHistoryStore.rows().size();
        map.put("count", count);
        map.put("title", count == 0 ? "No recent records" : count + " recent record(s)");
        map.put("hint", count == 0
                ? "Open items, recipes, and machines to build your local Index history."
                : "History is local and can be cleared without changing provider data.");
        return map;
    }

    private static List<Map<String, Object>> modOptions() {
        ArrayList<Map<String, Object>> options = new ArrayList<>();
        options.add(option("all", "All Mods", "No mod filter"));
        snapshot().mods().stream()
                .map(mod -> option(mod.id(), mod.name(), mod.itemCount() + " item(s), " + mod.recipeCount() + " recipe(s)"))
                .forEach(options::add);
        return options;
    }

    private static List<Map<String, Object>> categoryOptions() {
        return List.of(
                option("all", "All", "Every item category"),
                option("blocks", "Blocks", "Placeable blocks"),
                option("items", "Items", "General items"),
                option("tools", "Tools", "Durable tools"),
                option("weapons", "Weapons", "Combat items"),
                option("armor", "Armor", "Wearable gear"),
                option("machines", "Machines", "Stations and process blocks"),
                option("components", "Components", "Crafting parts"));
    }

    private static List<Map<String, Object>> recipeTypeOptions() {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        ArrayList<Map<String, Object>> options = new ArrayList<>();
        options.add(option("all", "All Recipe Types", "No recipe type filter"));
        for (IndexRecipeCache.IndexRecipeData recipe : snapshot().recipes()) {
            if (seen.add(recipe.type())) {
                options.add(option(recipe.type(), recipe.typeName(), recipe.modName()));
            }
        }
        return options;
    }

    private static List<Map<String, Object>> machineOptions() {
        ArrayList<Map<String, Object>> options = new ArrayList<>();
        options.add(option("", "All Machines", "No machine filter"));
        snapshot().machines().stream()
                .map(machine -> option(machine.id(), machine.name(), machine.recipeCount() + " recipe(s)"))
                .forEach(options::add);
        return options;
    }

    private static List<Map<String, Object>> sortOptions() {
        return List.of(
                option("name", "Name", "Alphabetical"),
                option("mod", "Mod", "Group by addon"),
                option("category", "Category", "Item category or recipe type"),
                option("favorites_first", "Favorites First", "Pinned rows first"),
                option("recipe_count", "Recipe Count", "Most recipe outputs first"),
                option("usage_count", "Usage Count", "Most usages first"));
    }

    private static List<Map<String, Object>> activeFilterChips() {
        ArrayList<Map<String, Object>> chips = new ArrayList<>();
        if (!state().searchQuery().isBlank()) {
            chips.add(option("search", "Search: " + state().searchQuery(), "Clear search to remove"));
        }
        if (!state().filters().mod().isBlank()) {
            chips.add(option("mod", "Mod: " + state().filters().mod(), "Active mod filter"));
        }
        if (!"all".equals(state().filters().category())) {
            chips.add(option("category", "Category: " + state().filters().category(), "Active category filter"));
        }
        if (!"all".equals(state().filters().recipeType())) {
            chips.add(option("recipeType", "Recipe: " + state().filters().recipeType(), "Active recipe type filter"));
        }
        if (!state().filters().machine().isBlank()) {
            chips.add(option("machine", "Machine: " + state().filters().machine(), "Active machine filter"));
        }
        return chips;
    }

    private static Map<String, Object> filterSummary() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        int active = activeFilterChips().size();
        map.put("activeCount", active);
        map.put("label", active == 0 ? "No active filters" : active + " active filter(s)");
        map.put("hint", active == 0
                ? "Use search, mod, category, machine, or sort controls to narrow Index results."
                : "Clear search or reset filters to return to the full Index.");
        return map;
    }

    private static List<Map<String, Object>> searchResults() {
        ArrayList<Map<String, Object>> results = new ArrayList<>();
        results.addAll(visibleItems().stream().limit(16).toList());
        results.addAll(visibleRecipes().stream().limit(16).toList());
        results.addAll(visibleMachines().stream().limit(16).toList());
        return results;
    }

    private static List<Map<String, Object>> searchSuggestions() {
        ArrayList<Map<String, Object>> suggestions = new ArrayList<>();
        suggestions.add(option("@minecraft", "@minecraft", "Filter by Minecraft"));
        snapshot().mods().stream().limit(8)
                .map(mod -> option("@" + mod.id(), "@" + mod.id(), "Filter " + mod.name()))
                .forEach(suggestions::add);
        suggestions.add(option("!favorites", "!favorites", "Show favorites only"));
        suggestions.add(option("type:recipe", "type:recipe", "Recipe results"));
        suggestions.add(option("type:machine", "type:machine", "Machine results"));
        return suggestions;
    }

    private static Map<String, Object> settings() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("useScreenCore", Config.UI_USE_SCREENCORE.get());
        map.put("debug", Config.DEBUG_SCREENCORE.get());
        map.put("debugProviders", Config.DEBUG_PROVIDERS.get());
        map.put("debugActions", Config.DEBUG_ACTIONS.get());
        map.put("groupByMod", Config.UI_GROUP_BY_MOD.get());
        map.put("compactGrid", Config.UI_COMPACT_GRID.get());
        map.put("rememberLastPage", Config.UI_REMEMBER_LAST_PAGE.get());
        map.put("showLockedItems", Config.UI_SHOW_LOCKED_ITEMS.get());
        map.put("showRecipeIds", Config.DEBUG_SHOW_RECIPE_IDS.get());
        map.put("uiHint", "These values reflect current client config. Some changes require reopening the screen.");
        map.put("providerHint", snapshot().warnings().isEmpty()
                ? "No provider warnings. Missing optional addons are ignored quietly."
                : snapshot().warnings().size() + " provider warning(s) are listed below.");
        return map;
    }

    private static Map<String, Object> option(String id, String label, String subtitle) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("value", id);
        map.put("label", label);
        map.put("title", label);
        map.put("subtitle", subtitle);
        return map;
    }
}
