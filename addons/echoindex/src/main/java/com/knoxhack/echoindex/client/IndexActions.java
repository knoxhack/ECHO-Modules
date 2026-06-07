package com.knoxhack.echoindex.client;

import com.knoxhack.echoscreencore.api.EchoScreens;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.action.EchoActionContext;
import com.knoxhack.echoindex.Config;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.EchoIndexClient;
import com.knoxhack.echoindex.network.IndexActionPacket;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class IndexActions {
    private IndexActions() {
    }

    public static void register() {
        register("index.open_page", IndexActions::openPage);
        register("index.back", EchoActionContext::back);
        register("index.close", EchoActionContext::close);
        register("index.select_item", IndexActions::selectItem);
        register("index.select_recipe", IndexActions::selectRecipe);
        register("index.select_usage", IndexActions::selectUsage);
        register("index.select_machine", IndexActions::selectMachine);
        register("index.select_mod", IndexActions::selectMod);
        register("index.open_item_detail", context -> openItemPage(context, IndexScreenCorePages.ITEM_DETAIL));
        register("index.open_recipe_detail", context -> openRecipePage(context, IndexScreenCorePages.RECIPE_DETAIL));
        register("index.open_usage_detail", context -> openItemPage(context, IndexScreenCorePages.USAGES));
        register("index.open_machine_detail", context -> openMachinePage(context, IndexScreenCorePages.MACHINE_DETAIL));
        register("index.open_recipes_for_item", context -> openItemPage(context, IndexScreenCorePages.RECIPES));
        register("index.open_usages_for_item", context -> openItemPage(context, IndexScreenCorePages.USAGES));
        register("index.open_machine_recipes", context -> openMachinePage(context, IndexScreenCorePages.RECIPES));
        register("index.open_machine_uses", context -> openMachinePage(context, IndexScreenCorePages.USAGES));
        register("index.toggle_favorite", IndexActions::toggleFavorite);
        register("index.add_bookmark", IndexActions::addBookmark);
        register("index.remove_bookmark", IndexActions::removeBookmark);
        register("index.clear_history", IndexActions::clearHistory);
        register("index.search_changed", IndexActions::searchChanged);
        register("index.search_submit", IndexActions::searchChanged);
        register("index.clear_search", IndexActions::clearSearch);
        register("index.filter_changed", IndexActions::filterChanged);
        register("index.sort_changed", IndexActions::sortChanged);
        register("index.reset_filters", IndexActions::resetFilters);
        register("index.toggle_mod_section", IndexActions::toggleModSection);
        register("index.copy_item_id", IndexActions::copyItemId);
        register("index.copy_recipe_id", IndexActions::copyRecipeId);
        register("index.open_dialog", context -> true);
        register("index.close_dialog", context -> true);
        register("index.cycle_recipe_variant", IndexActions::selectRecipe);
        register("index.open_input_usage", context -> openItemPage(context, IndexScreenCorePages.USAGES));
        register("index.open_output_recipe", context -> openItemPage(context, IndexScreenCorePages.RECIPES));
        register("index.open_usage_recipe", context -> openRecipePage(context, IndexScreenCorePages.RECIPE_DETAIL));
        register("index.open_usage_machine", context -> openMachinePage(context, IndexScreenCorePages.MACHINE_DETAIL));
        register("index.open_related_item", context -> openItemPage(context, IndexScreenCorePages.ITEM_DETAIL));
        register("index.filter_machine_type", IndexActions::filterChanged);
    }

    private static void register(String id, com.knoxhack.echoscreencore.api.action.EchoAction action) {
        EchoScreenRegistry.registerAction(id, context -> {
            if (EchoIndexClient.nativeLoaderClientActiveForScreens()) {
                return EchoIndexClient.dispatchNativeScreenCoreAction(id, context, action);
            }
            return action.run(context);
        });
    }

    private static boolean openPage(EchoActionContext context) {
        Identifier page = pageFrom(context.param("page").isBlank() ? context.actionValue() : context.param("page"));
        IndexUiState.INSTANCE.setCurrentPage(page);
        invalidate();
        return context.open(page);
    }

    private static boolean selectItem(EchoActionContext context) {
        Identifier id = itemId(context.actionValue());
        if (id == null) {
            return invalid("item", context.actionValue());
        }
        IndexRecipeCache.IndexItemData item = IndexRecipeCache.snapshot().itemById().get(id.toString());
        if (item == null) {
            return invalid("item", id.toString());
        }
        IndexUiState.INSTANCE.selection().selectItem(id);
        IndexUiState.INSTANCE.history().add("item", item.id(), item.displayName(), item.icon());
        invalidate();
        return true;
    }

    private static boolean selectUsage(EchoActionContext context) {
        return selectRecipe(context);
    }

    private static boolean selectRecipe(EchoActionContext context) {
        Identifier id = Identifier.tryParse(context.actionValue());
        if (id == null) {
            return invalid("recipe", context.actionValue());
        }
        IndexRecipeCache.IndexRecipeData recipe = IndexRecipeCache.snapshot().recipeById().get(id.toString());
        if (recipe == null) {
            return invalid("recipe", id.toString());
        }
        IndexUiState.INSTANCE.selection().selectRecipe(id);
        IndexUiState.INSTANCE.history().add("recipe", recipe.id(), recipe.title(), recipe.icon());
        invalidate();
        return true;
    }

    private static boolean selectMachine(EchoActionContext context) {
        String id = value(context);
        if (id.isBlank()) {
            return invalid("machine", id);
        }
        IndexRecipeCache.IndexMachineData machine = IndexRecipeCache.snapshot().machineById().get(id);
        if (machine == null) {
            return invalid("machine", id);
        }
        IndexUiState.INSTANCE.selection().selectMachine(id);
        IndexUiState.INSTANCE.history().add("machine", machine.id(), machine.name(), machine.icon());
        invalidate();
        return true;
    }

    private static boolean selectMod(EchoActionContext context) {
        String modId = value(context);
        IndexUiState.INSTANCE.selection().selectMod(modId);
        IndexUiState.INSTANCE.filters().setMod(modId);
        invalidate();
        return true;
    }

    private static boolean openItemPage(EchoActionContext context, Identifier page) {
        selectItem(context);
        IndexUiState.INSTANCE.setCurrentPage(page);
        invalidate();
        return context.open(page);
    }

    private static boolean openRecipePage(EchoActionContext context, Identifier page) {
        selectRecipe(context);
        IndexUiState.INSTANCE.setCurrentPage(page);
        invalidate();
        return context.open(page);
    }

    private static boolean openMachinePage(EchoActionContext context, Identifier page) {
        selectMachine(context);
        IndexUiState.INSTANCE.setCurrentPage(page);
        invalidate();
        return context.open(page);
    }

    private static boolean toggleFavorite(EchoActionContext context) {
        String kind = context.param("kind").isBlank() ? "item" : context.param("kind");
        String id = valueOrSelected(context, kind);
        if (id.isBlank()) {
            return invalid(kind, id);
        }
        boolean enabled = IndexFavoriteStore.toggle(kind, id);
        sendBookmark(kind, id, enabled);
        IndexRecipeCache.invalidate();
        invalidate();
        return true;
    }

    private static boolean addBookmark(EchoActionContext context) {
        String kind = context.param("kind").isBlank() ? inferKind(context.actionValue()) : context.param("kind");
        String id = valueOrSelected(context, kind);
        if (id.isBlank()) {
            return invalid(kind, id);
        }
        IndexFavoriteStore.add(kind, id);
        sendBookmark(kind, id, true);
        IndexRecipeCache.invalidate();
        invalidate();
        return true;
    }

    private static boolean removeBookmark(EchoActionContext context) {
        String kind = context.param("kind").isBlank() ? inferKind(context.actionValue()) : context.param("kind");
        String id = valueOrSelected(context, kind);
        if (id.isBlank()) {
            return invalid(kind, id);
        }
        IndexFavoriteStore.remove(kind, id);
        sendBookmark(kind, id, false);
        IndexRecipeCache.invalidate();
        invalidate();
        return true;
    }

    private static boolean clearHistory(EchoActionContext context) {
        IndexHistoryStore.clear();
        invalidate();
        return true;
    }

    private static boolean searchChanged(EchoActionContext context) {
        IndexUiState.INSTANCE.setSearchQuery(context.actionValue());
        invalidate();
        return true;
    }

    private static boolean clearSearch(EchoActionContext context) {
        IndexUiState.INSTANCE.setSearchQuery("");
        invalidate();
        return true;
    }

    private static boolean filterChanged(EchoActionContext context) {
        String filter = context.param("filter");
        if (filter.isBlank()) {
            filter = context.componentId();
        }
        IndexUiState.INSTANCE.filters().set(filter, context.actionValue());
        invalidate();
        return true;
    }

    private static boolean sortChanged(EchoActionContext context) {
        IndexUiState.INSTANCE.filters().setSort(context.actionValue());
        invalidate();
        return true;
    }

    private static boolean resetFilters(EchoActionContext context) {
        IndexUiState.INSTANCE.resetFilters();
        invalidate();
        return true;
    }

    private static boolean toggleModSection(EchoActionContext context) {
        IndexUiState.INSTANCE.toggleModSection(value(context));
        invalidate();
        return true;
    }

    private static boolean copyItemId(EchoActionContext context) {
        Identifier id = itemId(valueOrSelected(context, "item"));
        if (id == null) {
            return false;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(id.toString());
        return true;
    }

    private static boolean copyRecipeId(EchoActionContext context) {
        String id = valueOrSelected(context, "recipe");
        if (id.isBlank()) {
            return false;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(id);
        return true;
    }

    private static void sendBookmark(String kind, String rawId, boolean enabled) {
        Minecraft minecraft = minecraft();
        if ((minecraft == null || minecraft.player == null) && !EchoNetClientActions.hasActionOverrideForTests()) {
            return;
        }
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            return;
        }
        String clean = kind == null ? "" : kind.toLowerCase(java.util.Locale.ROOT);
        if (clean.startsWith("recipe")) {
            EchoNetClientActions.sendServerboundAction(new IndexActionPacket(
                    enabled ? IndexActionPacket.Action.PIN_RECIPE : IndexActionPacket.Action.UNPIN_RECIPE, id));
        } else if (clean.startsWith("item") || clean.startsWith("bookmark")) {
            EchoNetClientActions.sendServerboundAction(new IndexActionPacket(
                    enabled ? IndexActionPacket.Action.BOOKMARK : IndexActionPacket.Action.UNBOOKMARK, id));
        }
    }

    private static String value(EchoActionContext context) {
        return context.actionValue() == null ? "" : context.actionValue().strip();
    }

    private static String valueOrSelected(EchoActionContext context, String kind) {
        String value = value(context);
        if (!value.isBlank()) {
            return value;
        }
        String clean = kind == null ? "" : kind.toLowerCase(java.util.Locale.ROOT);
        if (clean.startsWith("recipe") && IndexUiState.INSTANCE.selection().recipeId() != null) {
            return IndexUiState.INSTANCE.selection().recipeId().toString();
        }
        if (clean.startsWith("machine")) {
            return IndexUiState.INSTANCE.selection().machineId();
        }
        return IndexUiState.INSTANCE.selection().itemId() == null ? "" : IndexUiState.INSTANCE.selection().itemId().toString();
    }

    private static String inferKind(String value) {
        String id = value == null ? "" : value;
        if (IndexRecipeCache.snapshot().recipeById().containsKey(id)) {
            return "recipe";
        }
        if (IndexRecipeCache.snapshot().machineById().containsKey(id)) {
            return "machine";
        }
        return "item";
    }

    private static Identifier itemId(String raw) {
        Identifier id = Identifier.tryParse(raw == null ? "" : raw);
        return id == null || !IndexRecipeCache.snapshot().itemById().containsKey(id.toString()) ? null : id;
    }

    private static Identifier pageFrom(String raw) {
        Identifier parsed = Identifier.tryParse(raw == null ? "" : raw);
        if (parsed != null) {
            return parsed;
        }
        return IndexScreenCorePages.fromMode(raw);
    }

    private static boolean invalid(String kind, String value) {
        if (Config.DEBUG_ACTIONS.get()) {
            EchoIndex.LOGGER.debug("ECHO: ScreenCore Index ignored invalid {} action value '{}'.", kind, value);
        }
        return false;
    }

    private static void invalidate() {
        EchoScreens.invalidateData();
    }

    private static Minecraft minecraft() {
        try {
            return Minecraft.getInstance();
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
    }
}
